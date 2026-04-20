package com.example.hot6novelcraft.domain.novel.service;

import com.example.hot6novelcraft.domain.episode.service.EpisodeCacheService;
import com.example.hot6novelcraft.domain.novel.dto.response.NovelRankingResponse;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.entity.enums.MainGenre;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class NovelRankingIntegrationTest {

    @Autowired
    private EpisodeCacheService episodeCacheService;

    @Autowired
    private NovelRankingService novelRankingService;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private User dummyAuthor;
    private Novel novelA, novelB, novelC;

    @BeforeEach
    void setUp() {
        // 테스트 시작 전 Redis 도화지 초기화
        clearRedis();

        // 소설을 쓰기 위한 가짜 '작가' 유저를 먼저 DB에 저장합니다!
        User authorUser = User.builder()
                .email("author@test.com")
                .password("password123!")
                .nickname("테스트작가")
                .role(UserRole.AUTHOR)
                .build();
        dummyAuthor = userRepository.save(authorUser); // DB에 저장!

        // DB에 테스트용 더미 소설 3개 저장 (엔티티 필수값에 맞춰 수정 필요할 수 있음)
        novelA = novelRepository.save(createDummyNovel("테스트 소설 A", dummyAuthor, MainGenre.FANTASY));
        novelB = novelRepository.save(createDummyNovel("테스트 소설 B", dummyAuthor, MainGenre.HORROR));
        novelC = novelRepository.save(createDummyNovel("테스트 소설 C", dummyAuthor, MainGenre.CLASSIC));
    }

    @AfterEach
    void tearDown() {
        // 테스트 종료 후 다시 한번 Redis 청소
        clearRedis();
    }

    private void clearRedis() {
        redisTemplate.delete("ranking:novel:realtime");
        redisTemplate.delete("ranking:novel:weekly");
    }

    @Test
    @DisplayName("소설 랭킹 통합 테스트: 조회수가 높은 순서대로 실시간 및 주간 랭킹 TOP을 반환한다.")
    void novelRanking_IntegrationTest() {
        // given: 소설들의 랭킹 점수(조회수) 증가 (어뷰징 통과했다고 가정하고 메서드 직접 호출)

        // 소설A: 1번 읽힘 (3등)
        episodeCacheService.increaseRankingScore(novelA.getId());

        // 소설B: 3번 읽힘 (1등)
        episodeCacheService.increaseRankingScore(novelB.getId());
        episodeCacheService.increaseRankingScore(novelB.getId());
        episodeCacheService.increaseRankingScore(novelB.getId());

        // 소설C: 2번 읽힘 (2등)
        episodeCacheService.increaseRankingScore(novelC.getId());
        episodeCacheService.increaseRankingScore(novelC.getId());

        // when: 실시간 랭킹과 주간 랭킹 조회 API 로직 실행
        List<NovelRankingResponse> realtimeRanking = novelRankingService.getNovelRanking("realtime");
        List<NovelRankingResponse> weeklyRanking = novelRankingService.getNovelRanking("weekly");

        // then: 랭킹은 점수가 높은 B -> C -> A 순서여야 한다! (총 3개)

        // 1. 실시간 랭킹 검증
        assertThat(realtimeRanking).hasSize(3);
        assertThat(realtimeRanking.get(0).novelId()).isEqualTo(novelB.getId()); // 1등은 B
        assertThat(realtimeRanking.get(1).novelId()).isEqualTo(novelC.getId()); // 2등은 C
        assertThat(realtimeRanking.get(2).novelId()).isEqualTo(novelA.getId()); // 3등은 A

        // 2. 주간 랭킹 검증
        assertThat(weeklyRanking).hasSize(3);
        assertThat(weeklyRanking.get(0).novelId()).isEqualTo(novelB.getId());
        assertThat(weeklyRanking.get(1).novelId()).isEqualTo(novelC.getId());
        assertThat(weeklyRanking.get(2).novelId()).isEqualTo(novelA.getId());
    }

    /**
     * 테스트용 가짜 소설을 생성하는 헬퍼 메서드
     * (프로젝트의 Novel 엔티티 필수값 조건에 맞게 적절히 채워주세요!)
     */
    private Novel createDummyNovel(String title, User author, MainGenre genre) {
        return Novel.builder()
                .title(title)
                .description("테스트용 소설 소개글입니다.")
                .authorId(author.getId())
                .genre(genre.name())
                .tags("테스트,먼치킨,회귀")
                .build();
    }
}
