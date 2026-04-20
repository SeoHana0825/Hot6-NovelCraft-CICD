package com.example.hot6novelcraft.domain.mentoring.repository;

import com.example.hot6novelcraft.domain.mentoring.entity.Mentorship;
import com.example.hot6novelcraft.domain.mentoring.entity.enums.MentorshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MentorshipRepository extends JpaRepository<Mentorship, Long> {

    Page<Mentorship> findAllByMentorIdOrderByCreatedAtDesc(Long mentorId, Pageable pageable);

    // 대기 중 건수
    long countByMentorIdAndStatus(Long mentorId, MentorshipStatus status);

    // 이번 달 수락/거절 건수
    @Query("SELECT COUNT(m) FROM Mentorship m WHERE m.mentorId = :mentorId AND m.status = :status AND m.acceptedAt >= :startOfMonth")
    long countByMentorIdAndStatusAndAcceptedAtAfter(@Param("mentorId") Long mentorId,
                                                    @Param("status") MentorshipStatus status,
                                                    @Param("startOfMonth") LocalDateTime startOfMonth);

    @Query("SELECT COUNT(m) FROM Mentorship m WHERE m.mentorId = :mentorId AND m.status = :status AND m.rejectedAt >= :startOfMonth")
    long countRejectedThisMonth(@Param("mentorId") Long mentorId,
                                @Param("status") MentorshipStatus status,
                                @Param("startOfMonth") LocalDateTime startOfMonth);

    List<Mentorship> findAllByMentorIdAndStatus(Long mentorId, MentorshipStatus status);

}