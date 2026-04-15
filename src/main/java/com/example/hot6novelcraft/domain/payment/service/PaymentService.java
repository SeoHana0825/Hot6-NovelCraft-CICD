package com.example.hot6novelcraft.domain.payment.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.PaymentExceptionEnum;
import com.example.hot6novelcraft.domain.payment.dto.request.PaymentConfirmRequest;
import com.example.hot6novelcraft.domain.payment.dto.response.PaymentPrepareResponse;
import com.example.hot6novelcraft.domain.payment.dto.response.PaymentResponse;
import com.example.hot6novelcraft.domain.payment.entity.Payment;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentMethod;
import com.example.hot6novelcraft.domain.point.service.PointService;
import io.portone.sdk.server.payment.PaidPayment;
import io.portone.sdk.server.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 결제 흐름 오케스트레이터.
 * DB 트랜잭션({@link PaymentTransactionService})과 외부 API 호출을 분리하여
 * 트랜잭션 점유 중 외부 API 대기가 발생하지 않도록 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionService paymentTransactionService;
    private final PaymentClient paymentClient;
    private final PointService pointService;

    /**
     * 결제창 열기 전 PENDING Payment 미리 생성
     */
    public PaymentPrepareResponse preparePayment(Long userId, Long amount) {
        log.info("[결제 준비] 요청 userId={} amount={}", userId, amount);
        return paymentTransactionService.preparePendingPayment(userId, amount);
    }

    /**
     * 포트원 V2 결제 확인 및 포인트 충전
     *
     * 흐름:
     * 1. [TX] /prepare로 생성된 PENDING Payment 재사용 (없으면 신규 생성)
     * 2. [외부 API] 포트원 V2 SDK로 실제 결제 정보 조회 및 금액·상태 검증
     * 3. [TX] 검증 통과 시 COMPLETED 전환 + 포인트 충전 + 구매 이력 저장
     */
    public PaymentResponse confirmPayment(Long userId, PaymentConfirmRequest request) {
        log.info("[결제] 확인 시작 userId={} paymentId={} amount={}",
                userId, request.paymentId(), request.amount());

        // 1. 중복 결제 체크 + PENDING 저장 (짧은 트랜잭션)
        Payment pendingPayment = paymentTransactionService.savePendingPayment(userId, request);

        try {
            // 2. 포트원 V2 SDK 조회 (트랜잭션 밖 — DB 커넥션 미점유)
            log.info("[결제] 포트원 SDK 검증 시작 paymentKey={}", request.paymentId());
            io.portone.sdk.server.payment.Payment portOnePayment =
                    paymentClient.getPayment(request.paymentId()).get();

            // 3. 결제 완료(PAID) 상태 확인
            if (!(portOnePayment instanceof PaidPayment paidPayment)) {
                paymentTransactionService.failPayment(pendingPayment.getId());
                log.warn("[결제] 결제 미완료 상태 userId={} portoneType={}",
                        userId, portOnePayment.getClass().getSimpleName());
                throw new ServiceErrorException(PaymentExceptionEnum.ERR_INVALID_PENDING);
            }

            // 4. 금액 위변조 검증
            long actualAmount = paidPayment.getAmount().getTotal();
            if (actualAmount != request.amount()) {
                paymentTransactionService.failPayment(pendingPayment.getId());
                log.warn("[결제] 금액 위변조 감지 userId={} 요청금액={} 실제금액={}",
                        userId, request.amount(), actualAmount);
                throw new ServiceErrorException(PaymentExceptionEnum.ERR_AMOUNT_MISMATCH);
            }
            log.info("[결제] 금액 검증 통과 amount={}", request.amount());

            // 5. COMPLETED 전환 + 포인트 충전 + 구매 이력 저장 (짧은 트랜잭션)
            PaymentMethod resolvedMethod = PaymentMethod.from(paidPayment.getMethod());
            Payment completedPayment = paymentTransactionService.completePayment(
                    pendingPayment.getId(), userId, request.amount(), resolvedMethod
            );
            log.info("[결제] 결제 프로세스 완료 userId={} dbPaymentId={} amount={}",
                    userId, completedPayment.getId(), request.amount());
            return PaymentResponse.from(completedPayment);

        } catch (ServiceErrorException e) {
            throw e;
        } catch (Exception e) {
            paymentTransactionService.failPayment(pendingPayment.getId());
            log.error("[결제] 예상치 못한 오류 userId={} paymentKey={}", userId, request.paymentId(), e);
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_PORTONE_API_ERROR);
        }
    }

    /**
     * 전액 환불
     *
     * 흐름:
     * 1. [TX-readOnly] 결제 조회 + 환불 가능 상태 검증
     * 2. 포인트 잔액 검증 (충전 금액 전액이 남아있어야 환불 가능)
     * 3. [외부 API] 포트원 V2 SDK로 환불 요청
     * 4. [TX] REFUNDED 전환 + 포인트 차감
     */
    public PaymentResponse cancelPayment(Long userId, Long paymentId, String reason) {
        log.info("[환불] 요청 시작 userId={} paymentId={} reason={}", userId, paymentId, reason);

        // 1. 결제 조회 + 상태 검증 (읽기 전용 트랜잭션)
        Payment payment = paymentTransactionService.getPaymentForCancel(userId, paymentId);
        log.info("[환불] 결제 조회 완료 paymentKey={} status={} amount={}",
                payment.getPaymentKey(), payment.getStatus(), payment.getAmount());

        // 2. 포인트 잔액 검증 — PortOne 호출 전에 수행해야 함
        // 충전 후 일부 사용한 경우 잔액이 충전 금액보다 적을 수 있으므로 환불 불가 처리
        long currentBalance = pointService.getBalance(userId);
        if (currentBalance < payment.getAmount()) {
            log.warn("[환불] 포인트 잔액 부족 환불 불가 userId={} 잔액={}P 결제금액={}P",
                    userId, currentBalance, payment.getAmount());
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_INSUFFICIENT_POINT);
        }

        try {
            // 3. 포트원 SDK 환불 요청 (트랜잭션 밖 — DB 커넥션 미점유)
            paymentClient.cancelPayment(payment.getPaymentKey(), null, null, null,
                    reason, null, null, null, null, null, null).get();
            log.info("[환불] 포트원 환불 완료 paymentKey={}", payment.getPaymentKey());
        } catch (Exception e) {
            log.error("[환불] 포트원 환불 실패 paymentKey={}", payment.getPaymentKey(), e);
            throw new ServiceErrorException(PaymentExceptionEnum.ERR_PORTONE_API_ERROR);
        }

        // 4. REFUNDED 전환 + 포인트 차감 (짧은 트랜잭션)
        Payment cancelledPayment = paymentTransactionService.finalizeCancel(
                payment.getId(), userId, payment.getAmount()
        );
        log.info("[환불] 환불 프로세스 완료 userId={} dbPaymentId={}", userId, paymentId);
        return PaymentResponse.from(cancelledPayment);
    }


}
