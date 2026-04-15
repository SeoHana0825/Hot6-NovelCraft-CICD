package com.example.hot6novelcraft.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PhoneSendRequest (

        @NotBlank(message = "휴대폰번호 입력은 필수입니다.")
        String phoneNo
) {

}
