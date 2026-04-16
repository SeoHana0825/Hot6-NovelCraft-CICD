package com.example.hot6novelcraft.domain.payment.repository;

import com.example.hot6novelcraft.domain.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    boolean existsByPaymentKey(String paymentKey);

    Optional<Payment> findByPaymentKey(String paymentKey);

    Optional<Payment> findByIdAndUserId(Long id, Long userId);

    Page<Payment> findByUserId(Long userId, Pageable pageable);

}
