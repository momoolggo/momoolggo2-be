package com.green.mmg.main.chatbot.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotStatsService {

    private final ChatbotStatsMapper statsMapper;

    private static final int TOP_N = 3;
    private static final int RECENT_DAYS = 30;

    /** Lv.5~9 트렌드 컨텍스트 — 오늘 인기 카테고리 TOP 3. */
    @Transactional(readOnly = true)
    public String getTodayTrendText() {
        try {
            List<Map<String, Object>> rows = statsMapper.findTodayTopCategories(TOP_N);
            if (rows == null || rows.isEmpty()) return null;
            String joined = rows.stream()
                    .map(r -> String.valueOf(r.get("categoryName")))
                    .collect(Collectors.joining(", "));
            return "오늘 인기 카테고리: " + joined;
        } catch (Exception e) {
            log.warn("오늘 트렌드 조회 실패 — cause={}", e.getMessage());
            return null;
        }
    }

    /** Lv.10+ 개인화 컨텍스트 — 사용자 최근 30일 자주 주문한 카테고리 TOP 3. */
    @Transactional(readOnly = true)
    public String getUserHistoryText(Long userNo) {
        if (userNo == null) return null;
        try {
            LocalDate since = LocalDate.now().minusDays(RECENT_DAYS);
            List<Map<String, Object>> rows = statsMapper.findUserRecentTopCategories(userNo, since, TOP_N);
            if (rows == null || rows.isEmpty()) return null;
            String joined = rows.stream()
                    .map(r -> String.valueOf(r.get("categoryName")))
                    .collect(Collectors.joining(", "));
            return "최근 한 달 자주 주문하신 카테고리: " + joined;
        } catch (Exception e) {
            log.warn("사용자 이력 조회 실패 — userNo={}, cause={}", userNo, e.getMessage());
            return null;
        }
    }
}
