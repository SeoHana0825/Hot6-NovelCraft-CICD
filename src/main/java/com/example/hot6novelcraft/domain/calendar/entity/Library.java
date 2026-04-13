package com.example.hot6novelcraft.domain.calendar.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "library") // ERD의 테이블명 반영
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Library extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // 회원 아이디 직접 참조

    @Column(nullable = false)
    private Long novelId; // 소설 아이디 직접 참조

    private Library(Long userId, Long novelId) {
        this.userId = userId;
        this.novelId = novelId;
    }

    public static Library create(Long userId, Long novelId) {
        return new Library(userId, novelId);
    }
}
