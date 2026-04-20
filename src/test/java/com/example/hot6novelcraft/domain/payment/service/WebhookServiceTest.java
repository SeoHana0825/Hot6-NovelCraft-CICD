package com.example.hot6novelcraft.domain.payment.service;

import com.example.hot6novelcraft.common.security.RedisUtil;
import com.example.hot6novelcraft.domain.payment.dto.request.WebhookRequest;
import com.example.hot6novelcraft.domain.payment.entity.Payment;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentMethod;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentStatus;
import com.example.hot6novelcraft.domain.webhookevent.entity.WebhookEvent;
import com.example.hot6novelcraft.domain.webhookevent.entity.WebhookEventStatus;
import com.example.hot6novelcraft.domain.webhookevent.entity.WebhookEventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.portone.sdk.server.payment.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("WebhookService 테스트")
class WebhookServiceTest {

    @InjectMocks
    private WebhookService webhookService;

    @Mock
    private WebhookTransactionService webhookTransactionService;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RedisUtil redisUtil;

    private static final String PAYMENT_KEY = "payment-test-key-12345";
    private static final String TRANSACTION_ID = "transaction-12345";
    private static final String CANCELLATION_ID = "cancellation-12345";
    private static final Long PAYMENT_ID = 100L;
    private static final Long USER_ID = 1L;
    private static final Long AMOUNT = 10000L;
    private static final Long WEBHOOK_EVENT_ID = 1000L;

    private WebhookRequest createWebhookRequest(String type, String paymentId, String transactionId, String cancellationId) {
        WebhookRequest.WebhookData data = new WebhookRequest.WebhookData(paymentId, transactionId, cancellationId);
        return new WebhookRequest(type, data);
    }

    private Payment createMockPayment(Long id, PaymentStatus status) {
        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(id);
        given(payment.getUserId()).willReturn(USER_ID);
        given(payment.getAmount()).willReturn(AMOUNT);
        given(payment.getStatus()).willReturn(status);
        given(payment.getPaymentKey()).willReturn(PAYMENT_KEY);
        return payment;
    }

    private WebhookEvent createMockWebhookEvent(Long id) {
        WebhookEvent event = mock(WebhookEvent.class);
        given(event.getId()).willReturn(id);
        given(event.getStatus()).willReturn(WebhookEventStatus.PENDING);
        return event;
    }

    // =========================================================
    // handleWebhook() - 웹훅 처리 메인 로직
    // =========================================================
    @Nested
    @DisplayName("handleWebhook() - 웹훅 기본 검증")
    class HandleWebhookValidationTest {

        @Test
        @DisplayName("실패 - 처리 불가능한 타입 무시")
        void handleWebhook_unsupportedType_ignores() {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Unknown", PAYMENT_KEY, TRANSACTION_ID, null);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, never()).prepareWebhookEvent(anyString(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - data가 null인 경우 무시")
        void handleWebhook_nullData_ignores() {
            // given
            WebhookRequest request = new WebhookRequest("Transaction.Paid", null);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, never()).prepareWebhookEvent(anyString(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - paymentId가 null인 경우 무시")
        void handleWebhook_nullPaymentId_ignores() {
            // given
            WebhookRequest.WebhookData data = new WebhookRequest.WebhookData(null, TRANSACTION_ID, null);
            WebhookRequest request = new WebhookRequest("Transaction.Paid", data);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, never()).prepareWebhookEvent(anyString(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 멱등성 키 누락 시 무시 (Paid/Failed에서 transactionId 없음)")
        void handleWebhook_missingIdempotencyKey_ignores() {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, null, null);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, never()).prepareWebhookEvent(anyString(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("실패 - 멱등성 키 누락 시 무시 (Cancelled에서 cancellationId 없음)")
        void handleWebhook_missingCancellationId_ignores() {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Cancelled", PAYMENT_KEY, TRANSACTION_ID, null);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, never()).prepareWebhookEvent(anyString(), any(), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("handleWebhook() - 멱등성 처리")
    class HandleWebhookIdempotencyTest {

        @Test
        @DisplayName("멱등성 - prepareWebhookEvent가 null 반환 시 종료 (중복 웹훅)")
        void handleWebhook_duplicateWebhook_returns() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(
                    eq(TRANSACTION_ID), eq(WebhookEventType.TRANSACTION_PAID), eq(PAYMENT_KEY), anyString()
            )).willReturn(null); // 멱등성 체크 실패

            // when
            webhookService.handleWebhook(request);

            // then
            verify(paymentClient, never()).getPayment(anyString());
        }

        @Test
        @DisplayName("검증 - Transaction.Paid는 transactionId를 멱등성 키로 사용")
        void handleWebhook_paidType_usesTransactionIdAsKey() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(null); // 이후 처리는 불필요

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService).prepareWebhookEvent(
                    eq(TRANSACTION_ID), eq(WebhookEventType.TRANSACTION_PAID), eq(PAYMENT_KEY), anyString()
            );
        }

        @Test
        @DisplayName("검증 - Transaction.Cancelled는 cancellationId를 멱등성 키로 사용")
        void handleWebhook_cancelledType_usesCancellationIdAsKey() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Cancelled", PAYMENT_KEY, TRANSACTION_ID, CANCELLATION_ID);
            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Cancelled\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(null);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService).prepareWebhookEvent(
                    eq(CANCELLATION_ID), eq(WebhookEventType.TRANSACTION_CANCELLED), eq(PAYMENT_KEY), anyString()
            );
        }
    }

    @Nested
    @DisplayName("handleWebhook() - PortOne SDK 조회")
    class HandleWebhookPortOneVerificationTest {

        @Test
        @DisplayName("실패 - PortOne SDK 조회 실패 시 WebhookEvent를 FAILED로 표시")
        void handleWebhook_portOneFailure_marksEventFailed() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);

            CompletableFuture<io.portone.sdk.server.payment.Payment> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new RuntimeException("PortOne API error"));
            given(paymentClient.getPayment(PAYMENT_KEY)).willReturn(failedFuture);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).markEventFailed(eq(WEBHOOK_EVENT_ID), contains("포트원 SDK 조회 실패"));
            verify(webhookTransactionService, never()).getPaymentByKey(anyString());
        }

        @Test
        @DisplayName("성공 - PortOne SDK 조회 성공 후 Payment 조회")
        void handleWebhook_portOneSuccess_proceedsToPaymentLookup() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            PaidPayment paidPayment = mock(PaidPayment.class);
            given(paidPayment.getMethod()).willReturn(mock(io.portone.sdk.server.payment.PaymentMethod.class));

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(null); // Payment 없음 시나리오

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).getPaymentByKey(PAYMENT_KEY);
            verify(webhookTransactionService, times(1)).markEventComplete(WEBHOOK_EVENT_ID);
        }
    }

    @Nested
    @DisplayName("handleWebhook() - Payment 없음 케이스")
    class HandleWebhookNoPaymentTest {

        @Test
        @DisplayName("Payment 없음 - /confirm 대기 상태로 WebhookEvent COMPLETE 처리")
        void handleWebhook_paymentNotFound_marksEventComplete() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            PaidPayment paidPayment = mock(PaidPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(null);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).markEventComplete(WEBHOOK_EVENT_ID);
            verify(webhookTransactionService, never()).completePendingPayment(anyLong(), anyLong(), any());
        }
    }

    @Nested
    @DisplayName("handleWebhook() - Payment 최종 상태 케이스")
    class HandleWebhookTerminalStatusTest {

        @Test
        @DisplayName("Payment COMPLETED - 이미 처리 완료, WebhookEvent COMPLETE 처리")
        void handleWebhook_paymentCompleted_marksEventComplete() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.COMPLETED);
            PaidPayment paidPayment = mock(PaidPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).markEventComplete(WEBHOOK_EVENT_ID);
            verify(webhookTransactionService, never()).completePendingPayment(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("Payment REFUNDED - 이미 환불 완료, WebhookEvent COMPLETE 처리")
        void handleWebhook_paymentRefunded_marksEventComplete() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.REFUNDED);
            PaidPayment paidPayment = mock(PaidPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).markEventComplete(WEBHOOK_EVENT_ID);
        }

        @Test
        @DisplayName("Payment FAILED - 이미 실패 처리, WebhookEvent COMPLETE 처리")
        void handleWebhook_paymentFailed_marksEventComplete() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Failed", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.FAILED);
            FailedPayment failedPayment = mock(FailedPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Failed\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(failedPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).markEventComplete(WEBHOOK_EVENT_ID);
        }
    }

    @Nested
    @DisplayName("handleWebhook() - PENDING Payment 보정 (PaidPayment)")
    class HandleWebhookPendingCompensationTest {

        @Test
        @DisplayName("성공 - PENDING Payment를 PaidPayment로 보정 (Lock 획득)")
        void handleWebhook_pendingPayment_compensates() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            PaidPayment paidPayment = mock(PaidPayment.class);
            io.portone.sdk.server.payment.PaymentMethod portOneMethod = mock(io.portone.sdk.server.payment.PaymentMethod.class);
            given(paidPayment.getMethod()).willReturn(portOneMethod);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);
            given(redisUtil.acquireLock(anyString())).willReturn(true);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(redisUtil, times(1)).acquireLock(eq("payment:confirm:lock:" + PAYMENT_KEY));
            verify(webhookTransactionService, times(1)).completePendingPayment(eq(WEBHOOK_EVENT_ID), eq(PAYMENT_ID), any(PaymentMethod.class));
            verify(redisUtil, times(1)).releaseLock(eq("payment:confirm:lock:" + PAYMENT_KEY));
        }

        @Test
        @DisplayName("실패 - Lock 획득 실패 시 보정 스킵 (PortOne 재시도 대기)")
        void handleWebhook_lockAcquisitionFailed_skips() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            PaidPayment paidPayment = mock(PaidPayment.class);
            given(paidPayment.getMethod()).willReturn(mock(io.portone.sdk.server.payment.PaymentMethod.class));

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);
            given(redisUtil.acquireLock(anyString())).willReturn(false);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, never()).completePendingPayment(anyLong(), anyLong(), any());
            verify(redisUtil, never()).releaseLock(anyString());
        }

        @Test
        @DisplayName("검증 - Lock 해제는 finally 블록에서 실행 (예외 발생 시에도)")
        void handleWebhook_lockReleasedInFinally() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            PaidPayment paidPayment = mock(PaidPayment.class);
            given(paidPayment.getMethod()).willReturn(mock(io.portone.sdk.server.payment.PaymentMethod.class));

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(paidPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);
            given(redisUtil.acquireLock(anyString())).willReturn(true);
            doThrow(new RuntimeException("Transaction error"))
                    .when(webhookTransactionService).completePendingPayment(anyLong(), anyLong(), any());

            // when & then
            try {
                webhookService.handleWebhook(request);
            } catch (Exception e) {
                // 예외 발생 예상
            }

            verify(redisUtil, times(1)).releaseLock(eq("payment:confirm:lock:" + PAYMENT_KEY));
        }
    }

    @Nested
    @DisplayName("handleWebhook() - PENDING Payment 실패 처리")
    class HandleWebhookPendingFailureTest {

        @Test
        @DisplayName("성공 - PENDING Payment를 FailedPayment로 처리")
        void handleWebhook_failedPayment_failsPending() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Failed", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            FailedPayment failedPayment = mock(FailedPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Failed\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(failedPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).failPendingPayment(WEBHOOK_EVENT_ID, PAYMENT_ID);
        }

        @Test
        @DisplayName("성공 - PENDING Payment를 CancelledPayment로 처리")
        void handleWebhook_cancelledPayment_failsPending() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Cancelled", PAYMENT_KEY, TRANSACTION_ID, CANCELLATION_ID);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            CancelledPayment cancelledPayment = mock(CancelledPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Cancelled\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(cancelledPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).failPendingPayment(WEBHOOK_EVENT_ID, PAYMENT_ID);
        }

        @Test
        @DisplayName("성공 - PENDING Payment를 PartialCancelledPayment로 처리")
        void handleWebhook_partialCancelledPayment_failsPending() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Cancelled", PAYMENT_KEY, TRANSACTION_ID, CANCELLATION_ID);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            PartialCancelledPayment partialCancelledPayment = mock(PartialCancelledPayment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Cancelled\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(partialCancelledPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).failPendingPayment(WEBHOOK_EVENT_ID, PAYMENT_ID);
        }
    }

    @Nested
    @DisplayName("handleWebhook() - 알 수 없는 PortOne 상태")
    class HandleWebhookUnknownStatusTest {

        @Test
        @DisplayName("실패 - 알 수 없는 PortOne 상태 시 WebhookEvent FAILED 처리")
        void handleWebhook_unknownPortOneStatus_marksEventFailed() throws Exception {
            // given
            WebhookRequest request = createWebhookRequest("Transaction.Paid", PAYMENT_KEY, TRANSACTION_ID, null);
            WebhookEvent event = createMockWebhookEvent(WEBHOOK_EVENT_ID);
            Payment payment = createMockPayment(PAYMENT_ID, PaymentStatus.PENDING);
            io.portone.sdk.server.payment.Payment unknownPayment = mock(io.portone.sdk.server.payment.Payment.class);

            given(objectMapper.writeValueAsString(any())).willReturn("{\"type\":\"Transaction.Paid\"}");
            given(webhookTransactionService.prepareWebhookEvent(anyString(), any(), anyString(), anyString()))
                    .willReturn(event);
            given(paymentClient.getPayment(PAYMENT_KEY))
                    .willReturn(CompletableFuture.completedFuture(unknownPayment));
            given(webhookTransactionService.getPaymentByKey(PAYMENT_KEY)).willReturn(payment);

            // when
            webhookService.handleWebhook(request);

            // then
            verify(webhookTransactionService, times(1)).markEventFailed(eq(WEBHOOK_EVENT_ID), contains("알 수 없는 포트원 상태"));
        }
    }
}
