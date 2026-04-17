package com.example.hot6novelcraft.domain.mentor.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorRegisterRequest;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorUpdateRequest;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorProfileResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorRegisterResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorUpdateResponse;
import com.example.hot6novelcraft.domain.mentor.service.MentorService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;


    //멘토 등록 신청 multipart/form-data 로 전문가 인증 서류 파일 + 신청 정보 함께 수신
    @PostMapping
    public ResponseEntity<BaseResponse<MentorRegisterResponse>> register(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MentorRegisterRequest request
            // TODO: [S3 연동 후 multipart/form-data 로 변경 필요]
            // @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
            // @RequestPart("data") MentorRegisterRequest request
            // @RequestPart(value = "certificationFile", required = false) MultipartFile certificationFile
    ) {
        MentorRegisterResponse response = mentorService.register(userDetails.getUser().getId(), request, null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "멘토 신청이 정상적으로 접수되었습니다", response));
    }

    //멘토 정보 수정
    @PutMapping("/me")
    public ResponseEntity<BaseResponse<MentorUpdateResponse>> update(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MentorUpdateRequest request
    ) {
        MentorUpdateResponse response = mentorService.update(userDetails.getUser().getId(), request);
        return ResponseEntity.ok(BaseResponse.success("200", "멘토 정보가 수정되었습니다", response));
    }

    //내 멘토 프로필 조회
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<MentorProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        MentorProfileResponse response = mentorService.getMyProfile(userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "내 멘토 프로필 조회 성공", response));
    }
}
