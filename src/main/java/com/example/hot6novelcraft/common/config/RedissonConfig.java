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
    @Value("${spring.data.redis.sentinel.master}")
    private String masterName;

    @Value("${spring.data.redis.sentinel.nodes}")
    private List<String> sentinelNodes;

    // 값 없으면 빈 문자열 주입
    @Value("${app.redis.nat-mapping-ip:}")
    private String natMappingIp;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();

        // yml 에서 가져온 노드 주소들 "redis://" 붙여서 배열로 변환
        String[] nodesArray = sentinelNodes.stream()
                .map(node -> "redis://" + node)
                .toArray(String[]::new);

        org.redisson.config.SentinelServersConfig sentinelConfig = config.useSentinelServers()                .setMasterName(masterName)
                .addSentinelAddress(nodesArray)
                .setCheckSentinelsList(false)  // sentinel 인식 확인
                .setReadMode(org.redisson.config.ReadMode.SLAVE)
                .setConnectTimeout(1000)
                .setRetryAttempts(3)
                .setRetryInterval(1500);

        // 배포 시 같은 네트워크 써서 번역기가 필요 없다면 작동하지 않음
        if(natMappingIp != null && !natMappingIp.isBlank()) {
            sentinelConfig.setNatMapper(uri -> new org.redisson.misc.RedisURI(
                    uri.getScheme() + "://" + natMappingIp + ":" + uri.getPort()
            ));
        }
        return Redisson.create(config);
    }
}