package com.green.mmg.main.chatbot.dto;

import com.green.mmg.main.chatbot.entity.ChatSession;
import com.green.mmg.main.chatbot.entity.EntryPoint;
import com.green.mmg.main.chatbot.entity.SessionStatus;
import com.green.mmg.main.chatbot.entity.ToneMode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatSessionRes {
    private final Long sessionId;
    private final Long userNo;
    private final Long petNo;
    private final EntryPoint entryPoint;
    private final ToneMode toneMode;
    private final SessionStatus status;
    private final LocalDateTime escalatedAt;
    private final LocalDateTime closedAt;

    public ChatSessionRes(ChatSession s) {
        this.sessionId = s.getSessionId();
        this.userNo = s.getUserNo();
        this.petNo = s.getPetNo();
        this.entryPoint = s.getEntryPoint();
        this.toneMode = s.getToneMode();
        this.status = s.getStatus();
        this.escalatedAt = s.getEscalatedAt();
        this.closedAt = s.getClosedAt();
    }
}
