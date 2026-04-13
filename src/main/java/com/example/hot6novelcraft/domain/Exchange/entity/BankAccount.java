package com.example.hot6novelcraft.domain.Exchange.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "bank_accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BankAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 회원 아이디 직접 참조

    @Column(nullable = false, length = 50)
    private String bankName; // 은행명

    @Column(nullable = false, length = 50)
    private String accountNumber; // 계좌 번호

    @Column(nullable = false, length = 50)
    private String accountHolder; // 예금주

    @Column(nullable = false)
    private Boolean isVerified; // 인증 여부

    private BankAccount(Long userId, String bankName, String accountNumber, String accountHolder, Boolean isVerified) {
        this.userId = userId;
        this.bankName = bankName;
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder;
        this.isVerified = isVerified;
    }

    // 계좌 등록 시 정적 팩토리 메서드
    public static BankAccount create(Long userId, String bankName, String accountNumber, String accountHolder) {
        return new BankAccount(userId, bankName, accountNumber, accountHolder, false);
    }

    // 계좌 인증 완료 처리
    public void verify() {
        this.isVerified = true;
    }
}
