package com.example.hot6novelcraft.domain.novel.entity;

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
@Table(name = "novel_wiki",
        uniqueConstraints = @UniqueConstraint(columnNames = {"novel_id", "category"}))
public class NovelWiki extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long novelId;

    @Enumerated(EnumType.STRING)
    private WikiCategory category;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Builder
    public NovelWiki(Long novelId, WikiCategory category, String content) {
        this.novelId = novelId;
        this.category = category;
        this.content = content;
    }
}
