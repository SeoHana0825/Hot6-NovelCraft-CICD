package com.example.hot6novelcraft.domain.mentoring.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.MentoringExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.MentorExceptionEnum;
import com.example.hot6novelcraft.domain.file.service.FileUploadService;
import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.entity.MentorFeedback;
import com.example.hot6novelcraft.domain.mentor.repository.MentorFeedbackRepository;
import com.example.hot6novelcraft.domain.mentor.repository.MentorRepository;
import com.example.hot6novelcraft.domain.mentoring.dto.request.MentoringFeedbackRequest;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringDetailResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringFeedbackResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringReceivedResponse;
import com.example.hot6novelcraft.domain.mentoring.entity.Mentorship;
import com.example.hot6novelcraft.domain.mentoring.entity.enums.MentorshipStatus;
import com.example.hot6novelcraft.domain.mentoring.repository.MentorshipRepository;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentoringService {

    private final MentorshipRepository mentorshipRepository;
    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final MentorFeedbackRepository mentorFeedbackRepository;

    private final FileUploadService fileUploadService;


    public Page<MentoringReceivedResponse> getReceivedMentorings(Long userId, Pageable pageable) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        // TODO: 고도화 시 QueryDSL로 멘티명/소설명 JOIN 조회로 교체 (현재 N+1 발생 가능)
        return mentorshipRepository.findAllByMentorIdOrderByCreatedAtDesc(mentor.getId(), pageable)
                .map(mentorship -> {
                    String menteeName = userRepository.findByIdAndIsDeletedFalse(mentorship.getMenteeId())
                            .map(User::getNickname)
                            .orElse("알 수 없는 사용자");

                    // TODO: 고도화 시 NovelRepository에 findByIdAndIsDeletedFalse 추가 후 교체
                    String title = novelRepository.findById(mentorship.getCurrentNovelId())
                            .map(Novel::getTitle)
                            .orElse("알 수 없는 소설");

                    return MentoringReceivedResponse.of(mentorship, menteeName, title);
                });
    }

    @Transactional
    public void acceptMentee(Long mentoringId, Long menteeId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepository.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (!mentorship.getMenteeId().equals(menteeId)) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_MENTEE_NOT_MATCH);
        }

        if (mentorship.getStatus() != MentorshipStatus.PENDING) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_ALREADY_PROCESSED);
        }

        mentor.decreaseSlot();
        mentorship.approve();

        // TODO: 1:1 채팅방 자동 생성 (채팅 팀원 개발 후 연동)
        // TODO: 멘티에게 수락 알림 발송 (알림 팀원 개발 후 연동)
    }

    @Transactional
    public void rejectMentee(Long mentoringId, Long menteeId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepository.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (!mentorship.getMenteeId().equals(menteeId)) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_MENTEE_NOT_MATCH);
        }

        if (mentorship.getStatus() != MentorshipStatus.PENDING) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_ALREADY_PROCESSED);
        }

        mentorship.reject();

        // TODO: 멘티에게 거절 알림 발송 (알림 팀원 개발 후 연동)
    }

    @Transactional
    public String getManuscriptDownloadUrl(Long mentoringId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepository.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getUserId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (mentorship.getManuscriptUrl() == null) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_MANUSCRIPT_NOT_FOUND);
        }

        mentorship.increaseManuscriptDownloadCount();

        // S3 Presigned URL 발급 (1시간 유효)
        return fileUploadService.generateManuscriptPresignedUrl(mentorship.getManuscriptUrl());
    }

    @Transactional
    public void completeMentoring(Long mentoringId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepository.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (mentorship.getStatus() != MentorshipStatus.ACCEPTED) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_ACCEPTED);
        }

        mentor.increaseSlot();
        mentorship.complete();

        // TODO: 멘티에게 종료 알림 발송 (알림 팀원 개발 후 연동)
    }

    public MentoringDetailResponse getMentoringDetail(Long mentoringId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepository.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        String mentorName = userRepository.findByIdAndIsDeletedFalse(userId)
                .map(User::getNickname)
                .orElse("알 수 없는 사용자");

        String menteeName = userRepository.findByIdAndIsDeletedFalse(mentorship.getMenteeId())
                .map(User::getNickname)
                .orElse("알 수 없는 사용자");

        List<MentoringDetailResponse.FeedbackInfo> feedbacks = mentorFeedbackRepository
                .findAllByMentorshipIdOrderByCreatedAtAsc(mentoringId)
                .stream()
                .map(MentoringDetailResponse.FeedbackInfo::from)
                .toList();

        return MentoringDetailResponse.of(mentorship, mentorName, menteeName, feedbacks);
    }

    @Transactional
    public MentoringFeedbackResponse createFeedback(Long mentoringId, Long userId,
                                                    MentoringFeedbackRequest request) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepository.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (mentorship.getStatus() != MentorshipStatus.ACCEPTED) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_FEEDBACK_ONLY_ACCEPTED);
        }

        MentorFeedback feedback = MentorFeedback.create(mentoringId, mentor.getId(), request.content());
        mentorFeedbackRepository.save(feedback);
        mentorship.increaseSession();

        return MentoringFeedbackResponse.from(feedback);
    }
}