package com.example.hot6novelcraft.domain.payment.repository;

import com.example.hot6novelcraft.domain.payment.entity.Payment;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentMethod;
import com.example.hot6novelcraft.domain.payment.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentKey(String paymentKey);

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByIdAndUserId(Long id, Long userId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

    /**
     * PENDING 상태인 경우에만 COMPLETED로 전환한다.
     * /confirm과 웹훅이 동시에 처리를 시도할 때 먼저 성공한 쪽만 1을 반환하고,
     * 나머지는 0을 반환하여 포인트 중복 충전을 원자적으로 방지한다.
     *
     * @return 업데이트된 행 수 (1이면 처리 성공, 0이면 이미 처리됨)
     */
    @Modifying
    @Query("UPDATE Payment p SET p.status = :status, p.method = :method WHERE p.id = :id AND p.status = 'PENDING'")
    int completeIfPending(@Param("id") Long id,
                          @Param("status") PaymentStatus status,
                          @Param("method") PaymentMethod method);

    /**
     * COMPLETED 상태인 경우에만 REFUNDED로 전환한다.
     * 중복 환불 요청이 동시에 들어와도 먼저 성공한 쪽만 1을 반환하고,
     * 나머지는 0을 반환하여 포인트 중복 차감을 원자적으로 방지한다.
     *
     * @return 업데이트된 행 수 (1이면 처리 성공, 0이면 이미 처리됨)
     */
    @Modifying
    @Query("UPDATE Payment p SET p.status = 'REFUNDED', p.cancelledAt = CURRENT_TIMESTAMP WHERE p.id = :id AND p.status = 'COMPLETED'")
    int cancelIfCompleted(@Param("id") Long id);
}
