package com.example.hot6novelcraft.domain.admin.service;

import com.example.hot6novelcraft.domain.admin.dto.response.AdminDashboardMentorsStatusResponse;
import com.example.hot6novelcraft.domain.admin.dto.response.AdminDashboardNovelStatusResponse;
import com.example.hot6novelcraft.domain.admin.dto.response.AdminDashboardResponse;
import com.example.hot6novelcraft.domain.admin.dto.response.AdminDashboardUserStatusResponse;
import com.example.hot6novelcraft.domain.admin.repository.CustomAdminRepository;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "AdminDashboardService")
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final CustomAdminRepository adminRepository;

    /** ==== 대시보드 전체 통계 조회 ====
     * role, novelStatus, isDeleted 가 모두 null이면 전체 통계
     * 값이 있으면 해당 필터 적용
     */
    public AdminDashboardResponse getDashboardStatus(
            UserRole role
            , String status
            , Boolean isDeleted
    ) {
        NovelStatus filterStatus = null;
        if (status != null && !"ALL".equalsIgnoreCase(status)) {
            filterStatus = NovelStatus.valueOf(status.toUpperCase());
        }
        AdminDashboardUserStatusResponse userStatus = AdminDashboardUserStatusResponse.of(
                adminRepository.countTotalUsers()
                , adminRepository.countNewUsersToday()
                , adminRepository.countUsersByRole(role)
        );

        AdminDashboardNovelStatusResponse novelStatus = AdminDashboardNovelStatusResponse.of(
                adminRepository.countTotalNovels(status)
                , adminRepository.countNewNovelsToday()
                , adminRepository.countNovelsByFilter(filterStatus, isDeleted)
        );

        AdminDashboardMentorsStatusResponse mentorStatus = AdminDashboardMentorsStatusResponse.of(
                adminRepository.countTotalMentors()
                , adminRepository.countNewMentorsToday()
        );

        return AdminDashboardResponse.of(userStatus, novelStatus, mentorStatus);
    }
}
