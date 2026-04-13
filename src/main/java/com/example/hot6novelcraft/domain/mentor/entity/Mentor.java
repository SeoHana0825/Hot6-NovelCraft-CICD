package com.example.hot6novelcraft.domain.mentor.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import com.example.hot6novelcraft.domain.user.entity.CareerLevel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mentors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mentor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId; // 회원 아이디 (연관관계 없이 ID 참조)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CareerLevel careerLevel; // BEGINNER, INTERMEDIATE, EXPERT

    private String mainGenres; // 주력 장르 (JSON)

    private String mentoringStyle;

    @Column(length = 500)
    private String bio;

    @Column(length = 500)
    private String awardsCareer;

    @Column(nullable = false)
    private Integer maxMentees;

    @Column(nullable = false)
    private Boolean allowInstant;

    @Column(length = 500)
    private String preferredMenteeDesc;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MentorStatus status; // PENDING, APPROVED, REJECTED

    private Mentor(Long userId, CareerLevel careerLevel, String mainGenres, String mentoringStyle,
                   String bio, String awardsCareer, Integer maxMentees, Boolean allowInstant,
                   String preferredMenteeDesc, MentorStatus status) {
        this.userId = userId;
        this.careerLevel = careerLevel;
        this.mainGenres = mainGenres;
        this.mentoringStyle = mentoringStyle;
        this.bio = bio;
        this.awardsCareer = awardsCareer;
        this.maxMentees = maxMentees;
        this.allowInstant = allowInstant;
        this.preferredMenteeDesc = preferredMenteeDesc;
        this.status = status;
    }

    // 정적 팩토리 패턴
    public static Mentor create(Long userId, CareerLevel careerLevel, String mainGenres,
                                Integer maxMentees, Boolean allowInstant) {
        return new Mentor(userId, careerLevel, mainGenres, null, null, null,
                maxMentees, allowInstant, null, MentorStatus.PENDING);
    }
}
