package com.example.hot6novelcraft.domain.user.entity;

import com.example.hot6novelcraft.common.entity.BaseEntity;
import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.domain.user.entity.userEnum.CareerLevel;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "author_profiles")
public class AuthorProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 500)
    private String bio;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CareerLevel careerLevel;

    @Column(nullable = false)
    private String mainGenre;

    @Column(length = 500)
    private String instagramLinks;

    @Column(length = 500)
    private String xLinks;

    @Column(length = 500)
    private String blogLinks;

    @Column(nullable = false)
    private boolean allowMenteeRequest;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    private AuthorProfile(
            Long userId
            , String bio
            , CareerLevel careerLevel
            , String mainGenre
            , String instagramLinks
            , String xLinks
            , String blogLinks
            , boolean allowMenteeRequest
    ) {
        this.userId = userId;
        this.bio = bio;
        this.careerLevel = careerLevel;
        this.mainGenre = mainGenre;
        this.instagramLinks = instagramLinks;
        this.xLinks = xLinks;
        this.blogLinks = blogLinks;
        this.allowMenteeRequest = allowMenteeRequest;
    }

    @Builder
    public static AuthorProfile register(
            Long userId
            , String bio
            , CareerLevel careerLevel
            , String mainGenre
            , String instagramLinks
            , String xLinks
            , String blogLinks
            , boolean allowMenteeRequest
    ){
        return new AuthorProfile(userId, bio, careerLevel, mainGenre, instagramLinks, xLinks, blogLinks, allowMenteeRequest);
    }
}
