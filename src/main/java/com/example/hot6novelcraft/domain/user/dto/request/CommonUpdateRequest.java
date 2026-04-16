package com.example.hot6novelcraft.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommonUpdateRequest(

        @NotBlank(message = "닉네임 및 필명 입력은 필수입니다.")
        @Size(min = 1, max = 10, message = "10자이내의 닉네임 및 필명을 입력해주세요.")
        String nickname,

        @NotBlank(message = "휴대폰번호 입력은 필수입니다.")
        String phoneNo

//        @NotBlank(message = "휴대폰번호 인증이 필요합니다.")
//        String phoneToken
        ) {
}
