package com.example.hot6novelcraft.domain.search.service;

import com.example.hot6novelcraft.domain.search.dto.IntegratedAuthorSearchResponse;
import com.example.hot6novelcraft.domain.search.dto.NovelSearchResponse;
import com.example.hot6novelcraft.domain.search.dto.TagGroupSearchResponse;
import com.example.hot6novelcraft.domain.search.repository.CustomSearchRepository;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final CustomSearchRepository customSearchRepository;
    private final StringRedisTemplate redisTemplate;

    /** ============ V1 ============
     1. 제목(소설) 검색
     2. 태그 검색
     3. 작가 통합 검색
     =================================== */
    public Page<NovelSearchResponse> searchNovelsV1(String keyword, Pageable pageable) {
        return customSearchRepository.searchNovelsByTitle(keyword, pageable);
    }

    public List<TagGroupSearchResponse> searchByTagsV1(List<String> tags) {
        return customSearchRepository.searchNovelsByTags(tags);
    }

    public IntegratedAuthorSearchResponse searchAuthorsV1(String keyword) {
        return customSearchRepository.searchByAuthorKeyword(keyword);
    }

    /** ============ V2 ============
     1. 제목(소설) 검색
     2. 태그 검색
     3. 작가 통합 검색
        - 비로그인 가능
        - 로그인 시 검색어 저장
     =================================== */
    public Page<NovelSearchResponse> searchNovels(String keyword, Pageable pageable, UserDetailsImpl userDetails) {
        saveSearchHistoryIfLoggedIn(keyword, userDetails);
        return customSearchRepository.searchNovelsByTitle(keyword, pageable);
    }

    public List<TagGroupSearchResponse> searchByTags(List<String> tags, UserDetailsImpl userDetails) {
        String keyword = String.join(",", tags);
        saveSearchHistoryIfLoggedIn(keyword, userDetails);
        return customSearchRepository.searchNovelsByTags(tags);
    }

    public IntegratedAuthorSearchResponse searchAuthors(String keyword, UserDetailsImpl userDetails) {
        saveSearchHistoryIfLoggedIn(keyword, userDetails);
        return customSearchRepository.searchByAuthorKeyword(keyword);
    }

    /** ===============================
     로그인 사용자 검색어 Redis 저장
        - 00시 리셋
     =================================== */
    private void saveSearchHistoryIfLoggedIn(String keyword, UserDetailsImpl userDetails) {
        if(userDetails != null && keyword != null && !keyword.trim().isEmpty()) {
            Long userId = userDetails.getUser().getId();
            String redisKey = "search_history:" + userId;

            try {
                redisTemplate.opsForList().remove(redisKey, 0, keyword); // 기존 중복 검색어 제거
                redisTemplate.opsForList().leftPush(redisKey, keyword); // 최신 검색어 맨 앞으로

                // 해당 키의 TTL이 설정되어 있지 않다면, 금일 00시까지 만료 시간 설정
                Long expire = redisTemplate.getExpire(redisKey);
                if(expire == null || expire <= 0) {
                    LocalDateTime midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
                    Duration duration = Duration.between(LocalDateTime.now(), midnight);
                    redisTemplate.expire(redisKey, duration);
                }
            } catch (RuntimeException e) {
                log.warn("[Redis 장애] 검색 저장을 건너뜁니다.");
            }

        }
    }

}
