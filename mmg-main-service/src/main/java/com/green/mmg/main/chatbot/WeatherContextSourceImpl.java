package com.green.mmg.main.chatbot;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Lv.10+ 날씨/계절/시간대 컨텍스트 — 학원 발표 시연용 단순화.
 *
 * <p>현재 박제:
 * <ul>
 *     <li>계절: 월별 분기 (3-5=봄 / 6-8=여름 / 9-11=가을 / 12-2=겨울)</li>
 *     <li>시간대: 시간별 분기 (5-10=아침 / 11-13=점심 / 14-17=오후 / 18-21=저녁 / 그 외=심야)</li>
 *     <li>날씨: 시연용 placeholder "맑음" (기상청 실 API 호출은 tech-debt Phase 6+)</li>
 * </ul></p>
 *
 * <p>tech-debt: 기상청 단기예보 API + nx/ny 사용자별 좌표 변환 + Redis 캐시는 Phase 6+ 위임.</p>
 */
@Component
public class WeatherContextSourceImpl implements WeatherContextSource {

    private final Clock clock;

    public WeatherContextSourceImpl() {
        this(Clock.systemDefaultZone());
    }

    public WeatherContextSourceImpl(Clock clock) {
        this.clock = clock;
    }

    @Override
    public String buildLv10Context(Long userNo) {
        LocalDateTime now = LocalDateTime.now(clock);
        String season = season(now.getMonthValue());
        String timeSlot = timeSlot(now.getHour());
        String weather = lookupWeather(userNo);  // placeholder "맑음"
        return String.format("현재 시즌: %s / 시간대: %s / 날씨: %s", season, timeSlot, weather);
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

    /** tech-debt: 기상청 API + Redis 캐시 Phase 6+. 현재 시연용 placeholder. */
    private String lookupWeather(Long userNo) {
        return "맑음";
    }
}
