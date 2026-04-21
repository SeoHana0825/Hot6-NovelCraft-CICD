package com.example.hot6novelcraft.domain.mentoring.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MentoringFeedbackRequest(
        @NotBlank(message = "피드백 제목을 입력해 주세요")
        String title,

        @NotBlank(message = "피드백 내용을 입력해 주세요")
        String content
) {
}