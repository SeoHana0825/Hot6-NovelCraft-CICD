package com.example.hot6novelcraft.domain.episode.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.EpisodeExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.NovelExceptionEnum;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeCreateRequest;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeUpdateRequest;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeCreateResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeDeleteResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodePublishResponse;
import com.example.hot6novelcraft.domain.episode.dto.response.EpisodeUpdateResponse;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.entity.EpisodeStatus;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.entity.NovelStatus;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.userEnum.UserRole;
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
    public EpisodeCreateResponse createEpisode(Long novelId, EpisodeCreateRequest request, UserDetailsImpl userDetails) {

        // 작가 권한 확인
        validateAuthorRole(userDetails);

        // 소설 조회(본인 소설 및 삭제여부)
        findNovelById(novelId, userDetails.getUser().getId());

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
    public EpisodeUpdateResponse updateEpisode(Long episodeId, EpisodeUpdateRequest request, UserDetailsImpl userDetails) {

        // 작가 권한 확인
        validateAuthorRole(userDetails);

        // 회차 조회
        Episode episode = findEpisodeById(episodeId);

        // 본인 소설 회차인지 확인
        findNovelById(episode.getNovelId(), userDetails.getUser().getId());

        // 회차 수정
        episode.update(request.title(), request.content());

        return EpisodeUpdateResponse.from(episode.getId());
    }

    // 회차 삭제
    @Transactional
    public EpisodeDeleteResponse deleteEpisode(Long episodeId, UserDetailsImpl userDetails) {

        // 작가 권한 확인
        validateAuthorRole(userDetails);

        // 회차 조회
        Episode episode = findEpisodeById(episodeId);

        // 본인 소설 회차인지 확인
        findNovelById(episode.getNovelId(), userDetails.getUser().getId());

        // 마지막 회차만 삭제 가능
        if (episodeRepository.existsByNovelIdAndEpisodeNumberGreaterThanAndIsDeletedFalse(
                episode.getNovelId(), episode.getEpisodeNumber())) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_DELETE_NOT_LAST);
        }

        // 회차 삭제 (소프트 딜리트)
        episode.delete();

        return EpisodeDeleteResponse.from(episode.getId());
    }

    // 회차 발행
    @Transactional
    public EpisodePublishResponse publishEpisode(Long episodeId, UserDetailsImpl userDetails) {

        // 작가 권한 확인
        validateAuthorRole(userDetails);

        // 회차 조회
        Episode episode = findEpisodeById(episodeId);

        // 본인 소설 회차인지 확인
        Novel novel = findNovelById(episode.getNovelId(), userDetails.getUser().getId());

        // 이미 발행된 회차 확인
        if (episode.getStatus() == EpisodeStatus.PUBLISHED) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_ALREADY_PUBLISHED);
        }

        // 본문 내용 없으면 발행 불가
        if (episode.getContent() == null || episode.getContent().isBlank()) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_CONTENT_EMPTY);
        }

        // 이전 회차 순서 검증 (1화부터 순서대로 발행)
        if (episodeRepository.existsByNovelIdAndEpisodeNumberLessThanAndStatusNotAndIsDeletedFalse(
                episode.getNovelId(), episode.getEpisodeNumber(), EpisodeStatus.PUBLISHED)) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_PREVIOUS_NOT_PUBLISHED);
        }

        // 회차 발행
        episode.publish();

        // 1화 발행 시 소설 연재중으로 변경
        if (episode.getEpisodeNumber() == 1) {
            novel.changeStatus(NovelStatus.ONGOING);
        }
        return EpisodePublishResponse.from(episode.getId());
    }


    // -----------------------------------------공통 매서드---------------------------------------------------------------

    // 작가 권한 확인 공통 메서드
    private void validateAuthorRole(UserDetailsImpl userDetails) {
        if (userDetails.getUser().getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_FORBIDDEN);
        }
    }

    // 소설 조회 공통 메서드(본인 소설 및 삭제여부)
    private Novel findNovelById(Long novelId, Long userId) {
        Novel novel = novelRepository.findById(novelId)
                .orElseThrow(() -> new ServiceErrorException(NovelExceptionEnum.NOVEL_NOT_FOUND));

        if (novel.isDeleted()) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_ALREADY_DELETED);
        }

        if (!novel.getAuthorId().equals(userId)) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_FORBIDDEN);
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