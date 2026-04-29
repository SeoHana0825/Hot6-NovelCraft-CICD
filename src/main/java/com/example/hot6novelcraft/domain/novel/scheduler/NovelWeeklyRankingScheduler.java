package com.example.hot6novelcraft.domain.novel.scheduler;

import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class NovelWeeklyRankingScheduler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final NovelRepository novelRepository;

    private static final String REALTIME_RANKING_KEY = "ranking:novel:realtime";
    private static final String WEEKLY_RANKING_KEY = "ranking:novel:weekly";

    private static final String TEMP_REALTIME_RANKING_KEY = "ranking:novel:realtime:temp";
    private static final String TEMP_WEEKLY_RANKING_KEY = "ranking:novel:weekly:temp";

    /**
     * ======= [실시간 랭킹] ============
     * - 매 정각마다 초기화
     * - 11:00:00 에 10시~11시까지의 데이터 초기화
     * 서하나
     * ================================
     */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 * * * *")
    public void updateRealtimeRanking() {
        try {
            log.info("[Redis ZSet 실시간] 실시간 인기 소설 랭킹 무중단 업데이트 시작");

            // DB에서 최근 1시간 TOP 5 소설 목록 조회
            List<Novel> currentHourTopNovels = novelRepository.findHourlyTopNovels(5);

            // 데이터 없으면 진행하지 않음
            if (currentHourTopNovels.isEmpty()) {
                return;
            }

            // 기존 임시 키 삭제
            redisTemplate.delete(TEMP_REALTIME_RANKING_KEY);

            // temp 키에 1시간 데이터 적제
            for (Novel novel : currentHourTopNovels) {
                redisTemplate.opsForZSet().add(TEMP_REALTIME_RANKING_KEY, novel.getId().toString(), novel.getViewCount());
            }

            // 스케쥴러가 죽었을 때 대비 temp 키 생명주기 2시간 설정
            redisTemplate.expire(TEMP_REALTIME_RANKING_KEY, Duration.ofHours(2));

            // rename으로 진짜 키와 바꿔치기
            redisTemplate.rename(TEMP_REALTIME_RANKING_KEY, REALTIME_RANKING_KEY);

            log.info("[Redis ZSet 실시간] 실시간 인기 소설 랭킹 무중단 갱신 완료");

        } catch (Exception e) {
            log.error("[Redis] 랭킹 갱신 중 에러 발생, Temp Key를 청소합니다", e);

            // 에러 발생 시 애매한 temp key 삭제
            redisTemplate.delete(TEMP_REALTIME_RANKING_KEY);
            throw e;
        }
    }

    /** ======= [주간 랭킹] ============
     * - Redis 주간 키 초기화 및 새 주차 시작
     * - 일요일 00시 01분 업데이트 초기화
     * 서하나
     * =============================== */
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 1 0 * * SUN")
    public void updateWeeklyRanking() {
        try {
            log.info("[Redis ZSet 실시간] 주간 인기 소설 랭킹 무중단 업데이트 시작");

            List<Novel> weeklyTopNovels = novelRepository.findWeeklyTopNovels(5);

            if (weeklyTopNovels.isEmpty()) {
                return;
            }
            redisTemplate.delete(TEMP_WEEKLY_RANKING_KEY);

            for (Novel novel : weeklyTopNovels) {
                redisTemplate.opsForZSet().add(TEMP_WEEKLY_RANKING_KEY, novel.getId().toString(), novel.getViewCount());
            }

            // 스케쥴러가 죽었을 때 대비 temp 키 생명주기 8일 설정
            redisTemplate.expire(TEMP_WEEKLY_RANKING_KEY, Duration.ofDays(8));

            redisTemplate.rename(TEMP_WEEKLY_RANKING_KEY, WEEKLY_RANKING_KEY);

            log.info("[Redis ZSet 주간] 주간 인기 소설 랭킹 무중단 업데이트 완료, 새 주차 시작");

        } catch (Exception e) {
            log.error("[Redis] 랭킹 갱신 중 에러 발생, Temp Key를 청소합니다", e);

            // 에러 발생 시 애매한 temp key 삭제
            redisTemplate.delete(TEMP_WEEKLY_RANKING_KEY);
            throw e;
        }
    }
}
