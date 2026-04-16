package com.example.hot6novelcraft.domain.user.entity.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    ADMIN,
    READER,
    AUTHOR,
    TEMP    // 공통 가입 완료 후 임시 상태
}
