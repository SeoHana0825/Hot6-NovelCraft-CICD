package com.example.hot6novelcraft.domain.episode.repository;

import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface EpisodeRepository extends JpaRepository<Episode, Long>, CustomEpisodeRepository {

    // 회차 중복 확인
    boolean existsByNovelIdAndEpisodeNumberAndIsDeletedFalse(Long novelId, int episodeNumber);

    // 소설의 총 회차 수 조회
    int countByNovelIdAndIsDeletedFalse(Long novelId);

    // 해당 회차보다 큰 회차 번호가 존재하는지 확인(중간 회차 삭제 못하게!)
    boolean existsByNovelIdAndEpisodeNumberGreaterThanAndIsDeletedFalse(Long novelId, int episodeNumber);

    // 이전 회차 중 PUBLISHED 아닌 것 있는지 확인(순서대로 회차 발행 검증을 위해)
    boolean existsByNovelIdAndEpisodeNumberLessThanAndStatusNotAndIsDeletedFalse(Long novelId, int episodeNumber, EpisodeStatus status);

    @Query("SELECT e.novelId, COUNT(e) FROM Episode e " +
            "WHERE e.novelId IN :novelIds AND e.isDeleted = false " +
            "GROUP BY e.novelId")
    List<Object[]> countByNovelIdsRaw(@Param("novelIds") List<Long> novelIds);

    default Map<Long, Long> countByNovelIds(List<Long> novelIds) {
        if (novelIds.isEmpty()) return Map.of();
        return countByNovelIdsRaw(novelIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (Long) row[1]
                ));
    }
}
