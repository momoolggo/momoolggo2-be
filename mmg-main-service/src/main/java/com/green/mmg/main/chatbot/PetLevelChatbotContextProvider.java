package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.stats.ChatbotStatsService;
import com.green.mmg.main.pet.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 펫 레벨별 컨텍스트 주입:
 * <ul>
 *     <li>Lv.1~4: 오늘 트렌드 + 시간대/계절 (2026-06-06 메뉴 추천 핵심 기능 강화)</li>
 *     <li>Lv.5~9: 오늘 트렌드 + 시간대/계절</li>
 *     <li>Lv.10+: 위 + 사용자 1개월 주문 이력</li>
 * </ul>
 *
 * <p>2026-06-06: 날씨는 거짓 약속이라 제거됨. 기상청 실 API 도입은 tech-debt.</p>
 */
@Component
@RequiredArgsConstructor
public class PetLevelChatbotContextProvider implements ChatbotContextProvider {

    private final ChatbotStatsService statsService;

    /** 시간대/계절 컨텍스트 — 선택적 주입 (Bean 없으면 null로 동작). */
    @Autowired(required = false)
    private AmbientContextSource ambientContextSource;

    @Override
    public String buildContext(Pet pet) {
        if (pet == null) return null;
        int level = pet.getLevel();

        StringBuilder sb = new StringBuilder();
        String trend = statsService.getTodayTrendText();
        if (trend != null) sb.append(trend);

        // 2026-06-06 시간대/계절은 전 레벨에 주입
        if (ambientContextSource != null) {
            String ambient = ambientContextSource.buildAmbientContext(pet.getUserNo());
            if (ambient != null && !ambient.isBlank()) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(ambient);
            }
        }

        // Lv.10+ 만 사용자 1개월 주문 이력 추가 (개인 맞춤 깊이)
        if (level >= 10) {
            String history = statsService.getUserHistoryText(pet.getUserNo());
            if (history != null) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(history);
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
