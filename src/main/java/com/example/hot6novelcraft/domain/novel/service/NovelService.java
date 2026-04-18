package com.example.hot6novelcraft.domain.novel.service;

import com.example.hot6novelcraft.common.dto.PageResponse;
import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.NovelExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelCreateRequest;
import com.example.hot6novelcraft.domain.novel.dto.request.NovelUpdateRequest;
import com.example.hot6novelcraft.domain.novel.dto.response.*;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String NOVEL_LIST_CACHE_KEY = "novel_list::";
    private static final Duration NOVEL_LIST_CACHE_TTL = Duration.ofMinutes(30);

    // 소설 등록
    @Transactional
    public NovelCreateResponse createNovel(NovelCreateRequest request, UserDetailsImpl userDetails) {

        User user = userDetails.getUser();

        // 작가 권한 확인
        if (user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        Novel novel = Novel.createNovel(
                user.getId(),
                request.title(),
                request.description(),
                request.genre().toString(),
                request.tagsToString()
        );

        // DB 저장
        Novel savedNovel = novelRepository.save(novel);

        // 캐시 무효화
        evictNovelListCache();

        return NovelCreateResponse.from(savedNovel.getId());
    }

    // 소설 수정
    @Transactional
    public NovelUpdateResponse updateNovel(Long novelId, NovelUpdateRequest request, UserDetailsImpl userDetails) {

        User user = userDetails.getUser();

        // 작가 권한 확인
        if (user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        // 소설 조회 공통 메서드(본인 소설 및 삭제여부)
        Novel novel = findNovelById(novelId, user.getId());


        // 소설 수정
        novel.update(
                request.title(),
                request.description(),
                request.genre().toString(),
                request.tagsToString()
        );

        // 캐시 무효화
        evictNovelListCache();

        return NovelUpdateResponse.from(novel.getId());
    }

    // 소설 삭제
    @Transactional
    public NovelDeleteResponse deleteNovel(Long novelId, UserDetailsImpl userDetails) {

        User user = userDetails.getUser();

        // 작가 권한 확인
        if (user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        // 소설 조회 공통 메서드(본인 소설 및 삭제여부)
        Novel novel = findNovelById(novelId, user.getId());

        // 소설 삭제(소프트 딜리트)
        novel.delete();

        // 캐시 무효화
        evictNovelListCache();

        return NovelDeleteResponse.from(novel.getId());
    }

    // 소설 목록 조회 V1(JPA)
    @Transactional(readOnly = true)
    public PageResponse<NovelListResponse> getNovelListV1(Pageable pageable) {

        Page<Novel> novels = novelRepository.findAllByIsDeletedFalse(pageable);

        Page<NovelListResponse> response = novels.map(novel -> {
            // N+1 문제 발생 가능 - V2에서 QueryDSL로 개선 예정
            String authorNickname = userRepository.findById(novel.getAuthorId())
                    .map(user -> user.getNickname())
                    .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

            return NovelListResponse.of(
                    novel.getId(),
                    novel.getTitle(),
                    novel.getGenre(),
                    novel.getTags(),
                    novel.getStatus(),
                    novel.getCoverImageUrl(),
                    novel.getViewCount(),
                    novel.getBookmarkCount(),
                    authorNickname
            );
        });
        return PageResponse.register(response);
    }

    // 소설 목록 조회 V2(QueryDSL+인덱싱+Redis캐싱)
    @Transactional(readOnly = true)
    public PageResponse<NovelListResponse> getNovelListV2(String genre, NovelStatus status, Pageable pageable) {

        // 캐시 키 생성
        String cacheKey = NOVEL_LIST_CACHE_KEY
                + "genre:" + genre + "::"
                + "status:" + status + "::"
                + "page:" + pageable.getPageNumber() + "::"
                + "size:" + pageable.getPageSize();

        // Redis 캐시 확인
        Object cached = null;
        try {
            cached = redisTemplate.opsForValue().get(cacheKey);
        } catch (RuntimeException e) {
            log.warn("Novel list cache read failed. key={}", cacheKey, e);
        }

        if (cached != null) {
            log.debug("===== [캐시 HIT] key={} =====", cacheKey);
            return (PageResponse<NovelListResponse>) cached;
        }

        log.debug("===== [캐시 MISS] key={} DB 조회 =====", cacheKey);

        // DB 조회 (QueryDSL)
        Page<NovelListResponse> novels = novelRepository.findNovelListV2(genre, status, pageable);
        PageResponse<NovelListResponse> response = PageResponse.register(novels);

        // Redis 캐싱
        try {
            redisTemplate.opsForValue().set(cacheKey, response, NOVEL_LIST_CACHE_TTL);
            log.debug("===== [캐시 저장] key={} TTL=30분 =====", cacheKey);
        } catch (RuntimeException e) {
            log.warn("Novel list cache write failed. key={}", cacheKey, e);
        }
        return response;
    }


    // 소설 상세 조회 (QueryDSL + 인덱싱)
    @Transactional(readOnly = true)
    public NovelDetailResponse getNovelDetail(Long novelId) {

        NovelDetailResponse response = novelRepository.findNovelDetailByNovelId(novelId);

        if (response == null) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_NOT_FOUND);
        }
        return response;
    }

    // 작가용 소설 목록 조회 (에디터용)
    @Transactional(readOnly = true)
    public PageResponse<AuthorNovelListResponse> getAuthorNovelList(UserDetailsImpl userDetails, Pageable pageable) {

        // 작가 권한 확인
        validateAuthorRole(userDetails);

        Long authorId = userDetails.getUser().getId();

        // 본인 소설 목록 조회 (전체 상태 포함)
        Page<AuthorNovelListResponse> novels = novelRepository.findAuthorNovelList(authorId, pageable);

        return PageResponse.register(novels);
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

    // 작가 권한 확인 공통 메서드
    private void validateAuthorRole(UserDetailsImpl userDetails) {
        if (userDetails.getUser().getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_FORBIDDEN);
        }
    }

    // 캐시 무효화 공통 메서드(스캔방식)
    private void evictNovelListCache() {
        try {
            ScanOptions options = ScanOptions.scanOptions()
                    .match(NOVEL_LIST_CACHE_KEY + "*")
                    .count(100)
                    .build();
            try (Cursor<String> cursor = redisTemplate.scan(options)) {
                while (cursor.hasNext()) {
                    redisTemplate.delete(cursor.next());
                }
            }
        } catch (RuntimeException e) {
            log.warn("Novel list cache eviction failed.", e);
        }
    }
}