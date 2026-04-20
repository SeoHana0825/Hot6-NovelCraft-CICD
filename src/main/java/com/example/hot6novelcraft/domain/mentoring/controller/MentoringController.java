package com.example.hot6novelcraft.domain.mentoring.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.request.MentoringFeedbackRequest;
import com.example.hot6novelcraft.domain.mentoring.dto.response.ManuscriptUrlResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorStatisticsResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringDetailResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringFeedbackResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringReceivedResponse;
import com.example.hot6novelcraft.domain.mentoring.service.MentoringService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mentorings")
public class MentoringController {

    private final MentoringService mentoringService;

    @GetMapping("/received")
    public ResponseEntity<BaseResponse<PageResponse<MentoringReceivedResponse>>> getReceivedMentorings(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        PageResponse<MentoringReceivedResponse> response =
                PageResponse.register(mentoringService.getReceivedMentorings(
                        userDetails.getUser().getId(), pageable));
        return ResponseEntity.ok(BaseResponse.success("COMMON-200", "접수된 멘토링 목록 조회가 완료되었습니다", response));
    }

    @PatchMapping("/{mentoringId}/mentees/{menteeId}/accept")
    public ResponseEntity<BaseResponse<Void>> acceptMentee(
            @PathVariable Long mentoringId,
            @PathVariable Long menteeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringService.acceptMentee(mentoringId, menteeId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘티 수락이 완료되었습니다", null));
    }

    @PatchMapping("/{mentoringId}/mentees/{menteeId}/reject")
    public ResponseEntity<BaseResponse<Void>> rejectMentee(
            @PathVariable Long mentoringId,
            @PathVariable Long menteeId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringService.rejectMentee(mentoringId, menteeId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘티 거절이 완료되었습니다", null));
    }

    //@GetMapping("/{mentoringId}/documents/{fileId}") // TODO:S3 연동후 해당 도메인으로 이용
    @GetMapping("/{mentoringId}/documents")
    public ResponseEntity<BaseResponse<ManuscriptUrlResponse>> getManuscriptUrl(
            @PathVariable Long mentoringId,
            //@PathVariable String fileId, // TODO: S3 연동 후 presigned URL 생성 시 활용
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        String url = mentoringService.getManuscriptDownloadUrl(
                mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "원고 다운로드 URL 조회가 완료되었습니다",
                ManuscriptUrlResponse.of(mentoringId, url)));
    }
    @PatchMapping("/{mentoringId}/complete")
    public ResponseEntity<BaseResponse<Void>> completeMentoring(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        mentoringService.completeMentoring(mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘토링이 종료되었습니다", null));
    }
    @GetMapping("/{mentoringId}")
    public ResponseEntity<BaseResponse<MentoringDetailResponse>> getMentoringDetail(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        MentoringDetailResponse response = mentoringService.getMentoringDetail(
                mentoringId, userDetails.getUser().getId());
        return ResponseEntity.ok(BaseResponse.success("200", "멘토링 상세 정보 조회가 완료되었습니다", response));
    }
    @PostMapping("/{mentoringId}/feedbacks")
    public ResponseEntity<BaseResponse<MentoringFeedbackResponse>> createFeedback(
            @PathVariable Long mentoringId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody MentoringFeedbackRequest request
    ) {
        MentoringFeedbackResponse response = mentoringService.createFeedback(
                mentoringId, userDetails.getUser().getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "피드백이 등록되었습니다", response));
    }
}