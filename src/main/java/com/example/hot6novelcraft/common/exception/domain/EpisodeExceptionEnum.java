package com.example.hot6novelcraft.common.exception.domain;

import com.example.hot6novelcraft.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum EpisodeExceptionEnum implements ErrorCode {

    EPISODE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 회차를 찾을 수 없습니다."),
    EPISODE_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 회차만 수정/삭제할 수 있습니다."),
    EPISODE_ALREADY_DELETED(HttpStatus.NOT_FOUND, "이미 삭제된 회차입니다."),
    EPISODE_NUMBER_DUPLICATE(HttpStatus.BAD_REQUEST, "이미 존재하는 회차 번호입니다."),
    EPISODE_ALREADY_PUBLISHED(HttpStatus.BAD_REQUEST, "이미 발행된 회차입니다."),
    EPISODE_NUMBER_NOT_SEQUENTIAL(HttpStatus.BAD_REQUEST, "회차 번호는 순서대로 등록해야 합니다."),
    EPISODE_DELETE_NOT_LAST(HttpStatus.BAD_REQUEST, "마지막 회차만 삭제할 수 있습니다."),
    EPISODE_PREVIOUS_NOT_PUBLISHED(HttpStatus.BAD_REQUEST, "이전 회차를 먼저 발행해야 합니다."),
    EPISODE_CONTENT_EMPTY(HttpStatus.BAD_REQUEST, "본문 내용이 없는 회차는 발행할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    EpisodeExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}