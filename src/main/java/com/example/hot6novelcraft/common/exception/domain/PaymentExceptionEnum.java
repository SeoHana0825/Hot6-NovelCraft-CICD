package com.example.hot6novelcraft.common.exception.domain;

import com.example.hot6novelcraft.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum PaymentExceptionEnum implements ErrorCode {
    ERR_INVALID_PENDING(HttpStatus.CONFLICT, "결제 대기 상태에서만 가능 합니다")
    , ERR_INVALID_COMPLETE(HttpStatus.CONFLICT, "결제 완료 상태에서만 확정 가능 합니다")
    , ERR_INVALID_ORDER_COMPLETE(HttpStatus.CONFLICT, "주문 완료 상태에서만 확정 가능 합니다")
    , ERR_PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "결제 건을 찾을 수 없습니다")
    , ERR_NOT_MY_ORDER(HttpStatus.CONFLICT, "사용자가 주문한 건이 아닙니다")
    , ERR_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 금액이 일치하지 않습니다")
    , ERR_PORTONE_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "결제 서버 오류가 발생했습니다")
    , ERR_ALREADY_PAID(HttpStatus.CONFLICT, "이미 처리된 결제입니다")
    , ERR_INSUFFICIENT_POINT(HttpStatus.BAD_REQUEST, "포인트가 부족합니다")
    , ERR_POINT_NOT_FOUND(HttpStatus.NOT_FOUND, "포인트 정보를 찾을 수 없습니다");

    private final HttpStatus httpStatus;
    private final String message;

    PaymentExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
