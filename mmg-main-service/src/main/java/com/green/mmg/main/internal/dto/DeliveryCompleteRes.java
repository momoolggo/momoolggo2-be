package com.green.mmg.main.internal.dto;

/**
 * rider Consumer가 받는 응답 — interfaces.md §2.2.
 *
 * <p>{@code deliveryState}: 3 (DELIVERED 종결, ADR-004 매핑 표 line 90-98). order_state=6 동반 UPDATE는
 * 응답 노출 X (CLAUDE.md §7 일관, Q-A8.e-2 (가)). 직접 반환 (ResultResponse 래핑 X).</p>
 */
public record DeliveryCompleteRes(
        Long orderId,
        Integer deliveryState
) {
}
