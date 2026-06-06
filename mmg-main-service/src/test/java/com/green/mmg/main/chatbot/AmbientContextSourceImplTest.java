package com.green.mmg.main.chatbot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2026-06-06 — WeatherContextSourceImplTest 리네임. 날씨 검증 제거(거짓 약속이라 삭제됨).
 * 계절 + 시간대만 검증.
 */
@DisplayName("AmbientContextSourceImpl — 계절/시간대 분기")
class AmbientContextSourceImplTest {

    private AmbientContextSourceImpl atDateTime(String iso) {
        Clock fixed = Clock.fixed(LocalDateTime.parse(iso).atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        return new AmbientContextSourceImpl(fixed);
    }

    @Test
    @DisplayName("봄 + 점심")
    void spring_lunch() {
        String result = atDateTime("2026-04-15T12:30:00").buildAmbientContext(42L);
        assertThat(result).contains("봄").contains("점심");
        // 날씨 단어가 더 이상 응답에 없는지 검증 (거짓 약속 제거 회귀 방지)
        assertThat(result).doesNotContain("맑음").doesNotContain("날씨");
    }

    @Test
    @DisplayName("여름 + 저녁")
    void summer_evening() {
        String result = atDateTime("2026-07-20T19:00:00").buildAmbientContext(42L);
        assertThat(result).contains("여름").contains("저녁");
    }

    @Test
    @DisplayName("가을 + 아침")
    void autumn_morning() {
        String result = atDateTime("2026-10-05T08:00:00").buildAmbientContext(42L);
        assertThat(result).contains("가을").contains("아침");
    }

    @Test
    @DisplayName("겨울 + 심야")
    void winter_lateNight() {
        String result = atDateTime("2026-01-15T02:30:00").buildAmbientContext(42L);
        assertThat(result).contains("겨울").contains("심야");
    }

    @Test
    @DisplayName("계절 경계: 3월 1일 = 봄, 12월 1일 = 겨울, 9월 1일 = 가을, 6월 1일 = 여름")
    void seasonBoundary() {
        assertThat(atDateTime("2026-03-01T12:00:00").buildAmbientContext(42L)).contains("봄");
        assertThat(atDateTime("2026-12-01T12:00:00").buildAmbientContext(42L)).contains("겨울");
        assertThat(atDateTime("2026-09-01T12:00:00").buildAmbientContext(42L)).contains("가을");
        assertThat(atDateTime("2026-06-01T12:00:00").buildAmbientContext(42L)).contains("여름");
    }
}
