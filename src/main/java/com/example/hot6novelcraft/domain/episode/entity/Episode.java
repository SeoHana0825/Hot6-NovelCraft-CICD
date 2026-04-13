package com.example.hot6novelcraft.domain.episode.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "episodes")
public class Episode extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long novelId;

    @Column(nullable = false)
    private int episodeNumber;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean isFree = false;

    @Column(nullable = false)
    private int pointPrice = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EpisodeStatus status;

    private LocalDateTime publishedAt;  // 발행 일시

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private Long likeCount;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        status = EpisodeStatus.DRAFT;
        likeCount = 0L;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Episode(Long novelId, int episodeNumber, String title,
                   String content, boolean isFree, int pointPrice) {
        this.novelId = novelId;
        this.episodeNumber = episodeNumber;
        this.title = title;
        this.content = content;
        this.isFree = isFree;
        this.pointPrice = pointPrice;
    }


}
