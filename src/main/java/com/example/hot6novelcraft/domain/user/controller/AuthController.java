package com.example.hot6novelcraft.domain.user.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.user.dto.request.LoginUserRequest;
import com.example.hot6novelcraft.domain.user.dto.response.LoginUserResponse;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /* ======== 로그인 및 로그아웃 ========
    1. 로그인
    2. TODO!! 소셜 로그인
    3. 토큰 재발급
    4. 로그아웃
    =================================== */

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginUserResponse>> login(
            @Valid @RequestBody LoginUserRequest request
    ) {
        LoginUserResponse response = authService.login(request);
        return ResponseEntity.ok(BaseResponse.success("200","로그인 성공", response));
    }

    // TODO : 소셜 로그인 로직 추가 예정!!!!

    // TODO role 변경 추가!!!!


    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestHeader("Authorization") String accessToken,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        authService.logout(accessToken, userDetails.getUser().getEmail());
        return ResponseEntity.ok(BaseResponse.success("200", "로그아웃 성공",null));
    }
}
