package com.example.hot6novelcraft.domain.mentoring.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.MentoringExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.MentorExceptionEnum;
import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.entity.MentorFeedbackV2;
import com.example.hot6novelcraft.domain.mentor.repository.MentorFeedbackRepository;
import com.example.hot6novelcraft.domain.mentor.repository.MentorRepository;
import com.example.hot6novelcraft.domain.mentoring.dto.request.MentoringFeedbackRequest;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringDetailResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringFeedbackResponse;
import com.example.hot6novelcraft.domain.mentoring.dto.response.MentoringReceivedResponse;
import com.example.hot6novelcraft.domain.mentoring.entity.Mentorship;
import com.example.hot6novelcraft.domain.mentoring.entity.enums.MentorshipStatus;
import com.example.hot6novelcraft.domain.mentoring.repository.MentorshipRepositoryV2;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MentoringServiceV2 {

    private final MentorshipRepositoryV2 mentorshipRepositoryV2;
    private final MentorRepository mentorRepository;
    private final UserRepository userRepository;
    private final NovelRepository novelRepository;
    private final MentorFeedbackRepository mentorFeedbackRepository;

    public Page<MentoringReceivedResponse> getReceivedMentorings(Long userId, Pageable pageable) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        return mentorshipRepositoryV2.findAllByMentorIdOrderByCreatedAtDesc(mentor.getId(), pageable)
                .map(mentorship -> {
                    String menteeName = userRepository.findByIdAndIsDeletedFalse(mentorship.getMenteeId())
                            .map(User::getNickname)
                            .orElse("알 수 없는 사용자");

                    // V2: soft-delete 적용
                    String title = novelRepository.findByIdAndIsDeletedFalse(mentorship.getCurrentNovelId())
                            .map(Novel::getTitle)
                            .orElse("알 수 없는 소설");

                    return MentoringReceivedResponse.of(mentorship, menteeName, title);
                });
    }

    @Transactional
    public void acceptMentee(Long mentoringId, Long menteeId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepositoryV2.findById(mentoringId)
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
    }

    @Transactional
    public void rejectMentee(Long mentoringId, Long menteeId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepositoryV2.findById(mentoringId)
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
    }

    @Transactional
    public String getManuscriptDownloadUrl(Long mentoringId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepositoryV2.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (mentorship.getManuscriptUrl() == null) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_MANUSCRIPT_NOT_FOUND);
        }

        mentorship.increaseManuscriptDownloadCount();

        return mentorship.getManuscriptUrl();
    }

    @Transactional
    public void completeMentoring(Long mentoringId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepositoryV2.findById(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (mentorship.getStatus() != MentorshipStatus.ACCEPTED) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_ACCEPTED);
        }

        mentor.increaseSlot();
        mentorship.complete();
    }

    public MentoringDetailResponse getMentoringDetail(Long mentoringId, Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        Mentorship mentorship = mentorshipRepositoryV2.findById(mentoringId)
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

        // V2: soft-delete 적용 — 삭제된 소설 제목 노출 방지
        String novelTitle = novelRepository.findByIdAndIsDeletedFalse(mentorship.getCurrentNovelId())
                .map(Novel::getTitle)
                .orElse("알 수 없는 소설");

        List<MentoringDetailResponse.FeedbackInfo> feedbacks = mentorFeedbackRepository
                .findAllByMentorshipIdOrderByCreatedAtAsc(mentoringId)
                .stream()
                .map(MentoringDetailResponse.FeedbackInfo::from)
                .toList();

        return MentoringDetailResponse.of(mentorship, mentorName, menteeName, novelTitle, feedbacks);
    }

    // V2: 비관적 락 + 유니크 제약으로 sessionNumber 동시성 보호
    @Transactional
    public MentoringFeedbackResponse createFeedback(Long mentoringId, Long userId,
                                                    MentoringFeedbackRequest request) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        // V2: findByIdWithLock — 비관적 락으로 동시 요청 직렬화
        Mentorship mentorship = mentorshipRepositoryV2.findByIdWithLock(mentoringId)
                .orElseThrow(() -> new ServiceErrorException(MentoringExceptionEnum.MENTORING_NOT_FOUND));

        if (!mentorship.getMentorId().equals(mentor.getId())) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_UNAUTHORIZED);
        }

        if (mentorship.getStatus() != MentorshipStatus.ACCEPTED) {
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_FEEDBACK_ONLY_ACCEPTED);
        }

        int nextSession = mentorship.getTotalSessions() + 1;

        MentorFeedbackV2 feedback = MentorFeedbackV2.create(
                mentoringId,
                mentor.getId(),
                request.title(),
                nextSession,
                request.content()
        );

        try {
            mentorFeedbackRepository.save(feedback);
        } catch (DataIntegrityViolationException e) {
            // V2: 유니크 제약 충돌 — 동시 요청으로 같은 회차 저장 시도 방어
            throw new ServiceErrorException(MentoringExceptionEnum.MENTORING_SESSION_CONFLICT);
        }

        mentorship.increaseSession();

        return MentoringFeedbackResponse.from(feedback);
    }
}