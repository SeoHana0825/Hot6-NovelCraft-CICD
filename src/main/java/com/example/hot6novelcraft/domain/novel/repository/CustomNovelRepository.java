package com.example.hot6novelcraft.domain.novel.repository;

import com.example.hot6novelcraft.domain.novel.dto.response.NovelDetailResponse;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelListResponse;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CustomNovelRepository {

    // V2 - 소설 목록 조회 (QueryDSL + 필터링(상태, 태그))
    Page<NovelListResponse> findNovelListV2(String genre, NovelStatus status, Pageable pageable);

    // 소설 상세 조회 (QueryDSL + 인덱싱)
    NovelDetailResponse findNovelDetailByNovelId(Long novelId);
}
