package com.example.hot6novelcraft.common.exception.domain;

import com.example.hot6novelcraft.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum UserExceptionEnum implements ErrorCode {
    ERR_NOT_FOUND(HttpStatus.NOT_FOUND, "유저를 찾을 수 없습니다")
    , ERR_INVALID_ROLE(HttpStatus.BAD_REQUEST, "역할을 찾을 수 없습니다")

    // 회원가입
    , ERR_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다")
    , ERR_INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다")
    , ERR_NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다")
    , ERR_INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 2~10자이내로 입력하세요")
    , ERR_INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다")

    // 휴대폰 인증
    , ERR_INVALID_PHONE_NO(HttpStatus.BAD_REQUEST, "휴대폰 번호형식이 올바르지 않습니다")
    , ERR_PHONE_NO_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 휴대폰 번호입니다")
    , ERR_MANY_TO_PHONE_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 시도해주세요")
    , ERR_INVALID_PHONE(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다")
    , ERR_INVALID_PHONE_VERIFICATION(HttpStatus.UNAUTHORIZED,"휴대폰 인증이 유효하지 않습니다")

    // 로그인
    , ERR_FAILED_SOCIAL(HttpStatus.NOT_FOUND, "소셜 인증에 실패했습니다")
    , ERR_INVALID_EMAIL_OR_PASSWORD(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다")

    // Token
    , ERR_ACCESS_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AccessToken이 만료되었습니다. 재발급해주세요")
    , ERR_REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "RefreshToke이 만료되었습니다. 다시 로그인해주세요")
    , ERR_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다")
    , REDIS_CONNECTION_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "인증 캐시 서버 연결에 실패했습니다. 다시 시도해주세요");

    private final HttpStatus httpStatus;
    private final String message;

    UserExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
