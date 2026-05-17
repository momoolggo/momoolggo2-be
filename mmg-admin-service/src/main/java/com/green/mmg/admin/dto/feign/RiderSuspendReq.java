package com.green.mmg.admin.dto.feign;

import java.time.LocalDateTime;

/**
 * admin → rider Feign: 라이더 제재 요청 — interfaces.md §3.2 (Group 8.5 신설 2026-05-17).
 *
 * <p>rider 측 {@code com.green.mmg.rider.internal.dto.RiderSuspendReq} 1:1 일관 (case-#34 영역 분리 강제).</p>
 */
public record RiderSuspendReq(
        Long suspendedByAdminNo,
        String reason,
        LocalDateTime untilAt
) {
}
