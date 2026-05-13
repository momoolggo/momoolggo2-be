package com.green.mmg.common.exception;

import org.springframework.http.HttpStatus;

/** 리소스를 찾지 못했을 때(404). */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
