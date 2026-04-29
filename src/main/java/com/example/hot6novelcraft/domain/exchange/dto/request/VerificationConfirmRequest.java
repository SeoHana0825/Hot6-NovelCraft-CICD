package com.example.hot6novelcraft.domain.exchange.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record VerificationConfirmRequest(
        @NotNull(message = "계좌 ID를 입력해 주세요")
        Long bankAccountId,

        @NotBlank(message = "인증 코드를 입력해 주세요")
        String verificationCode
) {
}