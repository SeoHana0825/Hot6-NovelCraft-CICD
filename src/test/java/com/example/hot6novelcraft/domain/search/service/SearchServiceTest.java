package com.example.hot6novelcraft.domain.search.service;

import com.example.hot6novelcraft.domain.search.dto.IntegratedAuthorSearchResponse;
import com.example.hot6novelcraft.domain.search.dto.NovelSearchResponse;
import com.example.hot6novelcraft.domain.search.dto.TagGroupSearchResponse;
import com.example.hot6novelcraft.domain.search.repository.CustomSearchRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.search.dto.AuthorSearchResponse;
import com.example.hot6novelcraft.domain.search.dto.NovelSimpleResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SearchServiceTest {

    @InjectMocks
    private SearchService searchService;

    @Mock
    private CustomSearchRepository customSearchRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    // 테스트용 공통 변수
    private Pageable pageable;
    private UserDetailsImpl loggedInUser;
    private UserDetailsImpl anonymousUser;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 10);

        // 로그인 유저 Mock
        User mockUser = mock(User.class);
        given(mockUser.getId()).willReturn(1L);
        loggedInUser = mock(UserDetailsImpl.class);
        given(loggedInUser.getUser()).willReturn(mockUser);

        // 비로그인은 null
        anonymousUser = null;
    }

    // ========================
    // V1 테스트 - Redis 없이 DB 검색만
    // ========================
    @Nested
    @DisplayName("V1 - Redis 없이 DB 검색")
    class V1Test {

        @Test
        @DisplayName("V1 소설 제목 검색 - Redis 저장 없이 결과만 반환")
        void searchNovelsV1_success() {
            // given
            List<NovelSearchResponse> novels = List.of(
                    new NovelSearchResponse("cover.jpg", "바다가 보이는 카페", "바다작가", "HEALING"),
                    new NovelSearchResponse(null, "바다 위의 던전", "바다작가", "FANTASY")
            );
            Page<NovelSearchResponse> mockPage = new PageImpl<>(novels, pageable, 2);
            given(customSearchRepository.searchNovelsByTitle("바다", pageable)).willReturn(mockPage);

            // when
            Page<NovelSearchResponse> result = searchService.searchNovelsV1("바다", pageable);

            // then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).title()).isEqualTo("바다가 보이는 카페");

            // Redis 저장 안됐는지 검증
            verify(redisTemplate, never()).opsForList();
        }

        @Test
        @DisplayName("V1 태그 검색 - 태그별 그룹핑 반환, Redis 저장 없음")
        void searchByTagsV1_success() {
            // given
            List<String> tags = List.of("MUNCHKIN", "DUNGEON");
            List<TagGroupSearchResponse> mockResult = List.of(
                    new TagGroupSearchResponse("MUNCHKIN", List.of(
                            new NovelSimpleResponse("먼치킨 바다왕", "바다작가")
                    )),
                    new TagGroupSearchResponse("DUNGEON", List.of(
                            new NovelSimpleResponse("바다 위의 던전", "바다작가")
                    ))
            );
            given(customSearchRepository.searchNovelsByTags(tags)).willReturn(mockResult);

            // when
            List<TagGroupSearchResponse> result = searchService.searchByTagsV1(tags);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).tag()).isEqualTo("MUNCHKIN");
            assertThat(result.get(1).tag()).isEqualTo("DUNGEON");

            // Redis 저장 안됐는지 검증
            verify(redisTemplate, never()).opsForList();
        }

        @Test
        @DisplayName("V1 작가 검색 - 통합 결과 반환, Redis 저장 없음")
        void searchAuthorsV1_success() {
            // given
            IntegratedAuthorSearchResponse mockResult = new IntegratedAuthorSearchResponse(
                    List.of(new AuthorSearchResponse(1L, "백산", "판타지 작가", List.of())),
                    List.of(new NovelSimpleResponse("백산의 이세계 모험", "백산"))
            );
            given(customSearchRepository.searchByAuthorKeyword("백산")).willReturn(mockResult);

            // when
            IntegratedAuthorSearchResponse result = searchService.searchAuthorsV1("백산");

            // then
            assertThat(result.matchingAuthors()).hasSize(1);
            assertThat(result.matchingAuthors().get(0).nickname()).isEqualTo("백산");
            assertThat(result.matchingNovels()).hasSize(1);

            // Redis 저장 안됐는지 검증
            verify(redisTemplate, never()).opsForList();
        }
    }

    // ========================
    // V2 테스트 - Redis 검색어 저장 포함
    // ========================
    @Nested
    @DisplayName("V2 - Redis 검색어 저장 포함")
    class V2Test {

        @BeforeEach
        void setUpRedis() {
            // Redis ListOperations Mock 설정
            given(redisTemplate.opsForList()).willReturn(listOperations);
            given(redisTemplate.getExpire(anyString())).willReturn(-1L); // TTL 없는 상태
        }

        @Test
        @DisplayName("V2 소설 검색 - 로그인 시 Redis 저장과 Redis TTL 00시 설정")
        void searchNovelsV2_loggedIn_saveRedis() {
            // given
            Page<NovelSearchResponse> mockPage = new PageImpl<>(List.of(), pageable, 0);
            given(customSearchRepository.searchNovelsByTitle("바다", pageable)).willReturn(mockPage);

            // 테스트 실행 직전 자정까지 남은 시간 계산 (Service 로직과 동일)
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime midnight = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
            Duration expectedTTL = Duration.between(now, midnight);

            // when
            searchService.searchNovels("바다", pageable, loggedInUser);

            // then - Redis 저장 호출됐는지 검증
            verify(listOperations).remove(eq("search_history:1"), eq(0L), eq("바다"));
            verify(listOperations).leftPush(eq("search_history:1"), eq("바다"));
            verify(redisTemplate).expire(eq("search_history:1"), any());

            // TTL이 "자정까지 남은 시간"인지 검증 (±5초 오차 허용)
            verify(redisTemplate).expire(
                    eq("search_history:1"),
                    argThat(duration -> {
                        long diff = Math.abs(duration.getSeconds() - expectedTTL.getSeconds());
                        return diff <= 5; // 테스트 실행 시간 오차 5초 허용
                    })
            );
        }

        @Test
        @DisplayName("V2 소설 검색 - TTL이 00시간 이하여야함 / 비로그인 시 Redis 저장 안됨")
        void searchNovelsV2_anonymous_noRedis() {
            // given
            Page<NovelSearchResponse> mockPage = new PageImpl<>(List.of(), pageable, 0);
            given(customSearchRepository.searchNovelsByTitle("바다", pageable)).willReturn(mockPage);

            // when
            searchService.searchNovels("바다", pageable, anonymousUser);

            // then - 비로그인은 저장 안됨
            verify(redisTemplate, never()).opsForList();
            verify(redisTemplate, never()).expire(any(), any());
        }

        @Test
        @DisplayName("V2 태그 검색 - 로그인 시 태그 조합이 Redis에 저장됨")
        void searchByTagsV2_loggedIn_saveRedis() {
            // given
            List<String> tags = List.of("MUNCHKIN", "DUNGEON");
            given(customSearchRepository.searchNovelsByTags(tags)).willReturn(List.of());

            // when
            searchService.searchByTags(tags, loggedInUser);

            // then - "MUNCHKIN,DUNGEON" 형태로 저장됐는지 검증
            verify(listOperations).leftPush(eq("search_history:1"), eq("MUNCHKIN,DUNGEON"));
        }

        @Test
        @DisplayName("V2 작가 검색 - 로그인 시 Redis 저장됨")
        void searchAuthorsV2_loggedIn_saveRedis() {
            // given
            IntegratedAuthorSearchResponse mockResult = new IntegratedAuthorSearchResponse(
                    List.of(), List.of()
            );
            given(customSearchRepository.searchByAuthorKeyword("백산")).willReturn(mockResult);

            // when
            searchService.searchAuthors("백산", loggedInUser);

            // then
            verify(listOperations).leftPush(eq("search_history:1"), eq("백산"));
        }

        @Test
        @DisplayName("V2 검색 - 빈 키워드는 Redis 저장 안됨")
        void searchNovelsV2_emptyKeyword_noRedis() {
            // given
            Page<NovelSearchResponse> mockPage = new PageImpl<>(List.of(), pageable, 0);
            given(customSearchRepository.searchNovelsByTitle("", pageable)).willReturn(mockPage);

            // when
            searchService.searchNovels("", pageable, loggedInUser);

            // then
            verify(redisTemplate, never()).opsForList();
        }
    }
}
