package com.green.mmg.main.chatbot.dto;

import com.green.mmg.main.chatbot.entity.ChatMessage;
import com.green.mmg.main.chatbot.entity.MessageRole;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ChatMessageRes {
    private final Long messageId;
    private final Long sessionId;
    private final MessageRole role;
    private final String content;
    private final LocalDateTime createdAt;

    public ChatMessageRes(ChatMessage m) {
        this.messageId = m.getMessageId();
        this.sessionId = m.getSessionId();
        this.role = m.getRole();
        this.content = m.getContent();
        this.createdAt = m.getCreatedAt();
    }
}
