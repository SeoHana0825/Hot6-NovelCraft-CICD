package com.example.hot6novelcraft.domain.mentor.repository;

import com.example.hot6novelcraft.domain.mentor.entity.Mentor;
import com.example.hot6novelcraft.domain.mentor.entity.enums.MentorStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MentorRepository extends JpaRepository<Mentor, Long> {

    Optional<Mentor> findByUserId(Long userId);

    boolean existsByUserIdAndStatus(Long userId, MentorStatus status);

    // 배치용 - APPROVED 상태 멘토 전체 조회 (PROFICIENT 제외 - 관리자 수동)
    List<Mentor> findAllByStatusAndCareerLevelNot(MentorStatus status,
                                                  com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel careerLevel);
}