package com.green.mmg.main.chatbot;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 시간대 + 계절 컨텍스트 — 2026-06-06 메뉴 추천 강화.
 *
 * <p>구현:
 * <ul>
 *     <li>계절: 월별 분기 (3-5=봄 / 6-8=여름 / 9-11=가을 / 12-2=겨울)</li>
 *     <li>시간대: 시간별 분기 (5-10=아침 / 11-13=점심 / 14-17=오후 / 18-21=저녁 / 그 외=심야)</li>
 * </ul></p>
 *
 * <p>이전 WeatherContextSourceImpl의 lookupWeather placeholder("맑음")는 거짓 약속이라
 * 2026-06-06 제거. 기상청 단기예보 API + nx/ny 좌표 변환 + Redis 캐시는 tech-debt Phase 6+.</p>
 */
@Component
public class AmbientContextSourceImpl implements AmbientContextSource {

    private final Clock clock;

    public AmbientContextSourceImpl() {
        this(Clock.systemDefaultZone());
    }

    public AmbientContextSourceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String buildAmbientContext(Long userNo) {
        LocalDateTime now = LocalDateTime.now(clock);
        String season = season(now.getMonthValue());
        String timeSlot = timeSlot(now.getHour());
        return String.format("현재 시즌: %s / 시간대: %s", season, timeSlot);
    }

    private String season(int month) {
        if (month >= 3 && month <= 5) return "봄";
        if (month >= 6 && month <= 8) return "여름";
        if (month >= 9 && month <= 11) return "가을";
        return "겨울";
    }

    private String timeSlot(int hour) {
        if (hour >= 5 && hour <= 10) return "아침";
        if (hour >= 11 && hour <= 13) return "점심";
        if (hour >= 14 && hour <= 17) return "오후";
        if (hour >= 18 && hour <= 21) return "저녁";
        return "심야";
    }
}
