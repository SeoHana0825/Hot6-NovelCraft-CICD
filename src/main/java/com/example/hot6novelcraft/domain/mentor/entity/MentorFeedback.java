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
    private Long mentorshipId; // 멘토링 아이디 참조

    @Column(nullable = false)
    private Long authorId; // 작성자 회원 아이디

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 피드백 내용

    private MentorFeedback(Long mentorshipId, Long authorId, String content) {
        this.mentorshipId = mentorshipId;
        this.authorId = authorId;
        this.content = content;
    }

    public static MentorFeedback create(Long mentorshipId, Long authorId, String content) {
        return new MentorFeedback(mentorshipId, authorId, content);
    }
}
