package com.green.mmg.admin.dto.feign;

/**
 * admin → rider Feign: 정산 confirm 요청 (interfaces.md §3.3 + rider Provider {@code ConfirmReq} 일관, Group 5 신설 2026-05-17).
 *
 * <p>rider 측 {@code com.green.mmg.rider.settlement.dto.ConfirmReq} record 1 필드 1:1 일관 (case-#34 영역 분리 강제).
 * {@code adminNo}: 호출자 admin 식별자.</p>
 */
public record RiderSettlementConfirmReq(
        Long adminNo
) {
}
