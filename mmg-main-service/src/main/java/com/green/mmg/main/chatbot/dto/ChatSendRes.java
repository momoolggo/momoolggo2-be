package com.green.mmg.main.chatbot.dto;

import com.green.mmg.main.chatbot.entity.SessionStatus;
import lombok.Getter;

import java.util.List;

@Getter
public class ChatSendRes {
    private final ChatMessageRes userMessage;
    private final ChatMessageRes assistantMessage;
    private final SessionStatus sessionStatus;
    /** 멘토 피드백 #1+#2 (2026-06-06) — Gemini 추천 키워드로 검색한 가게 카드. 추천 의도 없으면 빈 리스트. */
    private final List<MenuCardDto> menuCards;

    public ChatSendRes(ChatMessageRes userMessage, ChatMessageRes assistantMessage, SessionStatus sessionStatus) {
        this(userMessage, assistantMessage, sessionStatus, List.of());
    }

    public ChatSendRes(ChatMessageRes userMessage, ChatMessageRes assistantMessage,
                       SessionStatus sessionStatus, List<MenuCardDto> menuCards) {
        this.userMessage = userMessage;
        this.assistantMessage = assistantMessage;
        this.sessionStatus = sessionStatus;
        this.menuCards = menuCards == null ? List.of() : menuCards;
    }
}
