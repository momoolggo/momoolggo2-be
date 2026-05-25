package com.green.mmg.main.chatbot;

import com.green.mmg.main.pet.entity.Pet;

/**
 * Lv.5~9 트렌드 / Lv.10+ 개인화 추천 컨텍스트 주입 확장 포인트.
 * P-4 (트렌드) / P-5 (날씨+개인화)에서 별 구현 추가.
 *
 * <p>Lv.1~4 또는 컨텍스트 미해당 시 null 반환 — 빈 컨텍스트로 동작.</p>
 */
public interface ChatbotContextProvider {
    /** @return Gemini system instruction 뒤에 붙일 보조 컨텍스트 (null이면 미주입). */
    String buildContext(Pet pet);
}
