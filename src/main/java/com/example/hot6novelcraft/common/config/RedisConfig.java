package com.example.hot6novelcraft.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = redisObjectMapper();

        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // key-value 관리 설정 1 : Redis 에서 받아올 때, 문자열 Json타입으로 파싱
        template.setKeySerializer(new StringRedisSerializer());
        //template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setValueSerializer(serializer);


        // key-value 관리 설정 2 : DTO 객체는 Hash Type 으로 받아올 수 있게 파싱
        template.setHashKeySerializer(new StringRedisSerializer());
        //template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(serializer);


        template.afterPropertiesSet();
        return template;
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }
}
