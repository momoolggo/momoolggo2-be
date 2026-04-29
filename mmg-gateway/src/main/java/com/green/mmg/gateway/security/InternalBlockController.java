package com.green.mmg.gateway.security;

import com.green.mmg.common.dto.ResultResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 4-B: /internal/** 외부 차단.
 *
 * <p>Internal API (Phase 4-A에서 도입된 cross-schema Feign 통신 endpoint)는 Gateway 외부에 노출하지 않음.
 * application.yml의 routes에 /internal/** 라우트를 정의하지 않고 + 본 Controller가 명시적 403으로 응답.</p>
 *
 * <p>서비스 간 직접 통신 (Feign)은 각 서비스 포트 (8081/8080/...)로 직접 호출 — Gateway 거치지 않음.</p>
 */
@RestController
public class InternalBlockController {

    @RequestMapping("/internal/**")
    public ResponseEntity<ResultResponse<Void>> block() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ResultResponse<>("외부 접근 불가 — Internal API는 서비스 간 통신 전용입니다.", null));
    }
}
