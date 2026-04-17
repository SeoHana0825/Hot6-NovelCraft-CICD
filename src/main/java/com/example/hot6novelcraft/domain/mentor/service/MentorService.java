package com.example.hot6novelcraft.domain.mentor.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.MentorExceptionEnum;
import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorRegisterRequest;
import com.example.hot6novelcraft.domain.mentor.dto.request.MentorUpdateRequest;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorProfileResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorRegisterResponse;
import com.example.hot6novelcraft.domain.mentor.dto.response.MentorUpdateResponse;
import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.entity.enums.MentorStatus;
import com.example.hot6novelcraft.domain.mentor.repository.MentorRepository;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MentorService {

    private static final long INTRODUCTION_MIN_EPISODES = 50L;
    private static final long ELEMENTARY_MIN_EPISODES = 50L;
    private static final long ELEMENTARY_MIN_LIKES = 50L;
    private static final long INTERMEDIATE_MIN_EPISODES = 100L;
    private static final long INTERMEDIATE_MIN_LIKES = 100L;

    private final MentorRepository mentorRepository;
    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;
    private final ObjectMapper objectMapper;

    /**
     * 멘토 등록 신청
     * - PENDING 또는 APPROVED 상태의 기존 신청이 있으면 중복 신청 불가
     * - careerLevel 기준으로 입문/중급 자동 승인, 전문은 PENDING 유지
     */
    @Transactional
    public MentorRegisterResponse register(Long userId, MentorRegisterRequest request,
                                           MultipartFile certificationFile) {
        if (mentorRepository.existsByUserIdAndStatus(userId, MentorStatus.PENDING)) {
            throw new ServiceErrorException(MentorExceptionEnum.MENTOR_PENDING_EXISTS);
        }
        if (mentorRepository.existsByUserIdAndStatus(userId, MentorStatus.APPROVED)) {
            throw new ServiceErrorException(MentorExceptionEnum.MENTOR_ALREADY_APPROVED);
        }

        String fileUrl = uploadCertificationFile(certificationFile);
        MentorStatus initialStatus = resolveInitialStatus(userId, request.careerLevel());

        Mentor mentor = Mentor.create(
                userId,
                request.careerLevel(),
                toJson(request.mainGenres()),
                toJson(request.specialFields()),
                toJson(request.mentoringStyles()),
                request.bio(),
                request.awardsCareer(),
                request.maxMentees(),
                request.allowInstant(),
                request.preferredMenteeDesc(),
                fileUrl,
                initialStatus
        );

        try {
            Mentor saved = mentorRepository.save(mentor);
            return MentorRegisterResponse.from(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ServiceErrorException(MentorExceptionEnum.MENTOR_ALREADY_APPROVED);
        }
    }

    /**
     * 멘토 정보 수정
     * - null 필드는 기존 값 유지 (부분 업데이트)
     */
    @Transactional
    public MentorUpdateResponse update(Long userId, MentorUpdateRequest request) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        if (request.careerHistory() != null && request.careerHistory().isBlank()) {
            throw new ServiceErrorException(MentorExceptionEnum.MENTOR_CAREER_REQUIRED);
        }

        mentor.update(
                request.introduction(),
                toJson(request.mainGenres()),
                toJson(request.specialFields()),
                toJson(request.mentoringStyles()),
                request.careerHistory(),
                request.maxMentees(),
                request.allowInstant(),
                request.preferredMenteeDesc()
        );

        return MentorUpdateResponse.from(mentor.getId(), LocalDateTime.now());
    }

    /**
     * 내 멘토 프로필 조회
     */
    @Transactional(readOnly = true)
    public MentorProfileResponse getMyProfile(Long userId) {
        Mentor mentor = mentorRepository.findByUserId(userId)
                .orElseThrow(() -> new ServiceErrorException(MentorExceptionEnum.MENTOR_NOT_FOUND));

        return MentorProfileResponse.from(mentor);
    }

    /**
     * careerLevel 기준 초기 상태 결정
     */
    private MentorStatus resolveInitialStatus(Long userId, CareerLevel careerLevel) {
        if (careerLevel == CareerLevel.PROFICIENT) {
            return MentorStatus.PENDING;
        }

        List<Long> novelIds = novelRepository.findNovelIdsByAuthorId(userId);
        if (novelIds.isEmpty()) {
            return MentorStatus.PENDING;
        }

        long publishedCount = episodeRepository.countByNovelIdInAndStatus(novelIds, EpisodeStatus.PUBLISHED);
        long totalLikes = episodeRepository.sumLikeCountByNovelIdIn(novelIds);

        return switch (careerLevel) {
            case INTRODUCTION -> publishedCount >= INTRODUCTION_MIN_EPISODES
                    ? MentorStatus.APPROVED : MentorStatus.PENDING;
            case ELEMENTARY -> (publishedCount >= ELEMENTARY_MIN_EPISODES && totalLikes >= ELEMENTARY_MIN_LIKES)
                    ? MentorStatus.APPROVED : MentorStatus.PENDING;
            case INTERMEDIATE -> (publishedCount >= INTERMEDIATE_MIN_EPISODES && totalLikes >= INTERMEDIATE_MIN_LIKES)
                    ? MentorStatus.APPROVED : MentorStatus.PENDING;
            default -> MentorStatus.PENDING;
        };
    }

    private String uploadCertificationFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        // TODO: S3 연동 후 UUID 기반 파일명으로 변경 필요 (path traversal 방어)
        return file.getOriginalFilename();
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            // JSON 직렬화 실패 - 파일 업로드와 무관한 내부 오류
            throw new ServiceErrorException(MentorExceptionEnum.MENTOR_JSON_SERIALIZE_FAILED);
        }
    }
}