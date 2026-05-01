package com.example.hot6novelcraft.domain.admin.service;

import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.domain.admin.dto.response.AdminReportListResponse;
import com.example.hot6novelcraft.domain.admin.repository.AdminReportRepository;
import com.example.hot6novelcraft.domain.report.entity.enums.ReportStatus;
import com.example.hot6novelcraft.domain.report.entity.enums.ReportTargetType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j(topic = "AdminReportService")
@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {

    private final AdminReportRepository adminReportRepository;

    // 신고 목록 조회 (필터링 + 페이징)
    @Transactional(readOnly = true)
    public PageResponse<AdminReportListResponse> getReportList(ReportStatus status, ReportTargetType targetType, Pageable pageable
    ) {
        Page<AdminReportListResponse> reports =
                adminReportRepository.findReportList(status, targetType, pageable);

        return PageResponse.register(reports);
    }
}
