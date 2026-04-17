package com.example.hot6novelcraft.domain.mentor.scheduler;

import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.entity.MentorCareerHistory;
import com.example.hot6novelcraft.domain.mentor.entity.enums.MentorStatus;
import com.example.hot6novelcraft.domain.mentor.repository.MentorCareerHistoryRepository;
import com.example.hot6novelcraft.domain.mentor.repository.MentorRepository;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// TODO: 스케줄러 관련해서 고도화 검토하기 아래내용
//스케줄러는 근본적으로 서버를 분리해야한다.
//스케줄러는 인서트와 업데이트만 담당하고
//조회는 따로분리한다
@Slf4j
@Component
@RequiredArgsConstructor
public class MentorCareerLevelScheduler {

    private static final long INTRODUCTION_MIN_EPISODES = 50L;
    private static final long ELEMENTARY_MIN_EPISODES = 50L;
    private static final long ELEMENTARY_MIN_LIKES = 50L;
    private static final long INTERMEDIATE_MIN_EPISODES = 100L;
    private static final long INTERMEDIATE_MIN_LIKES = 100L;

    private final MentorRepository mentorRepository;
    private final MentorCareerHistoryRepository mentorCareerHistoryRepository;
    private final NovelRepository novelRepository;
    private final EpisodeRepository episodeRepository;

    /**
     * 매일 자정 멘토 등급 자동 조정
     * PROFICIENT(전문)는 관리자 수동 승급이므로 배치 대상 제외
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void adjustCareerLevels() {
        log.info("[MentorCareerLevelScheduler] 멘토 등급 자동 조정 배치 시작");

        List<Mentor> targets = mentorRepository.findAllByStatusAndCareerLevelNot(
                MentorStatus.APPROVED, CareerLevel.PROFICIENT
        );

        int upgradedCount = 0;
        for (Mentor mentor : targets) {
            CareerLevel previousLevel = mentor.getCareerLevel(); // 변경 전 등급 먼저 저장
            CareerLevel newLevel = resolveNewLevel(mentor);

            if (newLevel != null && newLevel != previousLevel) {
                MentorCareerHistory history = MentorCareerHistory.create(
                        mentor.getId(),
                        previousLevel,
                        newLevel,
                        buildChangeReason(newLevel)
                );
                mentor.upgradeCareerLevel(newLevel);
                mentorCareerHistoryRepository.save(history);
                upgradedCount++;
                log.info("[MentorCareerLevelScheduler] mentorId={} {} → {}",
                        mentor.getId(), previousLevel, newLevel); // 저장한 previousLevel 사용
            }
        }

        log.info("[MentorCareerLevelScheduler] 배치 완료 - 총 {}명 등급 조정", upgradedCount);
    }

    private CareerLevel resolveNewLevel(Mentor mentor) {
        List<Long> novelIds = novelRepository.findNovelIdsByAuthorId(mentor.getUserId());
        if (novelIds.isEmpty()) return null;

        long publishedCount = episodeRepository.countByNovelIdInAndStatus(novelIds, EpisodeStatus.PUBLISHED);
        long totalLikes = episodeRepository.sumLikeCountByNovelIdIn(novelIds);

        return switch (mentor.getCareerLevel()) {
            case INTRODUCTION -> {
                if (publishedCount >= INTRODUCTION_MIN_EPISODES) yield CareerLevel.ELEMENTARY;
                yield null;
            }
            case ELEMENTARY -> {
                if (publishedCount >= ELEMENTARY_MIN_EPISODES && totalLikes >= ELEMENTARY_MIN_LIKES) {
                    yield CareerLevel.INTERMEDIATE;
                }
                yield null;
            }
            default -> null;
        };
    }

    private String buildChangeReason(CareerLevel newLevel) {
        return switch (newLevel) {
            case ELEMENTARY -> "PUBLISHED 에피소드 50회 이상 달성으로 자동 승급";
            case INTERMEDIATE -> "PUBLISHED 에피소드 100회 이상 및 좋아요 100개 이상 달성으로 자동 승급";
            default -> "자동 승급";
        };
    }
}