package com.example.hot6novelcraft.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// 탈퇴 후 계정 복구 DTO
public record UserRestoreRequest(

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일 입력은 필수 입니다.")
        String email,

        String recoveryToken
) {
}
