package com.example.hot6novelcraft.domain.exchange.dto.response;

public record RevenueOverviewResponse(
        Integer totalEarned,           // 총 누적 수익
        Integer totalWithdrawn,        // 총 환전 금액
        Integer availableBalance,      // 현재 가용 잔액
        BankAccountInfoResponse bankAccount  // 인증된 계좌 정보 (없으면 null)
) {
    public static RevenueOverviewResponse of(
            Integer totalEarned,
            Integer totalWithdrawn,
            Integer availableBalance,
            BankAccountInfoResponse bankAccount
    ) {
        return new RevenueOverviewResponse(totalEarned, totalWithdrawn, availableBalance, bankAccount);
    }
}