package com.green.mmg.admin.dto.feign;

/**
 * admin → rider Feign: 라이더 승인 요청 — interfaces.md §3.1 (Group 8.5 신설 2026-05-17).
 *
 * <p>rider 측 {@code com.green.mmg.rider.internal.dto.RiderApproveReq} 1:1 일관 (case-#34 영역 분리 강제).</p>
 */
public record RiderApproveReq(
        Long approvedByAdminNo
) {
}
