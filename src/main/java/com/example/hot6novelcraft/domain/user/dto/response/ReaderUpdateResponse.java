package com.example.hot6novelcraft.domain.user.dto.response;

import com.example.hot6novelcraft.domain.novel.entity.enums.MainGenre;
import com.example.hot6novelcraft.domain.user.entity.ReaderProfile;
import com.example.hot6novelcraft.domain.user.entity.enums.ReadingGoal;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record ReaderUpdateResponse(
        Long readerProfileId,

        List<MainGenre> preferredGenres,

        ReadingGoal readingGoal,

        LocalDateTime updated_At

) {
    public static ReaderUpdateResponse of(ReaderProfile profile) {
        return new ReaderUpdateResponse(
                profile.getId(),
                Arrays.stream(profile.getPreferredGenres().split(","))
                        .map(MainGenre::valueOf)
                        .toList(),
                profile.getReadingGoal(),
                profile.getUpdatedAt()
        );
    }
}
