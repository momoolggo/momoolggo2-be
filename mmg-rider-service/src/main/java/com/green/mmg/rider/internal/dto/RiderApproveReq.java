package com.green.mmg.rider.internal.dto;

/**
 * Admin → Rider 승인 요청 Body — interfaces.md §3.1 (Q-A1 (라+) Group 8.5 신설 2026-05-17).
 *
 * <p>{@code approvedByAdminNo}: 호출자 admin 식별자. Phase 6+ X-Admin-No 헤더 대안.</p>
 */
public record RiderApproveReq(
        Long approvedByAdminNo
) {
}
