package com.example.hot6novelcraft.domain.mentoring.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import com.example.hot6novelcraft.domain.mentorship.entity.MentorshipStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mentorships")
public class Mentorship extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mentorId;

    @Column(nullable = false)
    private Long menteeId;

    private Long currentNovelId;

    @Column(length = 500)
    private String motivation;

    @Column(length = 500)
    private String manuscriptUrl;  // S3

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MentorshipStatus status;

    private LocalDateTime acceptedAt; // 수락일
    private LocalDateTime completedAt; // 완료일 (멘토링이 끝나는날)

    @PrePersist
    protected void onCreate() {
        status = MentorshipStatus.PENDING;
    }

    @Builder
    public Mentorship(Long mentorId, Long menteeId, Long currentNovelId,
                      String motivation, String manuscriptUrl) {
        this.mentorId = mentorId;
        this.menteeId = menteeId;
        this.currentNovelId = currentNovelId;
        this.motivation = motivation;
        this.manuscriptUrl = manuscriptUrl;
    }
}
