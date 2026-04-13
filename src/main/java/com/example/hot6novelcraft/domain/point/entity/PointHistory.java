package com.example.hot6novelcraft.domain.point.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "point_histories")
public class PointHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long novelId;

    @Column(nullable = false)
    private Long episodeId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private PointHistoryType type;

    private String description;
}
