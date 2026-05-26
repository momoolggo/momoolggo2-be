package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.stats.ChatbotStatsService;
import com.green.mmg.main.pet.entity.Pet;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 펫 레벨별 컨텍스트 주입:
 * <ul>
 *     <li>Lv.&lt;5: null (Lv.1~4 단순 챗봇)</li>
 *     <li>Lv.5~9: 오늘 인기 카테고리 (트렌드)</li>
 *     <li>Lv.10+: 트렌드 + 사용자 1개월 이력 + 계절/시간대 + 날씨 (날씨는 WeatherContextSource 주입 시)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class PetLevelChatbotContextProvider implements ChatbotContextProvider {

    private final ChatbotStatsService statsService;

    /** P-5: 날씨/계절/시간대 컨텍스트 — 선택적 주입 (Bean 없으면 null로 동작). */
    @Autowired(required = false)
    private WeatherContextSource weatherContextSource;

    @Override
    public String buildContext(Pet pet) {
        if (pet == null) return null;
        int level = pet.getLevel();
        if (level < 5) return null;

        StringBuilder sb = new StringBuilder();
        String trend = statsService.getTodayTrendText();
        if (trend != null) sb.append(trend);

        if (level >= 10) {
            String history = statsService.getUserHistoryText(pet.getUserNo());
            if (history != null) {
                if (!sb.isEmpty()) sb.append("\n");
                sb.append(history);
            }
            if (weatherContextSource != null) {
                String weather = weatherContextSource.buildLv10Context(pet.getUserNo());
                if (weather != null && !weather.isBlank()) {
                    if (!sb.isEmpty()) sb.append("\n");
                    sb.append(weather);
                }
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
