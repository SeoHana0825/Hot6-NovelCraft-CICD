package com.example.hot6novelcraft.domain.user.repository;

import com.example.hot6novelcraft.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository <User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByNickname(String nickname);

    Boolean existsByEmail(String email);
    Boolean existsByNickname(String nickname);

    Optional<User> findByIdAndIsDeletedFalse(Long id);

     // 닉네임 중복 확인 (본인 제외)
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    // 탈퇴 스케쥴러
    List<User> findByIsDeletedTrueAndDeletedAtBeforeAndAnonymizedAtIsNull(LocalDateTime date);

}
