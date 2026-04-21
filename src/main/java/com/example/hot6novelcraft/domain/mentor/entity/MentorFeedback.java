package com.example.hot6novelcraft.domain.mentor.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mentorship_feedbacks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MentorFeedback extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long mentorshipId;

    @Column(nullable = false)
    private Long authorId;

    @Column(length = 200, nullable = false)
    private String title;           // 이동: Mentorship.title → MentorFeedback.title

    @Column(nullable = false)
    private int sessionNumber;      // 추가: 몇 회차 피드백인지

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private MentorFeedback(Long mentorshipId, Long authorId, String title,
                           int sessionNumber, String content) {
        this.mentorshipId  = mentorshipId;
        this.authorId      = authorId;
        this.title         = title;
        this.sessionNumber = sessionNumber;
        this.content       = content;
    }

    public static MentorFeedback create(Long mentorshipId, Long authorId, String title,
                                        int sessionNumber, String content) {
        return new MentorFeedback(mentorshipId, authorId, title, sessionNumber, content);
    }
}
