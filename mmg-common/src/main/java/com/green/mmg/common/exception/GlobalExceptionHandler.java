package com.green.mmg.common.exception;

import com.green.mmg.common.dto.ResultResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * MSA 전 서비스 공용 예외 처리. ResultResponse 형식으로 응답 통일.
 * 시큐리티 예외(401/403)는 시큐리티 필터 단계에서 처리되므로 여기서 안 잡힘 →
 * BaseSecurityConfig의 exceptionHandling()에서 별도 처리.
 */
@Slf4j
@RestControllerAdvice
@ConditionalOnClass(RestControllerAdvice.class)
public class GlobalExceptionHandler {

    /** 비즈니스 규칙 위반 — status는 BusinessException이 결정 */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResultResponse<Void>> handleBusiness(BusinessException e) {
        log.info("BusinessException: {} (status={})", e.getMessage(), e.getStatus());
        return ResponseEntity.status(e.getStatus())
                .body(new ResultResponse<>(e.getMessage(), null));
    }

    /** @Valid 검증 실패 → 400 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResultResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "검증에 실패했습니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse<>(msg, null));
    }

    /** JSON 파싱 실패 → 400 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResultResponse<Void>> handleParse(HttpMessageNotReadableException e) {
        log.info("HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ResultResponse<>("요청 형식이 올바르지 않습니다.", null));
    }

    /** Spring 7.x: 정적 리소스 미존재 → 404 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ResultResponse<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ResultResponse<>("리소스를 찾을 수 없습니다.", null));
    }

    /** 그 외 RuntimeException → 500 (메시지는 그대로 — 운영에선 마스킹 검토) */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResultResponse<Void>> handleRuntime(RuntimeException e) {
        log.warn("Unhandled RuntimeException", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResultResponse<>(e.getMessage() != null ? e.getMessage() : "서버 오류가 발생했습니다.", null));
    }

    /** 마지막 안전망 — Checked Exception 등 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResultResponse<Void>> handleAll(Exception e) {
        log.error("Unhandled Exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ResultResponse<>("서버 오류가 발생했습니다.", null));
    }
}
