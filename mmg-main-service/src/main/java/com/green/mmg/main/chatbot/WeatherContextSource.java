package com.green.mmg.main.chatbot;

/**
 * Lv.10+ 컨텍스트 — 날씨 + 계절 + 시간대 + (옵션) 사용자 지역.
 * P-5에서 구현체 @Component 등록 시 PetLevelChatbotContextProvider가 주입받아 활용.
 */
public interface WeatherContextSource {
    /** 사용자 기준 날씨/계절/시간대 보조 컨텍스트. null/blank = 미주입. */
    String buildLv10Context(Long userNo);
}
