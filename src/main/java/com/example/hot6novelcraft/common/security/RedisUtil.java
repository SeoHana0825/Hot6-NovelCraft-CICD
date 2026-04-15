    package com.example.hot6novelcraft.common.security;

    import lombok.RequiredArgsConstructor;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.stereotype.Component;

    import java.time.Duration;

    @Component
    @RequiredArgsConstructor
    public class RedisUtil {

        // 로그아웃 블랙리스트 메소드 셋팅
        private final RedisTemplate<String, Object> redisTemplate;

        // 블랙리스트 등록 (key -Token, value -상태, duration -유효시간)
        public void setBlackList (String accessToken, Object object, Duration duration) {
            redisTemplate.opsForValue().set (accessToken, object, duration);
        }

        public boolean isBlackList (String accessToken) {
            return Boolean.TRUE.equals (redisTemplate.hasKey(accessToken));
        }
    }