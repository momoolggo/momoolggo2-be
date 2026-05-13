package com.green.mmg.rider.work.dto;

import java.time.LocalDateTime;

/**
 * POST /api/rider/work-session/end 응답 — 종료된 세션 요약 (ADR-008 D9-a).
 */
public record WorkSessionEndRes(
        Long sessionNo,
        LocalDateTime endedAt,
        Integer workSeconds,
        Integer breakSeconds
) {
}
