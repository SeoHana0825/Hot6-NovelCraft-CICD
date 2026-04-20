package com.example.hot6novelcraft.domain.point.repository;

import com.example.hot6novelcraft.domain.point.entity.PointHistory;
import com.example.hot6novelcraft.domain.point.entity.enums.PointHistoryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {

    Page<PointHistory> findByUserId(Long userId, Pageable pageable);

    // 회차 구매 여부 확인
    boolean existsByUserIdAndEpisodeIdAndType(Long userId, Long episodeId, PointHistoryType type);

    // 소설의 구매한 회차 ID 목록 조회
    @Query("SELECT ph.episodeId FROM PointHistory ph WHERE ph.userId = :userId AND ph.novelId = :novelId AND ph.type = :type AND ph.episodeId IS NOT NULL")
    List<Long> findPurchasedEpisodeIds(@Param("userId") Long userId, @Param("novelId") Long novelId, @Param("type") PointHistoryType type);
}