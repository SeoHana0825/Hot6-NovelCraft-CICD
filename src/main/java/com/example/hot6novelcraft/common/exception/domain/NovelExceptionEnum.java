package com.example.hot6novelcraft.common.exception.domain;

import com.example.hot6novelcraft.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum NovelExceptionEnum implements ErrorCode {

    NOVEL_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 소설을 찾을 수 없습니다.")
    , NOVEL_FORBIDDEN(HttpStatus.FORBIDDEN, "본인 소설만 수정/삭제할 수 있습니다.")
    , NOVEL_ALREADY_DELETED(HttpStatus.NOT_FOUND, "이미 삭제된 소설입니다.");

    private final HttpStatus httpStatus;
    private final String message;

    NovelExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}