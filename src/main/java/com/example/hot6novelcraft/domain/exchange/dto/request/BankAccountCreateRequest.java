package com.example.hot6novelcraft.domain.exchange.dto.request;

import jakarta.validation.constraints.NotBlank;

public record BankAccountCreateRequest(
        @NotBlank(message = "은행명을 입력해 주세요")
        String bankName,

        @NotBlank(message = "계좌번호를 입력해 주세요")
        String accountNumber,

        @NotBlank(message = "예금주를 입력해 주세요")
        String accountHolder
) {
}