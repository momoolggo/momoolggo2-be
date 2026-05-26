package com.green.mmg.main.internal.dto;

/**
 * rider Consumer가 받는 응답 — interfaces.md §2.1.
 *
 * <p>{@code previousDeliveryState} → {@code newDeliveryState} 변경 결과. ADR-004 매핑(1배달전/2픽업완료/3배달완료).
 * 직접 반환 (ResultResponse 래핑 X) — rider {@code MainInternalClient.updateDeliveryStatus} Feign 시그니처 일관.</p>
 */
public record DeliveryStatusUpdateRes(
        Long orderId,
        Integer previousDeliveryState,
        Integer newDeliveryState
) {
}
