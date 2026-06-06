package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.stats.ChatbotStatsService;
import com.green.mmg.main.pet.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 펫 레벨별 컨텍스트 주입 — 발표 자료 박제 명세(`발표용 자료/02_기능별/펫_챗봇.md`):
 * <ul>
 *     <li>Lv.1~4: null (단순 안내·잡담만, 추천 X — 의도된 차별점)</li>
 *     <li>Lv.5~9: 오늘 인기 카테고리 (트렌드)</li>
 *     <li>Lv.10+: 트렌드 + 사용자 1개월 이력 + 시간대 + 계절 (날씨는 거짓 약속이라 2026-06-06 제거)</li>
 * </ul>
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
        if (level < 5) return null;  // 발표 자료 박제 — Lv.1~4 단순 안내만 (추천 차단)

        StringBuilder sb = new StringBuilder();
        String trend = statsService.getTodayTrendText();
        if (trend != null) sb.append(trend);

        // Lv.10+ 만 사용자 1개월 주문 이력 + 시간대/계절 추가 (개인 맞춤 깊이)
        if (level >= 10) {
            String history = statsService.getUserHistoryText(pet.getUserNo());
            if (history != null) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(history);
            }
            if (ambientContextSource != null) {
                String ambient = ambientContextSource.buildAmbientContext(pet.getUserNo());
                if (ambient != null && !ambient.isBlank()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(ambient);
                }
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
