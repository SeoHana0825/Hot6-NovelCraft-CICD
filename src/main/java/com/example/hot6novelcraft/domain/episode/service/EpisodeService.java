package com.example.hot6novelcraft.domain.episode.service;

import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.EpisodeExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.NovelExceptionEnum;
import com.example.hot6novelcraft.domain.episode.dto.cache.EpisodeBulkCache;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeCreateRequest;
import com.example.hot6novelcraft.domain.episode.dto.request.EpisodeUpdateRequest;
import com.example.hot6novelcraft.domain.episode.dto.response.*;
import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.entity.enums.EpisodeStatus;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.point.entity.enums.PointHistoryType;
import com.example.hot6novelcraft.domain.point.repository.PointHistoryRepository;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class EpisodeService {

    private static final int EPISODE_PRICE = 200;
    private static final int FREE_EPISODE_LIMIT = 2;

    private final EpisodeRepository episodeRepository;
    private final NovelRepository novelRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final EpisodeCacheService episodeCacheService;

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

        // 벌크 캐시 무효화
        episodeCacheService.evictBulkCache(episode.getNovelId());

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

        // 벌크 캐시 무효화
        episodeCacheService.evictBulkCache(episode.getNovelId());

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

        // 벌크 캐시 무효화
        episodeCacheService.evictBulkCache(episode.getNovelId());

        return EpisodePublishResponse.from(episode.getId());
    }

    // 회차 목록 조회 (QueryDSL + 인덱싱)
    @Transactional(readOnly = true)
    public PageResponse<EpisodeListResponse> getEpisodeList(Long novelId, Pageable pageable) {

        // 소설 존재 여부 확인
        novelRepository.findById(novelId)
                .orElseThrow(() -> new ServiceErrorException(NovelExceptionEnum.NOVEL_NOT_FOUND));

        // 회차 목록 조회 (PUBLISHED만)
        Page<EpisodeListResponse> episodes = episodeRepository.findEpisodeListByNovelId(novelId, pageable);

        return PageResponse.register(episodes);
    }

    // 회차 본문 조회 V1 (JPA 단건 조회)
    @Transactional
    public EpisodeDetailResponse getEpisodeContentV1(Long episodeId, UserDetailsImpl userDetails) {

        Long userId = userDetails.getUser().getId();

        // 회차 조회
        Episode episode = findEpisodeById(episodeId);

        // 발행된 회차인지 확인
        if (episode.getStatus() != EpisodeStatus.PUBLISHED) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NOT_PUBLISHED);
        }

        // 유료 회차 접근 제어 (PointHistory 이력 체크)
        validateEpisodeAccess(episode, userId);

        // 소설 조회수 +1 (어뷰징 방지)
        increaseNovelViewCount(episode.getNovelId(), userId);

        return EpisodeDetailResponse.from(episode);
    }

    // 회차 본문 조회 V2 (Hot Key + 벌크 캐싱)
    @Transactional
    public EpisodeDetailResponse getEpisodeContentV2(Long episodeId, UserDetailsImpl userDetails) {

        Long userId = userDetails.getUser().getId();

        // 회차 메타 정보만 조회 (content 제외 - 가벼운 쿼리)
        EpisodeMetaDto meta = episodeRepository.findMetaById(episodeId);

        // 회차 존재 여부 + 삭제 여부 체크
        if (meta == null || meta.isDeleted()) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NOT_FOUND);
        }

        // 발행 여부 체크
        if (meta.status() != EpisodeStatus.PUBLISHED) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NOT_PUBLISHED);
        }

        // 유료 회차 접근 제어 (메타 기반)
        validateEpisodeAccessByMeta(meta, userId);

        // 소설 조회수 +1 (어뷰징 방지)
        increaseNovelViewCount(meta.novelId(), userId);

        // Hot Key 카운터 증가
        long recentViews = episodeCacheService.increaseHotKeyCount(meta.novelId());

        // 비인기작 → 이때만 Episode 전체 조회 (content 포함)
        if (!episodeCacheService.isHotNovel(recentViews)) {
            Episode episode = findEpisodeById(episodeId);
            return EpisodeDetailResponse.from(episode);
        }

        // 인기작 → 벌크 캐시 사용 (content는 캐시에서!)
        return getEpisodeFromBulkCacheByMeta(meta);
    }

    // 작가용 회차 목록 조회 (에디터용)
    @Transactional(readOnly = true)
    public PageResponse<AuthorEpisodeListResponse> getAuthorEpisodeList(Long novelId, UserDetailsImpl userDetails, Pageable pageable) {

        // 작가 권한 확인
        validateAuthorRole(userDetails);

        // 본인 소설 확인 (다른 작가 소설 회차 조회 방지)
        findNovelById(novelId, userDetails.getUser().getId());

        // 회차 목록 조회 (DRAFT 포함)
        Page<AuthorEpisodeListResponse> episodes =
                episodeRepository.findAuthorEpisodeList(novelId, pageable);

        return PageResponse.register(episodes);
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

        // 본인 소설 확인 먼저
        if (!Objects.equals(novel.getAuthorId(), userId)) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_FORBIDDEN);
        }

        // 삭제 여부
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

    // 소설 조회수 +1 (어뷰징 방지)
    private void increaseNovelViewCount(Long novelId, Long userId) {
        if (episodeCacheService.isFirstView(userId, novelId)) {
            novelRepository.incrementViewCount(novelId);
        }
    }

    // 유료 회차 접근 제어 (PointHistory 이력 체크)
    private void validateEpisodeAccess(Episode episode, Long userId) {

        if (episode.isFree()) {
            return;
        }

        // 구매이력조회
        boolean hasPurchased = pointHistoryRepository
                .existsByUserIdAndEpisodeIdAndType(userId, episode.getId(), PointHistoryType.NOVEL);

        if (!hasPurchased) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_POINT_REQUIRED);
        }
    }

    // 벌크 캐시에서 회차 조회 (인기작만)
    private EpisodeDetailResponse getEpisodeFromBulkCache(Episode episode) {

        Long novelId = episode.getNovelId();
        int episodeNumber = episode.getEpisodeNumber();
        int bulkIndex = episodeCacheService.calculateBulkIndex(episodeNumber);

        // 캐시 조회
        List<EpisodeBulkCache> bulk = episodeCacheService.getBulkCache(novelId, bulkIndex);

        // MISS → DB에서 벌크 조회 후 캐싱
        if (bulk == null) {
            int startNumber = episodeCacheService.getBulkStartNumber(bulkIndex);
            int endNumber = episodeCacheService.getBulkEndNumber(bulkIndex);

            List<Episode> bulkEpisodes = episodeRepository.findBulkEpisodes(novelId, startNumber, endNumber);
            episodeCacheService.saveBulkCache(novelId, bulkIndex, bulkEpisodes);

            bulk = bulkEpisodes.stream()
                    .map(EpisodeBulkCache::from)
                    .toList();
        }

        // 해당 회차 찾기
        return bulk.stream()
                .filter(cache -> cache.episodeNumber() == episodeNumber)
                .findFirst()
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NOT_FOUND));
    }

    // EpisodeBulkCache -> EpisodeDetailResponse 변환
    private EpisodeDetailResponse toDetailResponse(EpisodeBulkCache cache) {
        return new EpisodeDetailResponse(
                cache.episodeId(),
                cache.episodeNumber(),
                cache.title(),
                cache.content(),
                cache.likeCount(),
                cache.isFree(),
                cache.pointPrice()
        );
    }


    // 유료 회차 접근 제어 (메타 기반 - V2 전용)
    private void validateEpisodeAccessByMeta(EpisodeMetaDto meta, Long userId) {

        if (meta.isFree()) {
            return;
        }

        boolean hasPurchased = pointHistoryRepository
                .existsByUserIdAndEpisodeIdAndType(userId, meta.id(), PointHistoryType.NOVEL);

        if (!hasPurchased) {
            throw new ServiceErrorException(EpisodeExceptionEnum.EPISODE_POINT_REQUIRED);
        }
    }

    // 벌크 캐시에서 회차 조회 (메타 기반 - V2 전용)
    private EpisodeDetailResponse getEpisodeFromBulkCacheByMeta(EpisodeMetaDto meta) {

        Long novelId = meta.novelId();
        int episodeNumber = meta.episodeNumber();
        int bulkIndex = episodeCacheService.calculateBulkIndex(episodeNumber);

        // 캐시 조회
        List<EpisodeBulkCache> bulk = episodeCacheService.getBulkCache(novelId, bulkIndex);

        // MISS → DB에서 벌크 조회 후 캐싱
        if (bulk == null) {
            int startNumber = episodeCacheService.getBulkStartNumber(bulkIndex);
            int endNumber = episodeCacheService.getBulkEndNumber(bulkIndex);

            List<Episode> bulkEpisodes = episodeRepository.findBulkEpisodes(novelId, startNumber, endNumber);
            episodeCacheService.saveBulkCache(novelId, bulkIndex, bulkEpisodes);

            bulk = bulkEpisodes.stream()
                    .map(EpisodeBulkCache::from)
                    .toList();
        }

        return bulk.stream()
                .filter(cache -> cache.episodeNumber() == episodeNumber)
                .findFirst()
                .map(this::toDetailResponse)
                .orElseThrow(() -> new ServiceErrorException(EpisodeExceptionEnum.EPISODE_NOT_FOUND));
    }
}