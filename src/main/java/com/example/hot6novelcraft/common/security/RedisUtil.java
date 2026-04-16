package com.example.hot6novelcraft.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    // 블랙리스트 등록 (key -Token, value -상태, duration -유효시간)
    public void setBlackList(String accessToken, Object object, Duration duration) {
        redisTemplate.opsForValue().set(accessToken, object, duration);
    }

    public boolean isBlackList(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessToken));
    }

    /**
     * Redis 분산 락 획득 (SET NX EX — 원자적 SETNX + TTL).
     * 키가 없을 때만 세팅되므로 동시 요청 중 하나만 true를 반환받는다.
     * TTL을 함께 지정해 서버 비정상 종료 시에도 Lock이 영구 잠금되지 않도록 보장한다.
     *
     * @param key            락 키
     * @param timeoutSeconds 락 자동 해제까지 최대 대기 시간
     * @return true면 락 획득 성공, false면 이미 다른 프로세스가 보유 중
     */
    public boolean acquireLock(String key, long timeoutSeconds) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofSeconds(timeoutSeconds));
        boolean acquired = Boolean.TRUE.equals(result);
        if (acquired) {
            log.info("[RedisLock] 락 획득 key={} ttl={}s", key, timeoutSeconds);
        } else {
            log.warn("[RedisLock] 락 획득 실패 (이미 점유 중) key={}", key);
        }
        return acquired;
    }

    /**
     * Redis 분산 락 해제.
     * finally 블록에서 반드시 호출해야 한다.
     */
    public void releaseLock(String key) {
        redisTemplate.delete(key);
        log.info("[RedisLock] 락 해제 key={}", key);
    }
}