package com.example.hot6novelcraft.domain.point.repository;

import com.example.hot6novelcraft.domain.point.entity.PointHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointHistoryRepository extends JpaRepository<PointHistory, Long> {
}