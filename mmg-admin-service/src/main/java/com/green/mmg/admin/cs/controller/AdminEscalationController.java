package com.green.mmg.admin.cs.controller;

import com.green.mmg.admin.cs.service.AdminEscalationSseService;
import com.green.mmg.common.model.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/cs")
public class AdminEscalationController {

    private final AdminEscalationSseService escalationSseService;

    /** 관리자 화면 SSE — 챗봇 에스컬레이션 실시간 알림. */
    @GetMapping(value = "/escalation-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal UserPrincipal principal) {
        return escalationSseService.subscribe(principal.getSignedUserNo());
    }
}
