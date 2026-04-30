package com.example.hot6novelcraft.domain.admin.controller;

import com.example.hot6novelcraft.common.dto.BaseResponse;
import com.example.hot6novelcraft.domain.admin.dto.response.AdminDashboardResponse;
import com.example.hot6novelcraft.domain.admin.service.AdminDashboardService;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final BasicErrorController basicErrorController;

    /** 대시보드 전체 통계 조회
     * 전체 회원 수 (탈퇴 회원 제외, GET /api/admin/dashboard)
     * 독자 회원 수 (GET /api/admin/dashboard?role=READER)
     * 작가 회원 수 (GET /api/admin/dashboard?role=AUTHOR)
     * 전체 소설 수 (휴재, 보류, 삭제 포함, GET /api/admin/dashboard?novelStatus=ALL)
     * 연재 중인 소설 수 (GET /api/admin/dashboard?novelStatus=ONGOING)
     * 보류 중인 소설 수 (GET /api/admin/dashboard?novelStatus=PENDING)
     * 휴재 중인 소설 수 (GET /api/admin/dashboard?novelStatus=HIATUS)
     * 완결된 소설 수 (GET /api/admin/dashboard?novelStatus=COMPLETED)
     * 삭제된 소설 수 (GET /api/admin/dashboard?novelStatus=isDeleted=true)
     * */

    @GetMapping
    public ResponseEntity<BaseResponse<AdminDashboardResponse>> getDashboard(
            @RequestParam(required = false)UserRole role
            , @RequestParam(required = false)String status
            , @RequestParam(required = false)Boolean isDeleted
    ) {
        AdminDashboardResponse response = adminDashboardService.getDashboardStatus(role, status, isDeleted);
        return ResponseEntity.ok(BaseResponse.success("200", "통계 출력 완료", response));
    }
}
