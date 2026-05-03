package com.example.hot6novelcraft.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;

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

    /** SignupService
     * Redis 원자성 보장 (getAndDelete)
     * */
    public Object getAndDelete(String signupKey) {
        return redisTemplate.opsForValue().getAndDelete(signupKey);
    }

    /** SmsService
     * Redis 원자성 보장 및 동시성 제어 적용
     * Lua Script를 활용한 SMS 인증번호 원자적 검증 및 삭제
     */
    public boolean verifyAndDeleteWithLua(String key, String expectedValue) {
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1] then \n" +
                "   redis.call('del', KEYS[1]) \n" +
                "   return 1 \n" +
                "else \n" +
                "   return 0 \n" +
                "end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        // 스크립트 실행
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(key), expectedValue);

        return result != null && result == 1L;
    }

    /** SMS 인증번호 저장 및 재사용 방지 및 삭제
    * 1. 데이터 조회 - get
    * 2. 데이터 삭제 (인증 성공 후 바로 삭제) -delete
    * 3. 데이터 저장 (TTL 사용) - set
    * 4. 일일 전송 제한 (중복 전송 방지 및 비용 과금 방지) - incrementAndExpire
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

    public Long incrementAndExpire(String key, long durationMinutes) {

        String script =
                "local count = redis.call('INCR', KEYS[1]) \n" +
                        "if tonumber(count) == 1 then \n" +
                        "   redis.call('EXPIRE', KEYS[1], ARGV[1]) \n" +
                        "end \n" +
                        "return count";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(script, Long.class);

        // durationMinutes * 60을 통해 초 단위로 변환해 넘겨줌
        return redisTemplate.execute(redisScript, Collections.singletonList(key), durationMinutes * 60);
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

    /** 대시보드 통계용 - TTL 초 단위
     * 1. setWithSeconds 으로 초단위 업데이트 설정
     * 2. 캐시 값 문자열로 파싱
     * 3. Redis INCR + 1 (동시성 제어)
     *    키가 없으면 1로 초기화하고 TTL 설정
     **/
    public void setWithSeconds(String key, Object value, long seconds) {

        redisTemplate.execute((RedisCallback<Void>) connection -> {

            StringRedisSerializer serializer = new StringRedisSerializer();
            byte[] rawKey = serializer.serialize(key);
            byte[] rawValue = serializer.serialize(value.toString());

            // Redis setEx 명령어로 TTL이랑 저장
            connection.setEx(rawKey, seconds, rawValue);
            return null;
        });
    }

    public String getAsString(String key) {

        Object value = redisTemplate.opsForValue().get(key);

        return value != null ? value.toString() : null;

    }
    public long incrementWithSeconds(String key, long durationSeconds) {
        StringRedisSerializer serializer = new StringRedisSerializer();

        Long result = redisTemplate.execute((RedisCallback<Long>) connection -> {
            byte[] rawKey = serializer.serialize(key);

            String script =
                    "local count = redis.call('INCR', KEYS[1]) \n" +
                    "if tonumber(count) == 1 then \n" +
                    "   redis.call('EXPIRE', KEYS[1], ARGV[1]) \n" +
                    "end \n" +
                    "return count";

            byte[] rawArg = serializer.serialize(String.valueOf(durationSeconds));

            return connection.eval(
                    script.getBytes()
                    , ReturnType.INTEGER
                    ,1
                    , rawKey
                    , rawArg
            );
        });
        return result != null ? result : 0L;
    }
}