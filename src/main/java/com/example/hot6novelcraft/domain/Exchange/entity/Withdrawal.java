package com.example.hot6novelcraft.domain.Exchange.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Withdrawal extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId; // 작가 회원 아이디 직접 참조

    @Column(nullable = false)
    private Long bankAccountId; // 계좌 아이디 직접 참조

    @Column(nullable = false)
    private Integer requestAmount; // 신청 금액

    @Column(nullable = false)
    private Integer fee; // 수수료

    @Column(nullable = false)
    private Integer actualAmount; // 실수령 금액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status; // PENDING, PROCESSING, COMPLETED, REJECTED

    private LocalDateTime requestedAt; // 신청일

    private LocalDateTime processedAt; // 처리일

    private Withdrawal(Long authorId, Long bankAccountId, Integer requestAmount, Integer fee, WithdrawalStatus status) {
        this.authorId = authorId;
        this.bankAccountId = bankAccountId;
        this.requestAmount = requestAmount;
        this.fee = fee;
        this.actualAmount = requestAmount - fee;
        this.status = status;
        this.requestedAt = LocalDateTime.now();
    }

    // 환전 신청 시 정적 팩토리 메서드
    public static Withdrawal request(Long authorId, Long bankAccountId, Integer requestAmount, Integer fee) {
        return new Withdrawal(authorId, bankAccountId, requestAmount, fee, WithdrawalStatus.PENDING);
    }

    // 상태 변경 및 처리일 기록
    public void complete() {
        this.status = WithdrawalStatus.COMPLETED;
        this.processedAt = LocalDateTime.now();
    }
}
