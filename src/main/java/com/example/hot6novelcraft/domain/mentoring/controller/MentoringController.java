package com.example.hot6novelcraft.domain.mentoring.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.request.MentoringFeedbackRequest;
import com.example.hot6novelcraft.domain.mentoring.dto.response.ManuscriptUrlResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringDetailResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringFeedbackResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringReceivedResponse;
import com.example.hot6novelcraft.domain.mentoring.service.MentoringServiceV1;
import com.example.hot6novelcraft.domain.mentoring.service.MentoringServiceV2;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class MentoringController {

    private final MentoringServiceV1 mentoringServiceV1;       // V1
    private final MentoringServiceV2 mentoringServiceV2;   // V2

    // ===================== 공통 엔드포인트 (V1 / V2 동일) =====================

    @GetMapping("/api/v1/mentorings/received")
    public ResponseEntity<BaseResponse<PageResponse<MentoringReceivedResponse>>> getReceivedMentoringsV1(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                BaseResponse.success("COMMON-200", "접수된 멘토링 목록 조회가 완료되었습니다",
                        PageResponse.register(mentoringServiceV1.getReceivedMentorings(
                                userDetails.getUser().getId(), PageRequest.of(page, size))))
        );
    }

    @GetMapping("/api/v2/mentorings/received")
    public ResponseEntity<BaseResponse<PageResponse<MentoringReceivedResponse>>> getReceivedMentoringsV2(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(
                BaseResponse.success("COMMON-200", "접수된 멘토링 목록 조회가 완료되었습니다",
                        PageResponse.register(mentoringServiceV2.getReceivedMentorings(
                                userDetails.getUser().getId(), PageRequest.of(page, size))))
        );
    }

    // ===================== 멘티 수락 =====================

    @PatchMapping("/api/v1/mentorings/{mentoringId}/mentees/{menteeId}/accept")
    public ResponseEntity<BaseResponse<Void>> acceptMenteeV1(
            @PathVariable Long mentoringId,
            @PathVariable Long menteeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringServiceV1.acceptMentee(mentoringId, menteeId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘티 수락이 완료되었습니다", null));
    }

    @PatchMapping("/api/v2/mentorings/{mentoringId}/mentees/{menteeId}/accept")
    public ResponseEntity<BaseResponse<Void>> acceptMenteeV2(
            @PathVariable Long mentoringId,
            @PathVariable Long menteeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringServiceV2.acceptMentee(mentoringId, menteeId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘티 수락이 완료되었습니다", null));
    }

    // ===================== 멘티 거절 =====================

    @PatchMapping("/api/v1/mentorings/{mentoringId}/mentees/{menteeId}/reject")
    public ResponseEntity<BaseResponse<Void>> rejectMenteeV1(
            @PathVariable Long mentoringId,
            @PathVariable Long menteeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringServiceV1.rejectMentee(mentoringId, menteeId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘티 거절이 완료되었습니다", null));
    }

    @PatchMapping("/api/v2/mentorings/{mentoringId}/mentees/{menteeId}/reject")
    public ResponseEntity<BaseResponse<Void>> rejectMenteeV2(
            @PathVariable Long mentoringId,
            @PathVariable Long menteeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringServiceV2.rejectMentee(mentoringId, menteeId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘티 거절이 완료되었습니다", null));
    }

    // ===================== 원고 다운로드 =====================

    @GetMapping("/api/v1/mentorings/{mentoringId}/documents")
    public ResponseEntity<BaseResponse<ManuscriptUrlResponse>> getManuscriptUrlV1(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        String url = mentoringServiceV1.getManuscriptDownloadUrl(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(
                BaseResponse.success("200", "원고 다운로드 URL 조회가 완료되었습니다",
                        new ManuscriptUrlResponse(mentoringId, url))
        );
    }

    @GetMapping("/api/v2/mentorings/{mentoringId}/documents")
    public ResponseEntity<BaseResponse<ManuscriptUrlResponse>> getManuscriptUrlV2(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        String url = mentoringServiceV2.getManuscriptDownloadUrl(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(
                BaseResponse.success("200", "원고 다운로드 URL 조회가 완료되었습니다",
                        new ManuscriptUrlResponse(mentoringId, url))
        );
    }

    // ===================== 멘토링 종료 =====================

    @PatchMapping("/api/v1/mentorings/{mentoringId}/complete")
    public ResponseEntity<BaseResponse<Void>> completeMentoringV1(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringServiceV1.completeMentoring(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘토링이 종료되었습니다", null));
    }

    @PatchMapping("/api/v2/mentorings/{mentoringId}/complete")
    public ResponseEntity<BaseResponse<Void>> completeMentoringV2(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringServiceV2.completeMentoring(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘토링이 종료되었습니다", null));
    }

    // ===================== 멘토링 상세 조회 =====================

    @GetMapping("/api/v1/mentorings/{mentoringId}")
    public ResponseEntity<BaseResponse<MentoringDetailResponse>> getMentoringDetailV1(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        MentoringDetailResponse response =
                mentoringServiceV1.getMentoringDetail(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(
                BaseResponse.success("200", "멘토링 상세 정보 조회가 완료되었습니다", response)
        );
    }

    // V2: soft-delete 적용된 소설 제목 조회
    @GetMapping("/api/v2/mentorings/{mentoringId}")
    public ResponseEntity<BaseResponse<MentoringDetailResponse>> getMentoringDetailV2(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        MentoringDetailResponse response =
                mentoringServiceV2.getMentoringDetail(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(
                BaseResponse.success("200", "멘토링 상세 정보 조회가 완료되었습니다", response)
        );
    }

    // ===================== 피드백 작성 =====================

    // V1: 동시성 보호 없음
    @PostMapping("/api/v1/mentorings/{mentoringId}/feedbacks")
    public ResponseEntity<BaseResponse<MentoringFeedbackResponse>> createFeedbackV1(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MentoringFeedbackRequest request
    ) {
        MentoringFeedbackResponse response =
                mentoringServiceV1.createFeedback(mentoringId, userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "피드백이 등록되었습니다", response));
    }

    // V2: 비관적 락 + 유니크 제약으로 동시성 보호, @Size 검증 추가
    @PostMapping("/api/v2/mentorings/{mentoringId}/feedbacks")
    public ResponseEntity<BaseResponse<MentoringFeedbackResponse>> createFeedbackV2(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MentoringFeedbackRequest request
    ) {
        MentoringFeedbackResponse response =
                mentoringServiceV2.createFeedback(mentoringId, userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "피드백이 등록되었습니다", response));
    }
}