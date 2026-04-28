package com.green.mmg.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 규칙 위반 시 던지는 예외 베이스.
 * 메시지는 사용자에게 그대로 노출되므로 안전한 문구로 작성.
 * status를 명시하지 않으면 400(Bad Request).
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
