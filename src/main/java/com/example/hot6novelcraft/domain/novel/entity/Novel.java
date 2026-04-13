package com.example.hot6novelcraft.domain.novel.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "novels")
public class Novel extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 500)
    private String coverImageUrl;

    @Column(nullable = false, length = 50)
    private String genre;

    @Column(nullable = false, length = 500)
    private String tags;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NovelStatus status;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private int bookmarkCount = 0;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
        status = NovelStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public Novel(Long authorId, String title, String description,
                 String coverImageUrl, String genre, String tags) {
        this.authorId = authorId;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.genre = genre;
        this.tags = tags;
    }
}
