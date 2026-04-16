package com.example.hot6novelcraft.domain.user.dto.response;

import com.example.hot6novelcraft.domain.user.entity.AuthorProfile;
import com.example.hot6novelcraft.domain.user.entity.ReaderProfile;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;

public record MyPageResponse(
        Long uerId,
        String email,
        String nickname,
        UserRole role,
        String phoneNo,
        AuthorUpdateResponse authorProfile,
        ReaderUpdateResponse readerProfile

) {
    public static MyPageResponse ofAuthor(User user, AuthorProfile authorProfile) {
        return new MyPageResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getPhoneNo(),
                AuthorUpdateResponse.of(authorProfile),
                null
        );
    }
    public static MyPageResponse ofReader(User user, ReaderProfile readerProfile) {
        return new MyPageResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getPhoneNo(),
                null,
                ReaderUpdateResponse.of(readerProfile)
        );
    }
    public static MyPageResponse ofDefault(User user) {
        return new MyPageResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getPhoneNo(),
                null,
                null
        );
    }
}
