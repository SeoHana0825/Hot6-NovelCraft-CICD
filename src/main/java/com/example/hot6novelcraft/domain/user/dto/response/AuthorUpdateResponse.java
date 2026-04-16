package com.example.hot6novelcraft.domain.user.dto.response;

import com.example.hot6novelcraft.domain.novel.entity.enums.MainGenre;
import com.example.hot6novelcraft.domain.user.entity.AuthorProfile;
import com.example.hot6novelcraft.domain.user.entity.enums.CareerLevel;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record AuthorUpdateResponse(

        Long authorProfileId,

        List<MainGenre> mainGenreList,

        String bio,

        CareerLevel careerLevel,

        String instagramLinks,

        String xLinks,

        String blogLinks,

        boolean allowMenteeRequest,

        LocalDateTime updated_At

) {
    public static AuthorUpdateResponse of(AuthorProfile profile) {
        return new AuthorUpdateResponse(
                profile.getId(),
                Arrays.stream(profile.getMainGenre().split(","))
                        .map(MainGenre::valueOf)
                        .toList(),
                profile.getBio(),
                profile.getCareerLevel(),
                profile.getInstagramLinks(),
                profile.getXLinks(),
                profile.getBlogLinks(),
                profile.isAllowMenteeRequest(),
                profile.getUpdatedAt()
        );
    }
}
