package com.example.hot6novelcraft.common.config;

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

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // 이미 데이터 있으면 스킵 (중복 삽입 방지)
        if (userRepository.count() > 0) {
            log.info("[DataInitializer] 기존 데이터 존재 → 더미데이터 삽입 스킵");
            return;
        }

        log.info("[DataInitializer] 더미데이터 삽입 시작");

        // ========================
        // 유저 3명 생성
        // ========================
        User user1 = userRepository.save(User.register(
                "백산@test.com",
                passwordEncoder.encode("test1234!"),
                "백산",
                "010-1111-1111",
                null,
                UserRole.AUTHOR
        ));

        User user2 = userRepository.save(User.register(
                "백산아@test.com",
                passwordEncoder.encode("test1234!"),
                "백산아",
                "010-2222-2222",
                null,
                UserRole.AUTHOR
        ));

        User user3 = userRepository.save(User.register(
                "바다작가@test.com",
                passwordEncoder.encode("test1234!"),
                "바다작가",
                "010-3333-3333",
                null,
                UserRole.AUTHOR
        ));

        // ========================
        // 작가 프로필 3명 생성
        // ========================
        authorProfileRepository.save(AuthorProfile.register(
                user1.getId(),
                "판타지와 먼치킨을 주로 씁니다.",
                CareerLevel.INTERMEDIATE,
                "FANTASY",
                null, null, null,
                true
        ));

        authorProfileRepository.save(AuthorProfile.register(
                user2.getId(),
                "로맨스 전문 작가입니다.",
                CareerLevel.INTERMEDIATE,
                "ROMANCE",
                null, null, null,
                false
        ));

        authorProfileRepository.save(AuthorProfile.register(
                user3.getId(),
                "바다를 배경으로 한 힐링물을 씁니다.",
                CareerLevel.PROFICIENT,
                "HEALING",
                null, null, null,
                true
        ));

        // ========================
        // 소설 생성 (제목/태그 검색 테스트용)
        // ========================

        // user1(백산) 소설 - 제목 검색용
        saveNovel(user1.getId(), "백산의 이세계 모험", "이세계로 떨어진 백산의 이야기", "FANTASY", "ISEKAI,MUNCHKIN", 1500L);
        saveNovel(user1.getId(), "백산 회귀하다", "회귀한 백산의 복수극", "FANTASY", "REGRESSION,REVENGE", 3000L);
        saveNovel(user1.getId(), "백산 던전 공략기", "던전을 정복하는 먼치킨 백산", "FANTASY", "DUNGEON,MUNCHKIN,GROWTH", 800L);

        // user2(백산아) 소설
        saveNovel(user2.getId(), "백산아 너를 사랑해", "백산아와의 로맨스", "ROMANCE", "ROMANCE,CONTRACT", 2200L);
        saveNovel(user2.getId(), "계약 결혼의 비밀", "계약으로 시작된 사랑", "ROMANCE", "ROMANCE,CONTRACT,HEALING", 500L);

        // user3(바다작가) 소설 - 제목 + 태그 검색용
        saveNovel(user3.getId(), "바다가 보이는 카페", "힐링 바다 소설", "HEALING", "HEALING,ROMANCE", 4000L);
        saveNovel(user3.getId(), "바다 위의 던전", "바다를 배경으로 한 던전물", "FANTASY", "DUNGEON,ISEKAI", 1200L);
        saveNovel(user3.getId(), "먼치킨 바다왕", "바다를 지배하는 먼치킨 주인공", "FANTASY", "MUNCHKIN,DUNGEON,HAREM", 6000L);

        log.info("[DataInitializer] 더미데이터 삽입 완료 ✅");
    }

    // 소설 저장 헬퍼 메서드
    private void saveNovel(Long authorId, String title, String description, String genre, String tags, Long viewCount) {
        Novel novel = Novel.createNovel(authorId, title, description, genre, tags);
        Novel saved = novelRepository.save(novel);
        saved.changeStatus(NovelStatus.ONGOING);
        novelRepository.save(saved);
    }
}
