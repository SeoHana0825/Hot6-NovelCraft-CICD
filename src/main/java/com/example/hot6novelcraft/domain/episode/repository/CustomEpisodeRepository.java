package com.example.hot6novelcraft.domain.episode.repository;

import com.example.hot6novelcraft.domain.episode.dto.response.AuthorEpisodeListResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeListResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeMetaDto;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomEpisodeRepository {

    // 회차 목록 조회 (QueryDSL + 인덱싱)
    Page<EpisodeListResponse> findEpisodeListByNovelId(Long novelId, Pageable pageable);

    // 회차 본문 조회 (20회 단위)
    List<Episode> findBulkEpisodes(Long novelId, int startNumber, int endNumber);

    // 회차 메타 정보 조회 (content 제외, 가벼운 쿼리)
    EpisodeMetaDto findMetaById(Long episodeId);

    // 작가용 회차 목록 조회 (본인 소설의 회차, DRAFT 포함)
    Page<AuthorEpisodeListResponse> findAuthorEpisodeList(Long novelId, Pageable pageable);
}
