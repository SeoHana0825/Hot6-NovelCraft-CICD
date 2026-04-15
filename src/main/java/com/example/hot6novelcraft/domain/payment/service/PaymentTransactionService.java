package com.example.hot6novelcraft.domain.payment.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.PaymentExceptionEnum;
import com.example.hot6novelcraft.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.hot6novelcraft.domain.payment.dto.response.PaymentPrepareResponse;
import com.example.hot6novelcraft.domain.payment.entity.Payment;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentMethod;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentStatus;
import com.example.hot6novelcraft.domain.payment.repository.PaymentRepository;
import com.example.hot6novelcraft.domain.point.service.PointService;
import com.example.hot6novelcraft.domain.purchases.entity.Purchase;
import com.example.hot6novelcraft.domain.purchases.entity.enums.PurchaseType;
import com.example.hot6novelcraft.domain.purchases.repository.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 관련 DB 트랜잭션 전담 서비스.
 * 외부 API 호출 없이 DB 작업만 수행하여 트랜잭션 점유 시간을 최소화한다.
 * 오케스트레이션은 {@link PaymentService}가 담당한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentTransactionService {

    private final PaymentRepository paymentRepository;
    private final PurchaseRepository purchaseRepository;
    private final PointService pointService;

    /**
     * 결제창 열기 전 PENDING Payment를 미리 생성한다.
     * 서버가 paymentKey를 생성하여 반환하면 프론트가 PortOne SDK에 전달한다.
     * 이를 통해 결제창 이용 중 토큰이 만료되어도 웹훅으로 포인트 충전이 가능하다.
     */
    @Transactional
    public PaymentPrepareResponse preparePendingPayment(Long userId, Long amount) {
        String paymentKey = "payment-" + UUID.randomUUID().toString().replace("-", "");
        Payment payment = paymentRepository.save(
                Payment.create(userId, paymentKey, amount, PaymentMethod.CARD)
        );
        log.info("[결제 준비] PENDING 생성 userId={} paymentKey={}", userId, paymentKey);
        return new PaymentPrepareResponse(payment.getPaymentKey(), amount);
    }

    /**
     * /prepare로 생성된 PENDING Payment를 조회하고 소유자·금액을 검증한 뒤 반환한다.
     * /prepare 없이 /confirm을 직접 호출하는 경우는 허용하지 않는다.
     */
    @Transactional
    public Payment savePendingPayment(Long userId, PaymentConfirmRequest request) {
        Payment existing = paymentRepository.findByPaymentKey(request.paymentId())
                .orElseThrow(() -> {
                    log.warn("[결제] /prepare 없이 /confirm 호출 userId={} paymentKey={}", userId, request.paymentId());
                    return new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_NOT_FOUND);
                });

        if (!existing.getUserId().equals(userId)) {
            log.warn("[결제] 소유자 불일치 userId={} paymentKey={}", userId, request.paymentId());
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_NOT_MY_ORDER);
        }

        if (!existing.getAmount().equals(request.amount())) {
            log.warn("[결제] 금액 불일치 userId={} prepare금액={} confirm금액={}",
                    userId, existing.getAmount(), request.amount());
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_AMOUNT_MISMATCH);
        }

        if (existing.getStatus() != PaymentStatus.PENDING) {
            log.warn("[결제] 이미 처리된 결제 userId={} paymentKey={} status={}",
                    userId, request.paymentId(), existing.getStatus());
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_ALREADY_PAID);
        }

        log.info("[결제] PENDING 재사용 dbPaymentId={}", existing.getId());
        return existing;
    }

    /**
     * 포트원 검증 완료 후 결제를 COMPLETED로 전환하고 포인트를 충전한다.
     * completeIfPending으로 원자적 UPDATE를 수행하여 웹훅 보정 처리와의 중복 충전을 방지한다.
     * 벌크 UPDATE 후 재조회하여 영속성 컨텍스트 비동기화 문제를 방지한다.
     *
     * @return DB 최신 상태의 payment (updated=0이면 웹훅이 먼저 처리한 것이므로 충전 스킵)
     */
    @Transactional
    public Payment completePayment(Long paymentId, Long userId, Long amount, PaymentMethod method) {
        int updated = paymentRepository.completeIfPending(paymentId, PaymentStatus.COMPLETED, method);

        // 벌크 UPDATE 후 재조회 — JPQL 벌크 UPDATE는 1차 캐시를 우회하므로 항상 DB에서 읽어야 한다
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_NOT_FOUND));

        if (updated == 0) {
            log.info("[결제] 이미 처리됨 (웹훅 선처리) dbPaymentId={}", paymentId);
            return payment;
        }

        log.info("[결제] COMPLETED 전환 dbPaymentId={} method={}", paymentId, method);

        pointService.charge(userId, amount);
        log.info("[결제] 포인트 충전 완료 userId={} amount={}P", userId, amount);

        purchaseRepository.save(Purchase.create(userId, PurchaseType.POINT, amount, paymentId));
        log.info("[결제] 구매 이력 저장 완료 userId={} dbPaymentId={}", userId, paymentId);

        return payment;
    }

    /**
     * 결제를 FAILED 상태로 전환한다.
     * PENDING 상태일 때만 전환하여 웹훅이 선처리한 COMPLETED 결제를 되돌리는 것을 방지한다.
     */
    @Transactional
    public void failPayment(Long paymentId) {
        paymentRepository.findById(paymentId).ifPresent(payment -> {
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("[결제] FAILED 전환 스킵 - 이미 최종 상태 dbPaymentId={} status={}",
                        paymentId, payment.getStatus());
                return;
            }
            payment.fail();
            log.info("[결제] FAILED 전환 dbPaymentId={}", paymentId);
        });
    }

    /**
     * 환불 대상 결제를 조회하고 환불 가능 여부를 검증한다.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentForCancel(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> {
                    if (paymentRepository.existsById(paymentId)) {
                        log.warn("[환불] 다른 사용자 결제 접근 시도 userId={} paymentId={}", userId, paymentId);
                        return new ServiceErrorException(PaymentExceptionEnum.ERR_NOT_MY_ORDER);
                    }
                    log.warn("[환불] 존재하지 않는 결제 userId={} paymentId={}", userId, paymentId);
                    return new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_NOT_FOUND);
                });

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            log.warn("[환불] 환불 불가 상태 userId={} paymentId={} status={}", userId, paymentId, payment.getStatus());
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_INVALID_COMPLETE);
        }
        return payment;
    }

    /**
     * 포트원 환불 완료 후 결제를 REFUNDED로 전환한다.
     * 포인트 차감은 PaymentService에서 PortOne 호출 전에 선차감하므로 여기서는 상태 전환만 수행한다.
     * cancelIfCompleted로 원자적 UPDATE를 수행하여 중복 환불 요청 시 포인트 중복 차감을 방지한다.
     * 벌크 UPDATE 후 재조회하여 영속성 컨텍스트 비동기화 문제를 방지한다.
     */
    @Transactional
    public Payment finalizeCancel(Long paymentId) {
        int updated = paymentRepository.cancelIfCompleted(paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ServiceErrorException(PaymentExceptionEnum.ERR_PAYMENT_NOT_FOUND));

        if (updated == 0) {
            // 동시 요청으로 이미 REFUNDED 전환된 케이스 — 포인트·PortOne은 이미 처리됐으므로 그대로 반환
            log.warn("[환불] REFUNDED 전환 스킵 - 이미 최종 상태 dbPaymentId={} status={}", paymentId, payment.getStatus());
            return payment;
        }

        log.info("[환불] REFUNDED 전환 dbPaymentId={}", paymentId);
        return payment;
    }

}
