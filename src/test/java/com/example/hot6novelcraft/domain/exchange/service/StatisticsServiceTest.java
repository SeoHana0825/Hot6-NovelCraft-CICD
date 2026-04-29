package com.example.hot6novelcraft.domain.exchange.service;

import com.example.hot6novelcraft.domain.exchange.dto.response.RevenueStatisticsResponse;
import com.example.hot6novelcraft.domain.exchange.dto.response.RevenueStatisticsResponse.RevenueStatisticsItem;
import com.example.hot6novelcraft.domain.exchange.entity.enums.StatisticsPeriod;
import com.example.hot6novelcraft.domain.exchange.repository.RevenueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @InjectMocks
    private StatisticsService statisticsService;

    @Mock
    private RevenueRepository revenueRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private final Long AUTHOR_ID = 1L;

    @Test
    @DisplayName("성공 - 통계 조회 시 캐시가 없으면 DB에서 조회 후 캐시에 저장한다")
    void getStatistics_CacheMiss() {
        // given
        StatisticsPeriod period = StatisticsPeriod.MONTHLY;
        Integer year = 2026;
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(null); // 캐시 미스

        // RevenueStatisticsItem 생성
        RevenueStatisticsItem item = new RevenueStatisticsItem("2026-03", 20000, 10000, 30000);
        List<RevenueStatisticsItem> items = List.of(item);

        // 레포지토리 커스텀 메서드 findStatistics 반영
        given(revenueRepository.findStatistics(AUTHOR_ID, period, year)).willReturn(items);

        // when
        RevenueStatisticsResponse response = statisticsService.getStatistics(AUTHOR_ID, period, year);

        // then
        assertThat(response.totalAmount()).isEqualTo(30000);
        assertThat(response.items()).hasSize(1);
        // 캐시에 저장되는지 확인
        verify(valueOperations).set(anyString(), any(RevenueStatisticsResponse.class), any(Duration.class));
    }

    @Test
    @DisplayName("성공 - 캐시 히트 시 DB를 조회하지 않고 캐시 데이터를 반환한다")
    void getStatistics_CacheHit() {
        // given
        StatisticsPeriod period = StatisticsPeriod.MONTHLY;
        Integer year = 2026;
        RevenueStatisticsResponse cachedResponse = RevenueStatisticsResponse.of("MONTHLY", 2026, 10000, List.of());

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(anyString())).willReturn(cachedResponse); // 캐시 히트

        // when
        RevenueStatisticsResponse response = statisticsService.getStatistics(AUTHOR_ID, period, year);

        // then
        assertThat(response.totalAmount()).isEqualTo(10000);
        // DB 조회가 발생하지 않았는지 확인
        verify(revenueRepository, org.mockito.Mockito.never()).findStatistics(anyLong(), any(), anyInt());
    }
}