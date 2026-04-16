package com.example.hot6novelcraft.domain.payment.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.PaymentExceptionEnum;
import com.example.hot6novelcraft.domain.payment.entity.Payment;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentMethod;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentStatus;
import com.example.hot6novelcraft.domain.payment.repository.PaymentRepository;
import com.example.hot6novelcraft.domain.point.service.PointService;
import com.example.hot6novelcraft.domain.purchases.entity.Purchase;
import com.example.hot6novelcraft.domain.purchases.entity.enums.PurchaseType;
import com.example.hot6novelcraft.domain.purchases.repository.PurchaseRepository;
import com.example.hot6novelcraft.domain.webhookevent.entity.WebhookEvent;
import com.example.hot6novelcraft.domain.webhookevent.entity.WebhookEventStatus;
import com.example.hot6novelcraft.domain.webhookevent.entity.WebhookEventType;
import com.example.hot6novelcraft.domain.webhookevent.repository.WebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 웹훅 관련 DB 트랜잭션 전담 서비스.
 * 외부 API 호출 없이 DB 작업만 수행하여 트랜잭션 점유 시간을 최소화한다.
 * 오케스트레이션은 {@link WebhookService}가 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookTransactionService {

    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final PointService pointService;

    /**
     * 멱등성 체크 후 WebhookEvent를 준비한다.
     *
     * @return null이면 이미 처리 완료된 이벤트 (처리 skip),
     *         non-null이면 신규 또는 재시도 이벤트 (처리 계속)
     */
    @Transactional
    public WebhookEvent prepareWebhookEvent(String transactionId, WebhookEventType eventType,
                                            String paymentId, String rawPayload) {
        WebhookEvent existing = webhookEventRepository.findByWebhookId(transactionId).orElse(null);

        if (existing != null && existing.getStatus() == WebhookEventStatus.COMPLETE) {
            log.info("웹훅 중복 수신 무시 transactionId={}", transactionId);
            return null;
        }

        if (existing != null) {
            return existing;
        }

        WebhookEvent event = WebhookEvent.create(transactionId, eventType, paymentId, rawPayload);
        webhookEventRepository.save(event);
        log.info("웹훅 이벤트 생성 transactionId={} type={}", transactionId, eventType);
        return event;
    }

    /**
     * paymentKey로 Payment를 조회한다.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByKey(String paymentKey) {
        return paymentRepository.findByPaymentKey(paymentKey).orElse(null);
    }

    /**
     * WebhookEvent를 COMPLETE 상태로 전환한다.
     */
    @Transactional
    public void markEventComplete(Long webhookEventId) {
        webhookEventRepository.findById(webhookEventId).ifPresent(event -> {
            event.complete();
            log.info("웹훅 이벤트 COMPLETE 처리 webhookEventId={}", webhookEventId);
        });
    }

    /**
     * WebhookEvent를 FAIL 상태로 전환한다.
     */
    @Transactional
    public void markEventFailed(Long webhookEventId, String reason) {
        webhookEventRepository.findById(webhookEventId).ifPresent(event -> {
            event.fail(reason);
            log.info("웹훅 이벤트 FAIL 처리 webhookEventId={} reason={}", webhookEventId, reason);
        });
    }

    /**
     * [결제 실패 처리] PENDING 상태인 결제를 FAILED로 전환한다.
     * 포인트 변동 없이 상태만 갱신한다.
     */
    @Transactional
    public void failPendingPayment(Long webhookEventId, Long paymentDbId) {
        paymentRepository.findById(paymentDbId).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PENDING) {
                payment.fail();
                log.info("결제 실패 처리 paymentDbId={}", paymentDbId);
            }
        });
        webhookEventRepository.findById(webhookEventId).ifPresent(event -> {
            event.complete();
            log.info("웹훅 이벤트 COMPLETE 처리 webhookEventId={}", webhookEventId);
        });
    }

    /**
     * [/confirm 누락 보정] PENDING 상태인 결제를 COMPLETED로 전환하고 포인트를 충전한다.
     * Redis Lock으로 /confirm과의 상호 배제가 보장되므로 원자적 UPDATE 없이 직접 전환한다.
     */
    @Transactional
    public void completePendingPayment(Long webhookEventId, Long paymentDbId, PaymentMethod method) {
        Payment payment = paymentRepository.findById(paymentDbId)
                .orElseThrow(() -> new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_NOT_FOUND));

        // Redis Lock으로 이 케이스는 발생하지 않지만 방어 코드
        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("웹훅 보정 스킵 - 이미 처리됨 paymentDbId={} status={}", paymentDbId, payment.getStatus());
            webhookEventRepository.findById(webhookEventId).ifPresent(WebhookEvent::complete);
            return;
        }

        payment.complete(method);

        pointService.charge(payment.getUserId(), payment.getAmount());
        purchaseRepository.save(
                Purchase.create(payment.getUserId(), PurchaseType.POINT, payment.getAmount(), paymentDbId)
        );
        webhookEventRepository.findById(webhookEventId).ifPresent(WebhookEvent::complete);
        log.info("웹훅 보정 완료 (/confirm 누락) paymentDbId={} userId={}", paymentDbId, payment.getUserId());
    }
}
