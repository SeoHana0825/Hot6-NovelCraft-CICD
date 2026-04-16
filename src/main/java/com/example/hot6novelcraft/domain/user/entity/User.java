package com.example.hot6novelcraft.domain.user.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = true, length = 20)
    private String phoneNo;

    @Column(nullable = true)
    private LocalDate birthday;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    private String refreshToken;

    private boolean isDeleted;

    private LocalDateTime deletedAt;

    private LocalDateTime updatedAt;

    @PreUpdate
    protected void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @PreRemove
    protected void preRemove() {
        deletedAt = LocalDateTime.now();
    }

    public static User register(String email, String password, String nickname, String phoneNo, LocalDate birthday, UserRole role) {
        return User.builder()
                .email(email)
                .password(password)
                .nickname(nickname)
                .phoneNo(phoneNo)
                .birthday(birthday)
                .role(role)
                .build();
    }

    // 관리자 전용 메서드
    public static User registerAdmin(String email, String password, String phoneNo, UserRole role) {
        return User.register(
                email,
                password,
                "ADMIN_" + email,
                phoneNo,
                null,
                UserRole.ADMIN
        );
    }

    // 소셜 로그인 빌드
    public static User socialUser(String email, String nickname, UserRole role) {
        return User.builder()
                .email(email)
                .nickname(nickname)
                .role(role)
                .password("SOCIAL_LOGIN")
                .birthday(null)
                .phoneNo(null)
                .build();
    }

    public void updateForSocialSignup(String nickname, String phoneNo, LocalDate birthday) {
        this.nickname = nickname;
        this.phoneNo = phoneNo;
        this.birthday = birthday;
        this.password = "SOCIAL_LOGIN";
        this.role=UserRole.TEMP;
    }

    // 회원정보 수정
    public void update(String nickname, String phoneNo) {
        this.nickname = nickname;
        this.phoneNo = phoneNo;
    }

    // 비밀번호 수정
    public void updatePassword(String password) {
        this.password = password;
    }

    // 회원 탈퇴
    public void withdraw() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    // TODO 재가입 시 계정 재활성화

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void changeRole(UserRole role) {
        this.role = role;
    }
}
