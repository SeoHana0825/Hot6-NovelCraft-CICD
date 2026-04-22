package com.example.hot6novelcraft.domain.mentoring.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.request.MentorshipCreateRequest;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentorshipCreateResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentorshipDetailResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentorshipListResponse;
import com.example.hot6novelcraft.domain.mentoring.service.MentorshipService;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/mentorships")
@RequiredArgsConstructor
public class MentorshipController {

    private final MentorshipService mentorshipService;

    /**
     * 멘토링 신청
     * 정은식
     */
    @PostMapping
    public ResponseEntity<BaseResponse<MentorshipCreateResponse>> applyMentorship(
            @Valid @RequestBody MentorshipCreateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {

        Long menteeId = userDetails.getUser().getId();

        MentorshipCreateResponse response = mentorshipService.applyMentorship(menteeId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success("201", "멘토링 신청이 완료되었습니다.", response));
    }

    /**
     * 멘토 목록 조회
     * - 필터: 장르, 등급
     * 정은식
     */
    @GetMapping("/mentors")
    public ResponseEntity<BaseResponse<Page<MentorshipListResponse>>> getMentorList(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) CareerLevel careerLevel,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<MentorshipListResponse> response = mentorshipService.getMentorList(genre, careerLevel, pageable);
        return ResponseEntity.ok(BaseResponse.success("200", "멘토 목록 조회 성공", response));
    }

    /**
     * 멘토 상세 조회
     * 정은식
     */
    @GetMapping("/mentors/{mentorId}")
    public ResponseEntity<BaseResponse<MentorshipDetailResponse>> getMentorDetail(
            @PathVariable Long mentorId
    ) {
        MentorshipDetailResponse response = mentorshipService.getMentorDetail(mentorId);
        return ResponseEntity.ok(BaseResponse.success("200", "멘토 상세 조회 성공", response));
    }
}