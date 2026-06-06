package com.green.mmg.main.chatbot;

import com.green.mmg.common.dto.ResultResponse;
import com.green.mmg.common.model.UserPrincipal;
import com.green.mmg.main.chatbot.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/sessions")
    public ResultResponse<ChatSessionRes> startSession(@AuthenticationPrincipal UserPrincipal principal,
                                                       @RequestBody ChatSessionStartReq req) {
        return new ResultResponse<>("세션 시작",
                chatbotService.startSession(
                        principal.getSignedUserNo(), principal.getRole(),
                        req.getEntryPoint(), req.getToneMode()));
    }

    @GetMapping("/sessions")
    public ResultResponse<List<ChatSessionRes>> listMySessions(@AuthenticationPrincipal UserPrincipal principal) {
        return new ResultResponse<>("세션 목록",
                chatbotService.listMySessions(principal.getSignedUserNo()));
    }

    @PostMapping("/sessions/{sessionId}/messages")
    public ResultResponse<ChatSendRes> sendMessage(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long sessionId,
                                                   @RequestBody ChatSendReq req) {
        return new ResultResponse<>("메시지 전송",
                chatbotService.sendMessage(principal.getSignedUserNo(), principal.getRole(), sessionId, req.getContent()));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ResultResponse<List<ChatMessageRes>> listMessages(@AuthenticationPrincipal UserPrincipal principal,
                                                             @PathVariable Long sessionId) {
        return new ResultResponse<>("메시지 이력",
                chatbotService.listMessages(principal.getSignedUserNo(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/escalate")
    public ResultResponse<ChatSessionRes> escalate(@AuthenticationPrincipal UserPrincipal principal,
                                                   @PathVariable Long sessionId) {
        return new ResultResponse<>("상담원 연결 요청",
                chatbotService.escalate(principal.getSignedUserNo(), sessionId));
    }

    @PostMapping("/sessions/{sessionId}/close")
    public ResultResponse<ChatSessionRes> close(@AuthenticationPrincipal UserPrincipal principal,
                                                @PathVariable Long sessionId) {
        return new ResultResponse<>("세션 종료",
                chatbotService.closeSession(principal.getSignedUserNo(), sessionId));
    }
}
