package com.green.mmg.admin.cs.controller;

import com.green.mmg.admin.cs.dto.EscalationReq;
import com.green.mmg.admin.cs.service.CsService;
import com.green.mmg.common.dto.ResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * P-7 main 챗봇 에스컬레이션 수신.
 * 정식 /internal/ 경로 컨벤션 박제 (기존 /api/admin/cs/internal/inquiry는 별 트랙 정리).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/chatbot")
public class InternalChatbotController {

    private final CsService csService;

    @PostMapping("/escalate")
    public ResultResponse<Void> escalate(@RequestBody EscalationReq req) {
        csService.receiveChatbotEscalation(req.getUserNo(), req.getSessionId(), req.getLastUserMessage());
        return new ResultResponse<>("에스컬레이션 접수", null);
    }
}
