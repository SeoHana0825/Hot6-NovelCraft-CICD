package com.example.hot6novelcraft.domain.Exchange.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "revenues")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Revenue extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId; // 작가 아이디 (memberId 대신 ERD 명칭 반영)

    private Long episodeId; // 회차 아이디 (NULL 가능)

    @Column(nullable = false)
    private Integer amount; // 금액

    private Integer balance; // 거래 후 잔액

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RevenueType type; // EPISODE_SALE, SUBSCRIPTION, WITHDRAWAL

    private Revenue(Long authorId, Long episodeId, Integer amount, Integer balance, RevenueType type) {
        this.authorId = authorId;
        this.episodeId = episodeId;
        this.amount = amount;
        this.balance = balance;
        this.type = type;
    }

    // 정적 팩토리 메서드
    public static Revenue create(Long authorId, Long episodeId, Integer amount, Integer balance, RevenueType type) {
        return new Revenue(authorId, episodeId, amount, balance, type);
    }
}