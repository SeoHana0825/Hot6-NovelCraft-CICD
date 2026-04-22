package com.example.hot6novelcraft.domain.mentoring.dto.response;

import com.example.hot6novelcraft.domain.mentoring.entity.enums.MentorshipStatus;

import java.time.LocalDateTime;

public record MentorshipHistoryResponse(
        Long mentorshipId,
        String mentorNickname,
        MentorshipStatus status,
        LocalDateTime appliedAt
) {
}