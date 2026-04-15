package com.example.hot6novelcraft.domain.user.dto.request;

import com.example.hot6novelcraft.domain.novel.entity.MainGenre;
import com.example.hot6novelcraft.domain.user.entity.userEnum.CareerLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.stream.Collectors;

public record AuthorSignupRequest(

    @NotEmpty(message = "주력 장르 선택은 필수입니다.")
    @Size(min = 1, message = "주력 장르는 최소 1개 이상 선택해야합니다.")
    List<MainGenre> genres,

    @NotBlank
    @Size(min = 10, max = 500, message = "작가소개는 500자 이내로 작성 가능합니다.")
    String bio,

    @NotNull
    @Size(max = 1, message = "작가 경력사항은 필수 사항입니다.")
    CareerLevel careerLevel,

    String instagramLinks,

    String xLinks,

    String blogLinks,

    @NotNull(message = "멘티 요청 허용 여부는 필수입니다.")
    Boolean allowMenteeRequest

){
    // 주력 장르 변환
    public String mainGenreToString() {
        if (genres == null || genres.isEmpty())
            return null;
        return genres.stream()
                .map(MainGenre::toString)
                .collect(Collectors.joining(","));
    }
}
