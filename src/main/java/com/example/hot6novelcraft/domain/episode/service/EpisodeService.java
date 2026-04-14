package com.example.hot6novelcraft.domain.episode.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.EpisodeExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.NovelExceptionEnum;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeCreateRequest;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeUpdateRequest;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeCreateResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeDeleteResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeUpdateResponse;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private static final int EPISODE_PRICE = 200;
    private static final int FREE_EPISODE_LIMIT = 2;

    private final EpisodeRepository episodeRepository;
    private final NovelRepository novelRepository;

    // 회차 생성
    @Transactional
    public EpisodeCreateResponse createEpisode(Long novelId, EpisodeCreateRequest request) {

        // TODO : JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!

        // 소설 조회
        findNovelById(novelId);

        // 회차 번호 중복 확인(삭제된 회차 제외)
        if (episodeRepository.existsByNovelIdAndEpisodeNumberAndIsDeletedFalse(novelId, request.episodeNumber())) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NUMBER_DUPLICATE);
        }
        // 회차 번호 순서 검증 (ex : 1,2,3 ...10)
        int lastEpisodeNumber = episodeRepository.countByNovelIdAndIsDeletedFalse(novelId);
        if (request.episodeNumber() != lastEpisodeNumber + 1) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NUMBER_NOT_SEQUENTIAL);
        }

        // 무료/유료 자동 (1,2화 무료 / 3화부터 200포인트)
        boolean isFree = request.episodeNumber() <= FREE_EPISODE_LIMIT;
        int pointPrice = isFree ? 0 : EPISODE_PRICE;

        // 회차 생성 (초안 DRAFT)
        Episode episode = Episode.createEpisode(
                novelId,
                request.episodeNumber(),
                request.title(),
                request.content(),
                isFree,
                pointPrice
        );

        Episode savedEpisode = episodeRepository.save(episode);

        return EpisodeCreateResponse.from(savedEpisode.getId());
    }

    // 회차 수정
    @Transactional
    public EpisodeUpdateResponse updateEpisode(Long episodeId, EpisodeUpdateRequest request) {

        // TODO : JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!

        // 회차 조회
        Episode episode = findEpisodeById(episodeId);

        // 회차 수정
        episode.update(request.title(), request.content());

        return EpisodeUpdateResponse.from(episode.getId());
    }

    // 회차 삭제
    @Transactional
    public EpisodeDeleteResponse deleteEpisode(Long episodeId) {

        // TODO : JWT 구현후 작가ID로 교체 및 작가 권한 확인 예정임다!!!!!!!

        // 회차 조회
        Episode episode = findEpisodeById(episodeId);

        // 마지막 회차만 삭제 가능
        if (episodeRepository.existsByNovelIdAndEpisodeNumberGreaterThanAndIsDeletedFalse(
                episode.getNovelId(), episode.getEpisodeNumber())) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_DELETE_NOT_LAST);
        }

        // 회차 삭제 (소프트 딜리트)
        episode.delete();

        return EpisodeDeleteResponse.from(episode.getId());
    }

    // 소설 조회 공통 메서드
    private Novel findNovelById(Long novelId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new ServiceErrorException(NovelExceptionEnum.NOVEL_NOT_FOUND));

        if (novel.isDeleted()) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_ALREADY_DELETED);
        }

        return novel;
    }
    // 회차 조회 공통 메서드
    private Episode findEpisodeById(Long episodeId) {
        Episode episode = episodeRepository.findById(episodeId)
                .orElseThrow(() -> new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NOT_FOUND));

        if (episode.isDeleted()) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_ALREADY_DELETED);
        }
        return episode;
    }
}