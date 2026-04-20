package com.example.hot6novelcraft.domain.mentoring.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import com.example.hot6novelcraft.domain.mentoring.entity.enums.MentorshipStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "mentorships")
public class Mentorship extends BaseEntity {
    private LocalDateTime rejectedAt;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mentorId;

    @Column(nullable = false)
    private Long menteeId;

    @Column(nullable = false)
    private Long currentNovelId;

    @Column(length = 500)
    private String motivation;

    @Column(length = 500)
    private String manuscriptUrl;

    @Column(length = 200)
    private String title;

    @Column(nullable = false)
    private int totalSessions = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MentorshipStatus status;

    @Column(nullable = false)
    private int manuscriptDownloadCount = 0;

    public void increaseManuscriptDownloadCount() {
        this.manuscriptDownloadCount++;
    }

    private LocalDateTime acceptedAt;
    private LocalDateTime completedAt;

    public static Mentorship create(Long mentorId, Long menteeId, Long currentNovelId,
                                    String motivation, String manuscriptUrl, String title) {
        Mentorship mentorship = new Mentorship();
        mentorship.mentorId = mentorId;
        mentorship.menteeId = menteeId;
        mentorship.currentNovelId = currentNovelId;
        mentorship.title = title;
        mentorship.motivation = motivation;
        mentorship.manuscriptUrl = manuscriptUrl;
        mentorship.status = MentorshipStatus.PENDING;
        mentorship.totalSessions = 0;
        return mentorship;
    }

    public void approve() {
        this.status = MentorshipStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = MentorshipStatus.REJECTED;
        this.rejectedAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = MentorshipStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void increaseSession() {
        this.totalSessions++;
    }
}