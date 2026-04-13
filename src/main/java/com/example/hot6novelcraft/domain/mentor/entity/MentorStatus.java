package com.example.hot6novelcraft.domain.mentor.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MentorStatus {
    PENDING("심사 대기"),
    APPROVED("승인 완료"),
    REJECTED("등록 반려");

    private final String description;
}
