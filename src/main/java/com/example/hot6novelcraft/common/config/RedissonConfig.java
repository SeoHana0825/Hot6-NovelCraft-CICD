package com.example.hot6novelcraft.common.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetSocketAddress;
import java.util.List;

@Configuration
public class RedissonConfig {

//    @Value("${spring.data.redis.host}")
//    private String host;
//
//    @Value("${spring.data.redis.port}")
//    private int port;

//    싱글 Redis
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        config.useSingleServer()
//              .setAddress("redis://" + host + ":" + port);
//        return Redisson.create(config);
//    }

    // 마스터 - 슬레이브
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        config.useSentinelServers()
                .setMasterName("mymaster")
                .addSentinelAddress(
                        "redis://localhost:26379"
                        , "redis://localhost:26380"
                        , "redis://localhost:26381"
                )
                .setCheckSentinelsList(false)  // sentinel 인식 확인 로직
                .setReadMode(org.redisson.config.ReadMode.SLAVE)
                .setConnectTimeout(1000)
                .setRetryAttempts(3)
                .setRetryInterval(1500)
                .setNatMapper(uri -> new org.redisson.misc.RedisURI(
                        uri.getScheme() + "://127.0.0.1:" + uri.getPort()
                ));
        return Redisson.create(config);
    }
}