package com.green.mmg.main.chatbot;

/**
 * 시간대 + 계절 컨텍스트 — 전 레벨 펫 챗봇에 주입 (2026-06-06 메뉴 추천 강화).
 *
 * <p>이전 `WeatherContextSource`에서 리네임. 날씨 부분은 거짓 약속이라 제거됨
 * (기상청 실 API 호출은 tech-debt). 시간대/계절은 LocalDateTime 기반 실 계산.</p>
 */
public interface AmbientContextSource {
    /** 사용자 기준 시간대/계절 보조 컨텍스트. null/blank = 미주입. */
    String buildAmbientContext(Long userNo);
}
