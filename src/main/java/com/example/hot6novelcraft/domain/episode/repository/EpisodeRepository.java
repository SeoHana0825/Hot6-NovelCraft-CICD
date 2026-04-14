package com.example.hot6novelcraft.domain.episode.repository;

import com.example.hot6novelcraft.domain.episode.entity.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long>, CustomEpisodeRepository {

    // 회차 중복 확인
    boolean existsByNovelIdAndEpisodeNumber(Long novelId, int episodeNumber);

    // 소설의 총 회차 수 조회
    int countByNovelId(Long novelId);

}
