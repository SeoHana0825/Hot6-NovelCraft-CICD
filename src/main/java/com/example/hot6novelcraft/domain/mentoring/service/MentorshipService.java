package com.example.hot6novelcraft.domain.mentoring.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.MentorExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.MentoringExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.domain.file.service.FileUploadService;
import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.repository.MentorRepository;
import com.example.hot6novelcraft.domain.mentoring.dto.request.MentorshipCreateRequest;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentorshipCreateResponse;
import com.example.hot6novelcraft.domain.mentoring.entity.Mentorship;
import com.example.hot6novelcraft.domain.mentoring.entity.enums.MentorshipStatus;
import com.example.hot6novelcraft.domain.mentoring.repository.MentorshipRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorshipService {

    private final MentorshipRepository mentorshipRepository;
    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;

    private final FileUploadService fileUploadService;

    @Transactional
    public MentorshipCreateResponse applyMentorship(Long menteeId, MentorshipCreateRequest request) {

        // 작가 권한 확인
        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        if (mentee.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_AUTHOR);
        }

        // 멘토 조회 (못 찾으면 NOT_FOUND)
        Mentor mentor = mentorRepository.findByUserId(request.mentorId())
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        // 본인한테 신청 불가
        if (mentor.getUserId().equals(menteeId)) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_SELF_APPLY);
        }

        // 이미 진행중인 멘토링 있으면 신청 불가 (1:1 제약)
        boolean alreadyExists = mentorshipRepository.existsByMenteeIdAndStatusIn(
                menteeId,
                List.of(MentorshipStatus.PENDING, MentorshipStatus.ACCEPTED)
        );
        if (alreadyExists) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_ALREADY_EXISTS);
        }

        if (mentor.getMaxMentees() <= 0) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_SLOT_FULL);
        }

        // 멘토링 신청 생성
        Mentorship mentorship = Mentorship.create(
                mentor.getId(),
                menteeId,
                request.currentNovelId(),
                request.motivation(),
                request.manuscriptUrl()
        );

        Mentorship saved = mentorshipRepository.save(mentorship);
        log.info("[Mentorship] 멘토링 신청 완료 menteeId={} mentorId={}", menteeId, request.mentorId());

        return MentorshipCreateResponse.from(saved.getId());
    }

    /**
     * 멘토링 원고 파일 업로드
     * - 작가 권한만 업로드 가능
     * 정은식
     */
    public String uploadManuscript(MultipartFile file, Long menteeId) {

        // 작가 권한 확인
        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (mentee.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_AUTHOR);
        }

        return fileUploadService.uploadManuscript(file);
    }
}