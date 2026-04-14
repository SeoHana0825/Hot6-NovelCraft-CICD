package com.example.hot6novelcraft.domain.episode.repository;

import com.example.hot6novelcraft.domain.episode.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long>, CustomEpisodeRepository {

    // 회차 중복 확인
    boolean existsByNovelIdAndEpisodeNumberAndIsDeletedFalse(Long novelId, int episodeNumber);

    // 소설의 총 회차 수 조회
    int countByNovelIdAndIsDeletedFalse(Long novelId);

    // 해당 회차보다 큰 회차 번호가 존재하는지 확인(중간 회차 삭제 못하게!)
    boolean existsByNovelIdAndEpisodeNumberGreaterThanAndIsDeletedFalse(Long novelId, int episodeNumber);


}
