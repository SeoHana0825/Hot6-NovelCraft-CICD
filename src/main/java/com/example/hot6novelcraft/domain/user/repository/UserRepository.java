package com.example.hot6novelcraft.domain.user.repository;

import com.example.hot6novelcraft.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository <User, Long> {

    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByNickname(String nickname);
    Optional<User> findByIdAndIsDeletedFalse(Long id);

    // 탈퇴 유저 원본 이메일로 조회
    @Query("SELECT u FROM User u WHERE u.email LIKE concat(:email, '_Deleted_%')And u.isDeleted = true")
            Optional<User> findDeletedUserByOriginalEmail(@Param("email") String email);

    // 닉네임 중복 확인 (본인 제외)
    boolean existsByNicknameAndIdNot(String nickname, Long id);

    // 재가입 시 중복 방지 (활성 유저만 진행)
    boolean existsByEmailAndIsDeletedFalse(String email);
}
