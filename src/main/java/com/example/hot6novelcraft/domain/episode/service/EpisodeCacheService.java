package com.example.hot6novelcraft.domain.episode.service;

import com.example.hot6novelcraft.domain.episode.dto.cache.EpisodeBulkCache;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeCacheService {

    private static final int BULK_SIZE = 20;
    private static final int HOT_THRESHOLD = 50;

    private static final String NOVEL_VIEW_KEY_PREFIX = "novel_view::";
    private static final String HOT_KEY_PREFIX = "novel_hot::";
    private static final String BULK_KEY_PREFIX = "episode_bulk::";
    private static final String REALTIME_RANKING_KEY = "ranking:novel:realtime";
    private static final String WEEKLY_RANKING_KEY = "ranking:novel:weekly";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;


    // 조회수 어뷰징 체크 (true = 첫 조회, false = 중복 조회)
    public boolean isFirstView(Long userId, Long novelId) {
        String viewKey = NOVEL_VIEW_KEY_PREFIX + userId + "::" + novelId;

        Boolean isFirst = redisTemplate.opsForValue()
                .setIfAbsent(viewKey, "1", Duration.ofHours(1));

        return Boolean.TRUE.equals(isFirst);
    }


    // 최근 1시간 조회수 증가 및 반환
    public long increaseHotKeyCount(Long novelId) {
        String hotKey = HOT_KEY_PREFIX + novelId;

        Long count = redisTemplate.opsForValue().increment(hotKey);

        // 처음 생성된 경우 TTL 1시간 설정
        if (count != null && count == 1) {
            redisTemplate.expire(hotKey, Duration.ofHours(1));
        }

        return count != null ? count : 0L;
    }

    /**
     * 실시간 및 주간 랭킹 ZSet 점수 (조회수에 따른) 증가
     * 어뷰징 체크 통과 시에만 호출
     * 서하나 */
    public void increaseRankingScore(Long novelId) {

        String stringNovelId = String.valueOf(novelId);

        try {
            redisTemplate.opsForZSet().incrementScore(REALTIME_RANKING_KEY, stringNovelId, 1);
            redisTemplate.opsForZSet().incrementScore(WEEKLY_RANKING_KEY, stringNovelId, 1);
            log.debug("[랭킹 점수 증가] novelId: {}, 실시간/주간 랭킹 +1", novelId);
        } catch (RuntimeException e) {
            log.warn("[랭킹 점수 증가 실패] novelIdL {}", novelId, e);
        }
    }

    // 인기작 여부 판별
    public boolean isHotNovel(long recentViews) {
        return recentViews >= HOT_THRESHOLD;
    }


    // 벌크 인덱스 계산 (1~20화 -> 1, 21~40화 -> 2)
    public int calculateBulkIndex(int episodeNumber) {
        return (episodeNumber - 1) / BULK_SIZE + 1;
    }

    // 벌크 시작 회차 번호 (bulk 1 -> 1화, bulk 2 -> 21화)
    public int getBulkStartNumber(int bulkIndex) {
        return (bulkIndex - 1) * BULK_SIZE + 1;
    }

    // 벌크 끝 회차 번호 (bulk 1 -> 20화, bulk 2 -> 40화)
    public int getBulkEndNumber(int bulkIndex) {
        return bulkIndex * BULK_SIZE;
    }

    // 벌크 캐시 조회 (없으면 null)
    public List<EpisodeBulkCache> getBulkCache(Long novelId, int bulkIndex) {
        String cacheKey = getBulkCacheKey(novelId, bulkIndex);

        // Redis에서 JSON 문자열로 꺼냄
        Object cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached == null) {
            log.debug("[Cache MISS] {}", cacheKey);
            return null;
        }

        // JSON 문자열 → List<EpisodeBulkCache> 변환
        try {
            List<EpisodeBulkCache> result = objectMapper.readValue(
                    cached.toString(),
                    new TypeReference<List<EpisodeBulkCache>>() {}
            );
            log.debug("[Cache HIT] {}", cacheKey);
            return result;
        } catch (JsonProcessingException e) {
            log.error("[Cache 역직렬화 실패] {}", cacheKey, e);
            return null;
        }
    }

    // 벌크 캐시 저장 (TTL 30분)
    public void saveBulkCache(Long novelId, int bulkIndex, List<Episode> episodes) {
        String cacheKey = getBulkCacheKey(novelId, bulkIndex);

        List<EpisodeBulkCache> bulkCache = episodes.stream()
                .map(EpisodeBulkCache::from)
                .toList();

        try {
            // 객체 → JSON 문자열 변환해서 저장
            String json = objectMapper.writeValueAsString(bulkCache);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(30));
            log.debug("[Cache SAVE] {} (TTL 30분, 회차 {}개)", cacheKey, bulkCache.size());
        } catch (JsonProcessingException e) {
            log.error("[Cache 직렬화 실패] {}", cacheKey, e);
        }
    }


    // 특정 소설의 모든 벌크 캐시 무효화 (회차 수정/삭제 시)
    public void evictBulkCache(Long novelId) {
        String pattern = BULK_KEY_PREFIX + novelId + "::*";

        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(100)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }

        log.debug("[Cache EVICT] {}", pattern);
    }

    // 벌크캐시관련
    private String getBulkCacheKey(Long novelId, int bulkIndex) {
        return BULK_KEY_PREFIX + novelId + "::" + bulkIndex;
    }
}