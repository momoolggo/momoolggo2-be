package com.green.mmg.main.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

/**
 * Gemini responseSchema 매핑 — 멘토 피드백 #1+#2 (2026-06-06).
 * {message, menuKeywords[]} 구조 강제 출력 후 캐스팅.
 */
@Getter
public class ChatRecommendation {
    private final String message;
    private final List<String> menuKeywords;

    @JsonCreator
    public ChatRecommendation(@JsonProperty("message") String message,
                              @JsonProperty("menuKeywords") List<String> menuKeywords) {
        this.message = message;
        this.menuKeywords = menuKeywords == null ? List.of() : menuKeywords;
    }
}
