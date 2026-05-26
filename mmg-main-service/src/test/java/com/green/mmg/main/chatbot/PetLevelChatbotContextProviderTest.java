package com.green.mmg.main.chatbot;

import com.green.mmg.main.chatbot.stats.ChatbotStatsService;
import com.green.mmg.main.pet.entity.Pet;
import com.green.mmg.main.pet.entity.PetSpecies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PetLevelChatbotContextProvider — 레벨별 컨텍스트 분기")
class PetLevelChatbotContextProviderTest {

    @Mock private ChatbotStatsService statsService;
    @Mock private WeatherContextSource weatherContextSource;

    @InjectMocks
    private PetLevelChatbotContextProvider provider;

    private Pet petAtLevel(int level) {
        Pet p = new Pet(42L, PetSpecies.DOG, "테스트");
        for (int i = 1; i < level; i++) {
            int need = 100 + (i - 1) * 50;
            p.gainExp(need, 0);
        }
        return p;
    }

    @Test
    @DisplayName("null pet → null")
    void nullPet_returnsNull() {
        assertThat(provider.buildContext(null)).isNull();
        verifyNoInteractions(statsService);
    }

    @Test
    @DisplayName("Lv.1~4 → null (단순 챗봇)")
    void lowLevel_returnsNull() {
        assertThat(provider.buildContext(petAtLevel(1))).isNull();
        assertThat(provider.buildContext(petAtLevel(4))).isNull();
        verifyNoInteractions(statsService);
    }

    @Test
    @DisplayName("Lv.5 → 오늘 트렌드만")
    void lv5_trendOnly() {
        when(statsService.getTodayTrendText()).thenReturn("오늘 인기 카테고리: 치킨, 피자");

        String result = provider.buildContext(petAtLevel(5));

        assertThat(result).isEqualTo("오늘 인기 카테고리: 치킨, 피자");
        verify(statsService, never()).getUserHistoryText(any());
    }

    @Test
    @DisplayName("Lv.9 → 트렌드만 (사용자 이력 X)")
    void lv9_trendOnly() {
        when(statsService.getTodayTrendText()).thenReturn("오늘 인기: 한식");

        String result = provider.buildContext(petAtLevel(9));

        assertThat(result).isEqualTo("오늘 인기: 한식");
        verify(statsService, never()).getUserHistoryText(any());
    }

    @Test
    @DisplayName("Lv.10 → 트렌드 + 사용자 이력 + 날씨 (WeatherContextSource 주입 시)")
    void lv10_allLayers() {
        ReflectionTestUtils.setField(provider, "weatherContextSource", weatherContextSource);
        when(statsService.getTodayTrendText()).thenReturn("오늘 인기: 치킨");
        when(statsService.getUserHistoryText(42L)).thenReturn("최근 자주: 떡볶이");
        when(weatherContextSource.buildLv10Context(42L)).thenReturn("현재 날씨: 비 / 봄");

        String result = provider.buildContext(petAtLevel(10));

        assertThat(result)
                .contains("오늘 인기: 치킨")
                .contains("최근 자주: 떡볶이")
                .contains("현재 날씨: 비 / 봄");
    }

    @Test
    @DisplayName("Lv.10 + WeatherContextSource 미주입 → 트렌드 + 이력만")
    void lv10_noWeather() {
        when(statsService.getTodayTrendText()).thenReturn("오늘 인기: 치킨");
        when(statsService.getUserHistoryText(42L)).thenReturn("최근 자주: 떡볶이");

        String result = provider.buildContext(petAtLevel(10));

        assertThat(result)
                .contains("오늘 인기: 치킨")
                .contains("최근 자주: 떡볶이");
    }

    @Test
    @DisplayName("모든 stat null → null (빈 컨텍스트 미주입)")
    void allNull_returnsNull() {
        when(statsService.getTodayTrendText()).thenReturn(null);

        String result = provider.buildContext(petAtLevel(5));

        assertThat(result).isNull();
    }
}
