package com.example.hot6novelcraft.domain.user.service;

import com.example.hot6novelcraft.common.exception.ServiceErrorException;
import com.example.hot6novelcraft.common.exception.domain.UserExceptionEnum;
import com.example.hot6novelcraft.common.security.JwtUtil;
import com.example.hot6novelcraft.common.security.RedisUtil;
import com.example.hot6novelcraft.domain.user.dto.request.*;
import com.example.hot6novelcraft.domain.user.dto.response.AdminSignupResponse;
import com.example.hot6novelcraft.domain.user.dto.response.SignupResponse;
import com.example.hot6novelcraft.domain.user.dto.response.SocialSignupResponse;
import com.example.hot6novelcraft.domain.user.entity.AuthorProfile;
import com.example.hot6novelcraft.domain.user.entity.ReaderProfile;
import com.example.hot6novelcraft.domain.user.entity.SocialAuth;
import com.example.hot6novelcraft.domain.user.entity.User;
import com.example.hot6novelcraft.domain.user.entity.enums.ProviderSns;
import com.example.hot6novelcraft.domain.user.entity.enums.UserRole;
import com.example.hot6novelcraft.domain.user.repository.AuthorProfileRepository;
import com.example.hot6novelcraft.domain.user.repository.ReaderProfileRepository;
import com.example.hot6novelcraft.domain.user.repository.SocialAuthRepository;
import com.example.hot6novelcraft.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j(topic = "SignupService")
@Service
@RequiredArgsConstructor
public class SignupService {

    private final UserRepository userRepository;
    private final ReaderProfileRepository readerProfileRepository;
    private final AuthorProfileRepository authorProfileRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SocialAuthRepository socialAuthRepository;
    private final RedisUtil redisUtil;

    /** ======== 중복 확인 ========
    1. 이메일 중복 확인
    2. 닉네임 중복 확인
    탈퇴 후 재가입시도 시 확인 및 30일 이내 탈퇴자가 있을 때 사용
    ============================= */
    public void checkEmail(String email) {

        Optional<User> optionalUser = userRepository.findByEmail(email);

        // 탈퇴 유예 상태(30일 이내)인 경우 -> 복구 유도 에러
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            // 이메일이 겹쳐서 가입이 안 되는 경우에도, 복구하시겠습니까? 로 유도할 수 있음
            if (user.isDeleted()) {
                throw new ServiceErrorException(UserExceptionEnum.ERR_USER_WITHDRAWAL_PENDING_CONFLICT);
            }
            // 멀쩡히 활동 중인 계정이라면? 일반적인 이메일 중복 에러
            throw new ServiceErrorException(UserExceptionEnum.ERR_EMAIL_ALREADY_EXISTS);
        }
    }

    public void checkNickname(String nickname) {
        Optional<User> optionalUser = userRepository.findByNickname(nickname);

        if (optionalUser.isPresent()) {
            User existingUser = optionalUser.get();

            // \닉네임 주인이 탈퇴 유예(30일) 상태
            if (existingUser.isDeleted()) {

                // 닉네임이 겹쳐서 가입이 안 되는 경우에도, 복구하시겠습니까? 로 유도할 수 있음
                throw new ServiceErrorException(UserExceptionEnum.ERR_USER_WITHDRAWAL_PENDING_CONFLICT);
            }

            // 멀쩡히 활동 중인 계정이라면? 일반적인 닉네임 중복 에러
            throw new ServiceErrorException(UserExceptionEnum.ERR_NICKNAME_ALREADY_EXISTS);
        }
    }

    /** ======== 회원 가입 ========
    1. 공통 회원가입
        - 독자/작가 추가 정보 기입까지 완료 후, DB 저장 및 임시 JWT 발급으로 보안 설정
        - SMS 전송 및 인증
    2. 독자 회원가입 - 임시 JWT로만 접근 가능
    3. 작가 회원가입 - 임시 JWT로만 접근 가능
    4. 관리자 회원가입 - 이메일, 비밀번호, 핸드폰 인증만 진행
    ============================= */
    @Transactional
    public String commonSignup(CommonSignupRequest request) {

        // SMS 공통 메서드 (검증만)
        validatePhoneVerification(request.phoneNo());

        // 이메일 및 닉네임 중복 확인
        checkEmail(request.email());
        checkNickname(request.nickname());

        String encoderPassword = passwordEncoder.encode(request.password());
        User user = User.register(
                request.email(),
                encoderPassword,
                request.nickname(),
                request.phoneNo(),
                request.birthDay(),
                UserRole.TEMP  // 임시 역할 부여
        );

        userRepository.save(user);

        // DB update 강제 반영
        userRepository.flush();

        // DB 저장이 확실해 Redis 인증 정보 삭제
        consumerPhoneVerification(request.phoneNo());

        log.info("공통 회원가입 완료 - email: {}", request.email());

        // 임시 JWT 유저 저장 (독자/작가 추가 가입 전 임시 상태)
        return jwtUtil.createTempToken(request.email());
    }

    @Transactional
    public SignupResponse readerSignup(ReaderSignupRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        if(!user.getRole().equals(UserRole.TEMP)) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_ALREADY_COMPLETED_SIGNUP);
        }

        user.changeRole(UserRole.READER);

        ReaderProfile readerProfile = ReaderProfile.register(
                user.getId(),
                request.mainGenreToString(),
                request.readingGoal()
        );
        readerProfileRepository.save(readerProfile);

        log.info("독자 정보 추가 가입 완료 - email: {}", email);

        return SignupResponse.of(user);
    }

    @Transactional
    public SignupResponse authorSignup(AuthorRequest request, String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        if (!user.getRole().equals(UserRole.TEMP)) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_ALREADY_COMPLETED_SIGNUP);
        }

        user.changeRole(UserRole.AUTHOR);

        AuthorProfile authorProfile = AuthorProfile.register(
                user.getId(),
                request.bio(),
                request.careerLevel(),
                request.mainGenreToString(),
                request.instagramLinks(),
                request.xLinks(),
                request.blogLinks(),
                request.allowMenteeRequest()
        );
        authorProfileRepository.save(authorProfile);

        log.info("작가 정보 추가 가입 완료 - email: {}", email);

        return SignupResponse.of(user);
    }

    @Transactional
    public AdminSignupResponse adminSignup(AdminSignupRequest request, String email) {

        // SMS 공통 메서드 (검증만)
        validatePhoneVerification(request.phoneNo());

        // 이메일 중복 확인
        checkEmail(request.email());

        String encoderPassword = passwordEncoder.encode(request.password());

        User admin = User.registerAdmin(
                request.email(),
                encoderPassword,
                request.phoneNo(),
                UserRole.ADMIN
        );
        userRepository.save(admin);

        // DB 강제 저장 후, 저장 완료와 동시에 Redis 키 삭제
        userRepository.flush();
        consumerPhoneVerification(request.phoneNo());

        log.info("관리자 가입 완료 - email: {}", email);

        return AdminSignupResponse.of(admin);
    }

    /** ======== 소셜 회원 가입 ======== */
    @Transactional
    public SocialSignupResponse socialCommonSignup(SocialSignupRequest request, String email, String providerId, ProviderSns providerSns) {

        // SMS 공통 메서드 (검증만)
        validatePhoneVerification(request.phoneNo());

        checkNickname(request.nickname());

        // 소셜 유저 생성 (비밀번호는 SOCIAL LOGIN으로 고정
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ServiceErrorException(UserExceptionEnum.ERR_NOT_FOUND_USER));

        // 탈퇴 유예 기간 유저인지 체크
        if (user.isDeleted()) {
            throw new ServiceErrorException(UserExceptionEnum.ERR_USER_WITHDRAWAL_PENDING_CONFLICT);
        }

        user.updateForSocialSignup(request.nickname(), request.phoneNo(), request.birthDay());

        // sns 정보 저장
        SocialAuth socialAuth = SocialAuth.register(
                ProviderSns.GOOGLE
                , providerId
                , user.getId()
        );
        socialAuthRepository.save(socialAuth);

        // DB 강제 저장 후, 저장 완료와 동시에 Redis 키 삭제
        userRepository.flush();
        socialAuthRepository.flush();

        consumerPhoneVerification(request.phoneNo());

        log.info("[소셜 공통 가입] 유저 생성 완료, email: {}", email);

        String tempToken = jwtUtil.createTempToken(email);

        return SocialSignupResponse.of(tempToken, email, request.nickname());
    }

    /** ======== SMS 공통 메서드 ======== */
    // SMS 전송 - 검증
    public void validatePhoneVerification(String phoneNo) {
        String cleanPhoneNo = phoneNo.replaceAll("-","");
        String verifiedKey = "SMS:VERIFIED:" + cleanPhoneNo;

        Object isVerified = redisUtil.get(verifiedKey);

        if(isVerified == null || !"TRUE".equals(isVerified.toString())) {
            log.info("[SMS] 인증되지 않은 번호로 접근 시도됨");

            throw new ServiceErrorException(UserExceptionEnum.ERR_PHONE_NOT_VERIFIED);
        }
    }

    // redis 키 삭제 필요할 때 - 삭제
    public void consumerPhoneVerification(String phoneNo) {
        String cleanPhoneNo = phoneNo.replaceAll("-","");
        String verifiedKey = "SMS:VERIFIED:" + cleanPhoneNo;
        redisUtil.delete(verifiedKey);

        log.info("[SMS] Redis 인증 확인 및 삭제 완료, phoneNo: {} ", cleanPhoneNo);
    }
}
