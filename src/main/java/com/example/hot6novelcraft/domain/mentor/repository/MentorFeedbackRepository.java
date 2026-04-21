package com.example.hot6novelcraft.domain.mentor.repository;

import com.example.hot6novelcraft.domain.mentor.entity.MentorFeedbackV2;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MentorFeedbackRepository extends JpaRepository<MentorFeedbackV2, Long> {

    List<MentorFeedbackV2> findAllByMentorshipIdOrderByCreatedAtAsc(Long mentorshipId);

    Optional<MentorFeedbackV2> findTopByMentorshipIdOrderByCreatedAtDesc(Long mentorshipId);
}