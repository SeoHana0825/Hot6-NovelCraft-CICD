package com.example.hot6novelcraft.domain.exchange.repository;

import com.example.hot6novelcraft.domain.exchange.dto.response.RevenueStatisticsResponse.RevenueStatisticsItem;
import com.example.hot6novelcraft.domain.exchange.entity.Revenue;
import com.example.hot6novelcraft.domain.exchange.entity.enums.RevenueType;
import com.example.hot6novelcraft.domain.exchange.entity.enums.StatisticsPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import com.example.hot6novelcraft.common.config.QuerydslConfig; // 프로젝트의 QueryDSL 설정 클래스명 확인 필요

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // 자동 교체 방지
@Import(QuerydslConfig.class)
class RevenueRepositoryTest {

    @Autowired
    private RevenueRepository revenueRepository;

    @Test
    @DisplayName("성공 - QueryDSL 통계 쿼리가 정확한 금액을 합산하는지 확인한다")
    void findStatistics_Success() {
        // given
        Long authorId = 1L;
        // EPISODE_SALE 타입으로 데이터 저장 (수정된 부분)
        revenueRepository.save(Revenue.create(authorId, 100L, 5000, 5000, RevenueType.EPISODE_SALE));
        revenueRepository.save(Revenue.create(authorId, 101L, 3000, 8000, RevenueType.EPISODE_SALE));
        revenueRepository.save(Revenue.create(authorId, 102L, 2000, 10000, RevenueType.SUBSCRIPTION));

        // when
        // Impl 클래스의 QueryDSL 로직이 실행됨
        List<RevenueStatisticsItem> result = revenueRepository.findStatistics(authorId, StatisticsPeriod.MONTHLY, 2026);

        // then
        assertThat(result).isNotEmpty();

        // EPISODE_SALE(8000) + SUBSCRIPTION(2000) = 10000 확인
        // totalAmount() 메서드 명칭은 레코드 정의에 따름
        Integer total = result.stream()
                .mapToInt(RevenueStatisticsItem::totalAmount)
                .sum();

        assertThat(total).isEqualTo(10000);
    }
}