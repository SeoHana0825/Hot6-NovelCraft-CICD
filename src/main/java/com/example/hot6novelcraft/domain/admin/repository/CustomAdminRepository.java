package com.example.hot6novelcraft.domain.admin.repository;

import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;

public interface CustomAdminRepository {

    // 회원 통계
    Long countTotalUsers();
    Long countNewUsersToday();
    Long countUsersByRole(UserRole role);

    // 소설 통계
    Long countTotalNovels(String status);
    Long countNewNovelsToday();
    Long countNovelsByFilter(NovelStatus status, Boolean isDeleted);

    // 멘토링 통계
    Long countTotalMentors();
    Long countNewMentorsToday();
}