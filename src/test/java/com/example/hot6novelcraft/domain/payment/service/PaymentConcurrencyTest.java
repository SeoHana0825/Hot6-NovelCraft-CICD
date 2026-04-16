package com.example.hot6novelcraft.domain.payment.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.PaymentExceptionEnum;
import com.example.hot6novelcraft.common.security.RedisUtil;
import com.example.hot6novelcraft.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.hot6novelcraft.domain.payment.entity.Payment;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentMethod;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentStatus;
import com.example.hot6novelcraft.domain.payment.repository.PaymentRepository;
import com.example.hot6novelcraft.domain.point.service.PointService;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentAmount;
import io.portone.sdk.server.payment.PaymentClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 동시성 테스트 특성상 일부 stub이 실행 안 될 수 있음
@DisplayName("결제 동시성 테스트")
class PaymentConcurrencyTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentTransactionService paymentTransactionService;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private PointService pointService;
    @Mock
    private RedisUtil redisUtil;

    private static final Long USER_ID = 1L;
    private static final Long PAYMENT_ID = 10L;
    private static final Long AMOUNT = 10_000L;
    private static final String PAYMENT_KEY = "payment-test-abc123";
    private static final String CANCEL_REASON = "테스트 환불";
    private static final String MOCK_LOCK_TOKEN = "mock-lock-token";

    /**
     * status=COMPLETED 인 Payment mock 생성.
     * 반드시 given().willReturn() 밖에서 미리 생성 후 사용해야 한다.
     * (given() 내부에서 mock 생성 시 Mockito UnfinishedStubbingException 발생)
     */
    private Payment completedPaymentMock() {
        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(PAYMENT_ID);
        given(payment.getUserId()).willReturn(USER_ID);
        given(payment.getAmount()).willReturn(AMOUNT);
        given(payment.getPaymentKey()).willReturn(PAYMENT_KEY);
        given(payment.getStatus()).willReturn(PaymentStatus.COMPLETED);
        return payment;
    }

    private Payment refundedPaymentMock() {
        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(PAYMENT_ID);
        given(payment.getStatus()).willReturn(PaymentStatus.REFUNDED);
        return payment;
    }

    // =========================================================
    // 환불(cancelPayment) 동시성 테스트
    // =========================================================
    @Nested
    @DisplayName("환불 동시성 테스트")
    class CancelPaymentConcurrencyTest {

        @Test
        @DisplayName("락 획득 실패 시 ERR_PAYMENT_ALREADY_CANCELING 예외 반환")
        void cancelPayment_whenLockNotAcquired_throwsAlreadyCanceling() {
            // given
            given(redisUtil.acquireLock(anyString(), anyLong())).willReturn((String) null);

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment(USER_ID, PAYMENT_ID, CANCEL_REASON))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(PaymentExceptionEnum.ERR_PAYMENT_ALREADY_CANCELING.getMessage());

            verify(paymentTransactionService, never()).getPaymentForCancel(any(), any());
            verify(pointService, never()).deduct(any(), any());
        }

        @Test
        @DisplayName("동시 환불 요청 2개 - 락으로 포인트 차감 1번만 실행")
        void cancelPayment_concurrentTwoRequests_deductCalledOnlyOnce() throws InterruptedException {
            // given
            // AtomicBoolean으로 Redis SET NX 동작 시뮬레이션
            // compareAndSet(false, true): 첫 번째 스레드만 토큰 반환, 나머지는 null
            AtomicBoolean lockHeld = new AtomicBoolean(false);
            given(redisUtil.acquireLock(anyString(), anyLong()))
                    .willAnswer(inv -> lockHeld.compareAndSet(false, true) ? MOCK_LOCK_TOKEN : null);

            // ★ mock은 given().willReturn() 밖에서 미리 생성
            Payment completedPayment = completedPaymentMock();
            given(paymentTransactionService.getPaymentForCancel(USER_ID, PAYMENT_ID))
                    .willReturn(completedPayment);

            // paymentClient.cancelPayment는 stub 없이 null 반환 → .get() NullPointerException
            // → catch(Exception e)에서 포착 → compensateDeduct() → ERR_PORTONE_API_ERROR
            // 이 경로도 "포인트 차감이 1번만 발생했는가"를 증명하는 데 유효하다

            int threadCount = 2;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger alreadyCancelingCount = new AtomicInteger(0);
            AtomicInteger otherErrorCount = new AtomicInteger(0);
            List<Exception> unexpectedErrors = new ArrayList<>();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        paymentService.cancelPayment(USER_ID, PAYMENT_ID, CANCEL_REASON);
                    } catch (ServiceErrorException e) {
                        if (e.getMessage().equals(
                                PaymentExceptionEnum.ERR_PAYMENT_ALREADY_CANCELING.getMessage())) {
                            alreadyCancelingCount.incrementAndGet(); // 락 획득 실패
                        } else {
                            otherErrorCount.incrementAndGet(); // PortOne 실패 등
                        }
                    } catch (Exception e) {
                        unexpectedErrors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // then
            assertThat(completed).isTrue();
            assertThat(unexpectedErrors).isEmpty();

            // 락을 획득 못한 스레드: 정확히 1개
            assertThat(alreadyCancelingCount.get()).isEqualTo(1);
            // 락을 획득한 스레드: 에러 없이 성공 또는 PortOne 에러 = 1개
            assertThat(alreadyCancelingCount.get() + otherErrorCount.get()).isEqualTo(2);

            // ★ 핵심 검증: 포인트 차감은 락을 획득한 스레드만 수행 → 정확히 1번
            verify(pointService, times(1)).deduct(USER_ID, AMOUNT);
        }

        @Test
        @DisplayName("10개 동시 요청 중 락을 획득한 1개만 포인트 차감 실행")
        void cancelPayment_tenConcurrentRequests_deductCalledOnlyOnce() throws InterruptedException {
            // given
            AtomicBoolean lockHeld = new AtomicBoolean(false);
            given(redisUtil.acquireLock(anyString(), anyLong()))
                    .willAnswer(inv -> lockHeld.compareAndSet(false, true) ? MOCK_LOCK_TOKEN : null);

            // ★ mock 미리 생성
            Payment completedPayment = completedPaymentMock();
            given(paymentTransactionService.getPaymentForCancel(USER_ID, PAYMENT_ID))
                    .willReturn(completedPayment);

            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger alreadyCancelingCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        paymentService.cancelPayment(USER_ID, PAYMENT_ID, CANCEL_REASON);
                    } catch (ServiceErrorException e) {
                        if (e.getMessage().equals(
                                PaymentExceptionEnum.ERR_PAYMENT_ALREADY_CANCELING.getMessage())) {
                            alreadyCancelingCount.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // then
            // 락 실패: 9개 (10개 중 1개만 락 획득)
            assertThat(alreadyCancelingCount.get()).isEqualTo(9);

            // ★ 핵심 검증: 포인트 차감은 단 1번만
            verify(pointService, times(1)).deduct(USER_ID, AMOUNT);
        }

        @Test
        @DisplayName("포트원 실패 시 포인트 복구 후 락 해제")
        void cancelPayment_whenPortOneFails_compensatesAndReleasesLock() {
            // given
            given(redisUtil.acquireLock(anyString(), anyLong())).willReturn(MOCK_LOCK_TOKEN);

            Payment completedPayment = completedPaymentMock();
            given(paymentTransactionService.getPaymentForCancel(USER_ID, PAYMENT_ID))
                    .willReturn(completedPayment);

            // paymentClient.cancelPayment stub 없음 → null 반환 → .get() NPE → compensateDeduct

            // when & then
            assertThatThrownBy(() -> paymentService.cancelPayment(USER_ID, PAYMENT_ID, CANCEL_REASON))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(PaymentExceptionEnum.ERR_PORTONE_API_ERROR.getMessage());

            // 포인트 선차감 후 복구가 실행되어야 한다
            verify(pointService, times(1)).deduct(USER_ID, AMOUNT);
            verify(pointService, times(1)).compensateDeduct(USER_ID, AMOUNT);

            // finally 블록에서 락이 반드시 해제되어야 한다
            verify(redisUtil, times(1)).releaseLock(
                    eq("payment:cancel:lock:" + PAYMENT_ID), anyString());
        }
    }

    // =========================================================
    // 결제 확인(confirmPayment) 동시성 테스트
    // =========================================================
    @Nested
    @DisplayName("결제 확인 동시성 테스트")
    class ConfirmPaymentConcurrencyTest {

        private PaymentConfirmRequest confirmRequest() {
            return new PaymentConfirmRequest(PAYMENT_KEY, AMOUNT);
        }

        @Test
        @DisplayName("락 획득 실패 시 ERR_PAYMENT_PROCESSING 예외 반환")
        void confirmPayment_whenLockNotAcquired_throwsPaymentProcessing() {
            // given
            given(redisUtil.acquireLock(anyString(), anyLong())).willReturn((String) null);

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, confirmRequest()))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(PaymentExceptionEnum.ERR_PAYMENT_PROCESSING.getMessage());

            verify(paymentTransactionService, never()).savePendingPayment(any(), any());
            verify(pointService, never()).charge(any(), any());
        }

        @Test
        @DisplayName("동시 결제 확인 2개 - 락으로 포인트 충전 1번만 실행")
        void confirmPayment_concurrentTwoRequests_chargeCalledOnlyOnce() throws InterruptedException {
            // given
            AtomicBoolean lockHeld = new AtomicBoolean(false);
            given(redisUtil.acquireLock(anyString(), anyLong()))
                    .willAnswer(inv -> lockHeld.compareAndSet(false, true) ? MOCK_LOCK_TOKEN : null);

            // ★ mock 미리 생성
            Payment pendingPayment = mock(Payment.class);
            given(pendingPayment.getId()).willReturn(PAYMENT_ID);
            given(paymentTransactionService.savePendingPayment(anyLong(), any()))
                    .willReturn(pendingPayment);

            PaidPayment paidPayment = mock(PaidPayment.class);
            PaymentAmount paymentAmount = mock(PaymentAmount.class);
            given(paymentAmount.getTotal()).willReturn(AMOUNT);
            given(paidPayment.getAmount()).willReturn(paymentAmount);
            given(paidPayment.getMethod()).willReturn(null);

            Payment completedPayment = mock(Payment.class);
            given(completedPayment.getId()).willReturn(PAYMENT_ID);
            given(completedPayment.getStatus()).willReturn(PaymentStatus.COMPLETED);
            given(paymentTransactionService.completePayment(anyLong(), anyLong(), anyLong(), any()))
                    .willReturn(completedPayment);
            given(paymentClient.getPayment(anyString()))
                    .willReturn(java.util.concurrent.CompletableFuture.completedFuture(paidPayment));

            int threadCount = 2;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger processingCount = new AtomicInteger(0);
            List<Exception> unexpectedErrors = new ArrayList<>();

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            // when
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        paymentService.confirmPayment(USER_ID, confirmRequest());
                        successCount.incrementAndGet();
                    } catch (ServiceErrorException e) {
                        if (e.getMessage().equals(
                                PaymentExceptionEnum.ERR_PAYMENT_PROCESSING.getMessage())) {
                            processingCount.incrementAndGet();
                        } else {
                            unexpectedErrors.add(e);
                        }
                    } catch (Exception e) {
                        unexpectedErrors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // then
            assertThat(unexpectedErrors).isEmpty();
            assertThat(successCount.get()).isEqualTo(1);
            assertThat(processingCount.get()).isEqualTo(1);

            // ★ 핵심 검증: 포인트 충전(completePayment)은 락을 획득한 스레드만 수행 → 1번
            verify(paymentTransactionService, times(1))
                    .completePayment(anyLong(), anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("포트원 검증 중 예외 발생 시 finally에서 락 반드시 해제")
        void confirmPayment_whenPortOneFails_lockMustBeReleased() {
            // given
            given(redisUtil.acquireLock(anyString(), anyLong())).willReturn(MOCK_LOCK_TOKEN);

            Payment pendingPayment = mock(Payment.class);
            given(pendingPayment.getId()).willReturn(PAYMENT_ID);
            given(paymentTransactionService.savePendingPayment(anyLong(), any()))
                    .willReturn(pendingPayment);
            given(paymentClient.getPayment(anyString()))
                    .willReturn(java.util.concurrent.CompletableFuture
                            .failedFuture(new RuntimeException("PortOne 연결 오류")));

            // when & then
            assertThatThrownBy(() -> paymentService.confirmPayment(USER_ID, confirmRequest()))
                    .isInstanceOf(ServiceErrorException.class)
                    .hasMessage(PaymentExceptionEnum.ERR_PORTONE_API_ERROR.getMessage());

            // 예외가 발생해도 finally 블록에서 락이 반드시 해제되어야 한다
            verify(redisUtil, times(1)).releaseLock(
                    eq("payment:confirm:lock:" + PAYMENT_KEY), anyString());
        }
    }
}
