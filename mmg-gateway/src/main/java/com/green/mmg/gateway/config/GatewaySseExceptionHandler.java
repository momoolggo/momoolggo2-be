package com.green.mmg.gateway.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Gateway SSE 프록시 예외 처리.
 *
 * <p>GlobalExceptionHandler(mmg-common)가 text/event-stream 응답에
 * ResultResponse(JSON)를 쓰려다 HttpMessageNotWritableException 발생하는 문제 방지.
 * HIGHEST_PRECEDENCE로 먼저 잡아서 SSE 요청이면 조용히 종료, 일반 요청은 재던짐.</p>
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewaySseExceptionHandler {

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e,
                                HttpServletRequest request,
                                HttpServletResponse response) throws Exception {
        String accept = request.getHeader("Accept");
        if (accept != null && accept.contains("text/event-stream")) {
            log.debug("SSE 프록시 예외 무시 (정상 종료) uri={} error={}", request.getRequestURI(), e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.flushBuffer();
            }
            return;
        }
        throw e;
    }
}
