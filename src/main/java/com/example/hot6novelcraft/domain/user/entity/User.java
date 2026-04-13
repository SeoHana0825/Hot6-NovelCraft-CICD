package com.example.hot6novelcraft.domain.user.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false, unique = true, length = 20)
    private String phoneNo;

    @Column(nullable = false)
    private LocalDateTime birthday;

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

    private User (String email, String password, String nickname, String phoneNo, LocalDateTime birthday, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.phoneNo = phoneNo;
        this.birthday = birthday;
        this.role = role;
    }

    @Builder
    public static User register (String email, String password, String nickname, String phoneNo, LocalDateTime birthday, UserRole role) {
        return new User (email, password, nickname, phoneNo, birthday, role);
    }
}
