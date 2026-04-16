package com.example.hot6novelcraft.domain.user.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.user.dto.request.*;
import com.example.hot6novelcraft.domain.user.dto.response.*;
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

    @PostMapping("/login")
    public ResponseEntity<BaseResponse<LoginUserResponse>> login(
            @Valid @RequestBody LoginUserRequest request
    ) {
        LoginUserResponse response = authService.login(request);
        return ResponseEntity.ok(BaseResponse.success("200","로그인 성공", response));
    }
    // TODO role 변경 추가!!!!

    @GetMapping("/users/me")
    public ResponseEntity<BaseResponse<MyPageResponse>> getMyPage(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        MyPageResponse response = authService.getMyPage(userDetails);
        return ResponseEntity.ok(BaseResponse.success("200","내 정보 조회 성공", response));
    }

    /* ======== 회원 정보 수정 ========
    1. 공통 수정 - 닉네임, 전화번호
    2. 작가 프로필 - 장르, 소개글 등
    3. 독자 프로필 - 전호 장르, 독서 목표
    4. TODO 비번 변경
    ============================= */

    @PatchMapping("/users/me")
    public ResponseEntity<BaseResponse<CommonUpdateResponse>> updateUserInfo(
            @Valid @RequestBody CommonUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        CommonUpdateResponse response = authService.updateUserInfo(request, userDetails);
        return ResponseEntity.ok(BaseResponse.success("200","회원정보 수정이 완료되었습니다", response));
    }

    @PatchMapping("/users/me/author")
    public ResponseEntity<BaseResponse<AuthorUpdateResponse>> updateAuthor(
            @Valid @RequestBody AuthorRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        AuthorUpdateResponse response = authService.authorUpdateProfile(request, userDetails);
        return ResponseEntity.ok(BaseResponse.success("200","작가 회원정보 수정이 완료되었습니다.", response));
    }

    @PatchMapping("/users/me/reader")
    public ResponseEntity<BaseResponse<ReaderUpdateResponse>> updateReader(
            @Valid @RequestBody ReaderUpdatedRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        ReaderUpdateResponse response = authService.readerUpdateProfile(request, userDetails);
        return ResponseEntity.ok(BaseResponse.success("200","독자 회원정보 수정이 완료되었습니다.", response));
    }

    @PatchMapping("/users/me/password")
    public ResponseEntity<BaseResponse<Void>> updatePassword(
            @Valid @RequestBody PasswordUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        authService.updatePassword(request.oldPassword(), request.newPassword(), userDetails);
        return ResponseEntity.ok(BaseResponse.success("200", "비밀번호가 변경되었습니다",null));
    }

    @PostMapping("/logout")
    public ResponseEntity<BaseResponse<Void>> logout(
            @RequestHeader("Authorization") String accessToken,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ){
        authService.logout(accessToken, userDetails.getUser().getEmail());
        return ResponseEntity.ok(BaseResponse.success("200", "로그아웃 성공",null));
    }
}
