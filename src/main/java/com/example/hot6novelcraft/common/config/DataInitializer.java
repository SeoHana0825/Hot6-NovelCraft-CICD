package com.example.hot6novelcraft.common.config;

import com.example.hot6novelcraft.domain.episode.entity.Episode;
import com.example.hot6novelcraft.domain.episode.repository.EpisodeRepository;
import com.example.hot6novelcraft.domain.novel.entity.Novel;
import com.example.hot6novelcraft.domain.novel.entity.enums.NovelStatus;
import com.example.hot6novelcraft.domain.novel.repository.NovelRepository;
import com.example.hot6novelcraft.domain.user.entity.AuthorProfile;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.repository.AuthorProfileRepository;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"local", "dev", "test"})
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AuthorProfileRepository authorProfileRepository;
    private final NovelRepository novelRepository;
    private final PasswordEncoder passwordEncoder;

    // 랭킹 테스트 및 에피소드 생성을 위한 의존성 추가
    private final RedisTemplate<String, Object> redisTemplate;
    private final EpisodeRepository episodeRepository;

    // 직접 쿼리를 날리기 위해 JdbcTemplate 추가
    private final JdbcTemplate jdbcTemplate;

    private static final String REALTIME_RANKING_KEY = "ranking:novel:realtime";
    private static final String WEEKLY_RANKING_KEY = "ranking:novel:weekly";

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // 기존 데이터가 있으면 스킵 (에러 방지)
        if (userRepository.count() > 0
                || authorProfileRepository.count() > 0
                || novelRepository.count() > 0) {
            log.info("[DataInitializer] 기존 소설 데이터 존재 → 더미데이터 삽입 스킵");
            return;
        }

        log.info("[DataInitializer] 더미데이터 및 Redis 랭킹 세팅 시작");

        // 기존 Redis 랭킹 데이터 초기화
        redisTemplate.delete(REALTIME_RANKING_KEY);
        redisTemplate.delete(WEEKLY_RANKING_KEY);

        // ========================
        // 1. 유저 및 작가 프로필 생성
        // ========================
        User user1 = userRepository.save(User.register("백산@test.com", passwordEncoder.encode("test1234!"), "백산", "010-1111-1111", null, UserRole.AUTHOR));
        User user2 = userRepository.save(User.register("백산아@test.com", passwordEncoder.encode("test1234!"), "백산아", "010-2222-2222", null, UserRole.AUTHOR));
        User user3 = userRepository.save(User.register("바다작가@test.com", passwordEncoder.encode("test1234!"), "바다작가", "010-3333-3333", null, UserRole.AUTHOR));

        authorProfileRepository.save(AuthorProfile.register(user1.getId(), "판타지 전문", CareerLevel.INTERMEDIATE, "FANTASY", null, null, null, true));
        authorProfileRepository.save(AuthorProfile.register(user2.getId(), "로맨스 전문", CareerLevel.INTERMEDIATE, "ROMANCE", null, null, null, false));
        authorProfileRepository.save(AuthorProfile.register(user3.getId(), "힐링 전문", CareerLevel.PROFICIENT, "HEALING", null, null, null, true));

        // ========================
        // 2. 소설 생성 및 랭킹 세팅
        // ========================
        saveNovelAndRanking(user1.getId(), "먼치킨 백산의 귀환", "압도적 1위 테스트", "FANTASY", "MUNCHKIN,ISEKAI", 10000, 50000);
        saveNovelAndRanking(user2.getId(), "재벌집 로맨스", "로맨스 강자 테스트", "ROMANCE", "ROMANCE,CONTRACT", 8000, 45000);
        saveNovelAndRanking(user3.getId(), "바다가 보이는 카페", "갑자기 어제부터 뜬 소설", "HEALING", "HEALING,ROMANCE", 7500, 10000);
        saveNovelAndRanking(user1.getId(), "이세계 던전 공략", "꾸준한 인기 소설", "FANTASY", "DUNGEON,GROWTH", 2000, 40000);
        saveNovelAndRanking(user3.getId(), "심해의 군주", "바다물 판타지", "FANTASY", "DUNGEON,MUNCHKIN", 5000, 30000);
        saveNovelAndRanking(user2.getId(), "비밀스러운 계약", "잔잔한 로맨스", "ROMANCE", "ROMANCE,HEALING", 1500, 8000);

        log.info("[DataInitializer] 더미데이터 및 Redis 랭킹 세팅 완료 ✅");


        // ========================
        // 3. 신작 조회 테스트용 날짜 강제 조작 (JPA Auditing 무시하고 직접 SQL 실행)
        // ========================
        log.info("[DataInitializer] 테스트용 소설 생성일자 조작 시작...");

        // 정상 노출 데이터 (1달 이내)
        jdbcTemplate.execute("UPDATE novels SET created_at = DATE_SUB(NOW(), INTERVAL 1 DAY), status = 'ONGOING', is_deleted = false WHERE id = 1");
        jdbcTemplate.execute("UPDATE novels SET created_at = DATE_SUB(NOW(), INTERVAL 15 DAY), status = 'ONGOING', is_deleted = false WHERE id = 2");
        jdbcTemplate.execute("UPDATE novels SET created_at = DATE_SUB(NOW(), INTERVAL 29 DAY), status = 'ONGOING', is_deleted = false WHERE id = 3");

        // 기간 초과 데이터 (1달 초과 - 리스트에 안 나와야 함)
        jdbcTemplate.execute("UPDATE novels SET created_at = DATE_SUB(NOW(), INTERVAL 2 MONTH), status = 'ONGOING', is_deleted = false WHERE id = 4");
        jdbcTemplate.execute("UPDATE novels SET created_at = DATE_SUB(NOW(), INTERVAL 6 MONTH), status = 'ONGOING', is_deleted = false WHERE id = 5");

        // 상태 불만족 데이터
        jdbcTemplate.execute("UPDATE novels SET created_at = NOW(), status = 'PENDING', is_deleted = false WHERE id = 6");
        jdbcTemplate.execute("UPDATE novels SET created_at = NOW(), status = 'ONGOING', is_deleted = true WHERE id = 7");

        log.info("[DataInitializer] 더미데이터 및 Redis 랭킹 세팅 완료 ✅");
    }

    /**
     * 소설 DB 저장 및 Redis ZSET 랭킹 반영 + 에피소드(1화) 자동 생성 헬퍼 메서드
     */
    private void saveNovelAndRanking(Long authorId, String title, String description, String genre, String tags,
                                     double realtimeScore, double weeklyScore) {

        // 1. DB에 소설 저장
        Novel novel = Novel.createNovel(authorId, title, description, genre, tags);
        Novel saved = novelRepository.save(novel);
        saved.changeStatus(NovelStatus.ONGOING);
        novelRepository.save(saved);

        // 2. 소설이 만들어지면 무조건 1화를 같이 생성하고 발행!
        Episode episode = Episode.createEpisode(
                saved.getId(),   // novelId
                1,               // 1화
                title + " - 1화", // 에피소드 제목
                "이것은 테스트를 위한 " + title + "의 1화 본문입니다. 여기서 어뷰징 방지 및 조회수 로직이 작동합니다.", // 본문
                true,            // 무료 여부
                0                // 포인트 가격
        );

        episode.publish(); // 발행 상태로 변경해야 API로 조회 가능
        episodeRepository.save(episode);

        // 3. Redis ZSET에 랭킹 스코어(조회수) 등록
        redisTemplate.opsForZSet().incrementScore(REALTIME_RANKING_KEY, String.valueOf(saved.getId()), realtimeScore);
        redisTemplate.opsForZSet().incrementScore(WEEKLY_RANKING_KEY, String.valueOf(saved.getId()), weeklyScore);
    }
}