package com.example.hot6novelcraft.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedissonClient redissonClient;

    /** 블랙리스트 등록
     * key - Token
     * value - 상태
     * duration - 유효시간
     **/
    public void setBlackList(String accessToken, Object object, Duration duration) {
        redisTemplate.opsForValue().set(accessToken, object, duration);
    }

    public boolean isBlackList(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessToken));
    }

    /** SMS 인증번호 저장 및 재사용 방지 및 삭제
    * 1. 데이터 조회
    * 2. 데이터 삭제 (인증 성공 후 바로 삭제)
    * 3. 데이터 저장 (TTL 사용)
    **/
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public void set(String key, Object value, long duration) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMinutes(duration));
    }

    /**
     * Redis 분산 락 획득 (Redisson RLock).
     * tryLock()은 대기 없이 즉시 시도하며, 획득 성공 시 워치독이 TTL을 자동 갱신한다.
     * 워치독 덕분에 작업 시간이 길어져도 락이 만료되지 않는다.
     *
     * @return true면 락 획득 성공, false면 이미 다른 프로세스가 보유 중
     */
    public boolean acquireLock(String key) {
        boolean acquired = redissonClient.getLock(key).tryLock();
        if (acquired) {
            log.info("[RedisLock] 락 획득 key={}", key);
        } else {
            log.warn("[RedisLock] 락 획득 실패 (이미 점유 중) key={}", key);
        }
        return acquired;
    }

    /**
     * Redis 분산 락 해제.
     * 현재 스레드가 보유한 락만 해제한다 (다른 스레드의 락은 건드리지 않음).
     */
    public void releaseLock(String key) {
        RLock lock = redissonClient.getLock(key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("[RedisLock] 락 해제 key={}", key);
        } else {
            log.warn("[RedisLock] 락 해제 스킵 (현재 스레드가 소유하지 않음) key={}", key);
        }
    }
}
