package com.hhplus.hhplus_ecommerce.common.exception;

import lombok.Getter;

/**
 * 비즈니스 로직 예외
 * 도메인 규칙 위반 시 발생
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}