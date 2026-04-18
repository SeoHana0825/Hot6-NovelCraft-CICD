package com.example.hot6novelcraft.domain.user.scheduler;

import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserScheduler {

    private final UserRepository userRepository;

    // 매일 00시에 실행
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processExpiredUsers() {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 30일 지난 "탈퇴 대기" 유저들 조회 (물리적 x)
        List<User> expiredUsers =
                userRepository.findByIsDeletedTrueAndDeletedAtBeforeAndAnonymizedAtIsNull (thirtyDaysAgo);

        if(expiredUsers.isEmpty()) {
            log.info("[회원탈퇴] 오늘 00시 완전 탈퇴 처리할 회원이 없습니다.");
            return;
        }

        // 조회해온 유저들을 하나씩 돌면서 마스킹 처리
        for(User user : expiredUsers) {
            user.anonymize();
            log.info("[회원탈퇴] 회원ID : {}, 비식별화 완료", user.getId());
        }
    }
}