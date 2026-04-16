package com.example.hot6novelcraft.domain.user.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.NovelExceptionEnum;
import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.common.security.JwtUtil;
import com.example.hot6novelcraft.common.security.RedisUtil;
import com.example.hot6novelcraft.domain.user.dto.request.AuthorRequest;
import com.example.hot6novelcraft.domain.user.dto.request.CommonUpdateRequest;
import com.example.hot6novelcraft.domain.user.dto.request.LoginUserRequest;
import com.example.hot6novelcraft.domain.user.dto.request.ReaderUpdatedRequest;
import com.example.hot6novelcraft.domain.user.dto.response.*;
import com.example.hot6novelcraft.domain.user.entity.AuthorProfile;
import com.example.hot6novelcraft.domain.user.entity.ReaderProfile;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.UserDetailsImpl;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.repository.AuthorProfileRepository;
import com.example.hot6novelcraft.domain.user.repository.ReaderProfileRepository;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j(topic = "AuthService")
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final UserCacheService userCacheService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RedisUtil redisUtil;
    private final AuthorProfileRepository authorProfileRepository;
    private final ReaderProfileRepository readerProfileRepository;

    /* ======== 로그인 및 로그아웃 ========
    1. 로그인
    2. 내 정보 조회
    3. 회원정보 수정 - 공통, 작가, 독자별
    4. 비밀번호 변경 - TODO 번호 인증 진행
    5. TODO 회원 탈퇴
    6. 로그아웃
    =================================== */

    public LoginUserResponse login(LoginUserRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userDetails.getUser();

        String accessToken = jwtUtil.createAccessToken(user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.createRefreshToken(user.getEmail());

        long refreshExpiration = jwtUtil.getRefreshExpiration();
        userCacheService.saveRefreshToken(user.getEmail(), refreshToken, refreshExpiration);

        String pureNewRefresh = jwtUtil.substringToken(refreshToken);

        user.updateRefreshToken(pureNewRefresh);

        return LoginUserResponse.of(user, accessToken, refreshToken);
    }

    public MyPageResponse getMyPage(UserDetailsImpl userDetails) {

        User user = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(()-> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        return switch(user.getRole()) {
            case AUTHOR -> {
                AuthorProfile authorProfile = authorProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_AUTHOR_PROFILE));
                yield MyPageResponse.ofAuthor(user, authorProfile);
            }
            case READER -> {
                ReaderProfile readerProfile = readerProfileRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));
                yield MyPageResponse.ofReader(user, readerProfile);
            }
            default -> MyPageResponse.ofDefault(user);
        };
    }

    public CommonUpdateResponse updateUserInfo(CommonUpdateRequest request, UserDetailsImpl userDetails) {

        User user = userRepository.findByIdAndIsDeletedFalse(userDetails.getUser().getId())
                .orElseThrow(()-> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        if(userRepository.existsByNicknameAndIdNot(request.nickname(), user.getId())) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_NICKNAME_ALREADY_EXISTS);
        }

        // TODO 폰 검증 로직 추가 + SMS 인증 서비스 연동

        user.update(request.nickname(), request.phoneNo());

        log.info("[공통 회원정보 수정] email = {} ", user.getEmail());

        return CommonUpdateResponse.of(user);
    }

    public AuthorUpdateResponse authorUpdateProfile(AuthorRequest request, UserDetailsImpl userDetails) {

        User user = userRepository.findByIdAndIsDeletedFalse(userDetails.getUser().getId())
                .orElseThrow(()-> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        if(user.getRole() != UserRole.AUTHOR) {
            throw new ServiceErrorException(NovelExceptionEnum.NOVEL_AUTHOR_FORBIDDEN);
        }

        AuthorProfile authorProfile = authorProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_AUTHOR_PROFILE));

        authorProfile.authorUpdateProfile(
                request.mainGenreToString()
                , request.bio()
                , request.instagramLinks()
                , request.xLinks()
                , request.blogLinks()
                , Boolean.TRUE.equals(request.allowMenteeRequest())
        );

        log.info("[작가 프로필 수정] email: {}", user.getEmail());

        return AuthorUpdateResponse.of(authorProfile);

    }

    public ReaderUpdateResponse readerUpdateProfile(ReaderUpdatedRequest request, UserDetailsImpl userDetails) {

        User user = userRepository.findByIdAndIsDeletedFalse(userDetails.getUser().getId())
                .orElseThrow(()-> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        ReaderProfile readerProfile = readerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        readerProfile.readerUpdateProfile(
                request.mainGenreToString(),
                request.readingGoal()
        );

        log.info("[독자 프로필 수정] email: {}", user.getEmail());

        return ReaderUpdateResponse.of(readerProfile);
    }

    public void updatePassword(String oldPassword, String newPassword, UserDetailsImpl userDetails) {

        User user = userRepository.findByIdAndIsDeletedFalse(userDetails.getUser().getId())
                .orElseThrow(()-> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        if("SOCIAL_LOGIN".equals(user.getPassword())) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
        }

        // 현재, 변경 비밀번호 검증
        if(!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_PASSWORD_NOT_MATCH);
        }

        if(passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_SAME_AS_OLD_PASSWORD);
        }

        user.updatePassword(passwordEncoder.encode(newPassword));

        log.info("[비밀번호 변경] email: {}", user.getEmail());
    }

    // TODO 회원 탈퇴


    public void logout(String accessToken, String email) {
        String token = jwtUtil.substringToken(accessToken);
        userCacheService.deleteRefreshToken(email);

        try {
            long expiration = jwtUtil.getExpiration(token);

            log.warn("===== [디버깅] 추출된 만료 시간 숫자: {} =====", expiration);
            if(expiration > 0) {
                redisUtil.setBlackList(token, "Logout", Duration.ofMillis(expiration));
                log.info("===== [블랙리스트 등록] 사용자가 로그아웃하였습니다. 남은 시간: {}ms =====", expiration);
            }
        } catch(Exception e) {
            log.warn("이미 만료된 토큰입니다.");
        }
    }
}
