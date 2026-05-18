package com.green.mmg.rider.internal.dto;

import java.time.LocalDateTime;

/**
 * Admin → Rider 제재 요청 Body — interfaces.md §3.2 (Q-A1 (라+) Group 8.5 신설 2026-05-17).
 *
 * <p>{@code suspendedByAdminNo}: 호출자 admin 식별자.
 * {@code reason}: 제재 사유 (audit log Phase 6+ outbox 위임, Q-A18 (b) 일관). 본 endpoint는 수신 + log.info만.
 * {@code untilAt}: 제재 종료 시각 (null = 영구). 본 단계 미적용 (Phase 6+ scheduler 위임), DTO 수신만.</p>
 */
public record RiderSuspendReq(
        Long suspendedByAdminNo,
        String reason,
        LocalDateTime untilAt
) {
}
