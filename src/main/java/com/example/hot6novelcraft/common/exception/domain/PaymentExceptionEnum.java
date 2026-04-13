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
    , ERR_NOT_MY_ORDER(HttpStatus.CONFLICT, "사용자가 주문한 건이 아닙니다");

    private final HttpStatus httpStatus;
    private final String message;

    PaymentExceptionEnum(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
