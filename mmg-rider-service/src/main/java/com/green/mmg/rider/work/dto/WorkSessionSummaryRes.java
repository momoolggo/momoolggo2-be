package com.green.mmg.rider.work.dto;

/**
 * GET /api/rider/work-session/summary?period= 응답 — 주간/오늘 합계.
 */
public record WorkSessionSummaryRes(
        String period,
        Integer sessionCount,
        Integer totalWorkSeconds,
        Integer totalBreakSeconds
) {
}
