package com.example.hot6novelcraft.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> RELEASE_LOCK_SCRIPT;
    static {
        RELEASE_LOCK_SCRIPT = new DefaultRedisScript<>();
        RELEASE_LOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end"
        );
        RELEASE_LOCK_SCRIPT.setResultType(Long.class);
    }

    // 블랙리스트 등록 (key -Token, value -상태, duration -유효시간)
    public void setBlackList(String accessToken, Object object, Duration duration) {
        redisTemplate.opsForValue().set(accessToken, object, duration);
    }

    public boolean isBlackList(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessToken));
    }

    /**
     * Redis 분산 락 획득 (SET NX EX — 원자적 SETNX + TTL).
     * 키가 없을 때만 UUID 토큰을 값으로 저장하여 소유권을 추적한다.
     *
     * @param key            락 키
     * @param timeoutSeconds 락 자동 해제까지 최대 대기 시간
     * @return 락 소유 토큰 (null이면 획득 실패 — 이미 다른 프로세스가 보유 중)
     */
    public String acquireLock(String key, long timeoutSeconds) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, Duration.ofSeconds(timeoutSeconds));
        if (Boolean.TRUE.equals(acquired)) {
            log.info("[RedisLock] 락 획득 key={} ttl={}s", key, timeoutSeconds);
            return token;
        }
        log.warn("[RedisLock] 락 획득 실패 (이미 점유 중) key={}", key);
        return null;
    }

    /**
     * Redis 분산 락 해제 (Lua compare-and-delete — 소유권 검증 후 삭제).
     * TTL 만료로 락이 재획득된 경우 다른 소유자의 락을 삭제하지 않는다.
     *
     * @param key   락 키
     * @param token {@link #acquireLock}이 반환한 소유 토큰
     */
    public void releaseLock(String key, String token) {
        Long result = stringRedisTemplate.execute(
                RELEASE_LOCK_SCRIPT,
                Collections.singletonList(key),
                token
        );
        if (Long.valueOf(1L).equals(result)) {
            log.info("[RedisLock] 락 해제 key={}", key);
        } else {
            log.warn("[RedisLock] 락 해제 스킵 (소유권 없음 — TTL 만료 후 재획득된 것으로 추정) key={}", key);
        }
    }
}