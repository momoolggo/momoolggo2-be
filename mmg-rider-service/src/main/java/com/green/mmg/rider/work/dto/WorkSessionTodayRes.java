package com.green.mmg.rider.work.dto;

import java.time.LocalDateTime;

/**
 * GET /api/rider/work-session/today 응답 — Figma 170202 근무관리 카드.
 *
 * <p>오늘 시작된 세션 1건 (진행 중 또는 종료). 없으면 sessionNo null.</p>
 */
public record WorkSessionTodayRes(
        Long sessionNo,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer workSeconds,
        Integer breakSeconds,
        String vehicleType
) {
}
