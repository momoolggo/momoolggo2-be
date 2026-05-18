package com.green.mmg.admin.dto.feign;

import java.time.LocalDate;

/**
 * admin → rider Feign: 주간 정산 집계 요청 (interfaces.md §3.4 + rider Provider {@code CalculateReq} 일관, Group 5 신설 2026-05-17).
 *
 * <p>rider 측 {@code com.green.mmg.rider.settlement.dto.CalculateReq} record 2 필드 1:1 일관 (case-#34 영역 분리 강제 패턴 일관 — DTO 별 신설).
 * Q-A2 (다) path 따름 — rider Provider `/internal/rider/settlement/calculate`.</p>
 */
public record RiderSettlementCalculateReq(
        LocalDate periodStart,
        LocalDate periodEnd
) {
}
