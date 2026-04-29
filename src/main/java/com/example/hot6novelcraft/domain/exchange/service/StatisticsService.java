package com.example.hot6novelcraft.domain.exchange.service;

import com.example.hot6novelcraft.domain.exchange.dto.response.RevenueStatisticsResponse;
import com.example.hot6novelcraft.domain.exchange.dto.response.RevenueStatisticsResponse.RevenueStatisticsItem;
import com.example.hot6novelcraft.domain.exchange.entity.enums.StatisticsPeriod;
import com.example.hot6novelcraft.domain.exchange.repository.RevenueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {

    private static final String STATISTICS_KEY_PREFIX = "revenue:statistics:";
    private static final Duration CACHE_TTL = Duration.ofHours(1);

    private final RevenueRepository revenueRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 수익 분석 통계 조회
     * - 월별/주별 수익 집계
     * - Redis 캐싱 (TTL 1시간)
     */
    public RevenueStatisticsResponse getStatistics(Long authorId, StatisticsPeriod period, Integer year) {
        // Redis 캐시 조회
        String cacheKey = STATISTICS_KEY_PREFIX + authorId + ":" + period + ":" + year;
        RevenueStatisticsResponse cached = (RevenueStatisticsResponse) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            log.debug("통계 캐시 히트 - authorId: {}, period: {}, year: {}", authorId, period, year);
            return cached;
        }

        // QueryDSL 집계 쿼리
        List<RevenueStatisticsItem> items = revenueRepository.findStatistics(authorId, period, year);

        // 전체 합계
        Integer totalAmount = items.stream()
                .mapToInt(RevenueStatisticsItem::totalAmount)
                .sum();

        RevenueStatisticsResponse response = RevenueStatisticsResponse.of(
                period.name(), year, totalAmount, items
        );

        // Redis 캐시 저장
        redisTemplate.opsForValue().set(cacheKey, response, CACHE_TTL);
        log.debug("통계 캐시 저장 - authorId: {}, period: {}, year: {}", authorId, period, year);

        return response;
    }

    /**
     * 통계 캐시 무효화
     * - 수익 발생/환전 시 호출
     */
    public void evictStatisticsCache(Long authorId) {
        // 패턴 기반 삭제 (월별 + 주별 모두)
        String pattern = STATISTICS_KEY_PREFIX + authorId + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.debug("통계 캐시 무효화 - authorId: {}, 삭제 건수: {}", authorId, keys.size());
        }
    }
}