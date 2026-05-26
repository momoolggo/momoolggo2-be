package com.green.mmg.main.chatbot.dto;

import com.green.mmg.main.chatbot.entity.SessionStatus;
import lombok.Getter;

@Getter
public class ChatSendRes {
    private final ChatMessageRes userMessage;
    private final ChatMessageRes assistantMessage;
    private final SessionStatus sessionStatus;

    public ChatSendRes(ChatMessageRes userMessage, ChatMessageRes assistantMessage, SessionStatus sessionStatus) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.sessionStatus = sessionStatus;
    }
}
