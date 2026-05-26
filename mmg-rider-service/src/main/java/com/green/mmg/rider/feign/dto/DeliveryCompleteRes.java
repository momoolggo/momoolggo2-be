package com.green.mmg.rider.feign.dto;

/**
 * Main → Rider 응답: 배달 완료 처리 결과 (interfaces.md §2.2).
 *
 * <p>main Provider 측 {@code DeliveryCompleteRes} (`mmg-main-service/.../internal/dto/`) 2 필드 1:1 일관.
 * Main이 {@code orders.delivery_state=3} (DELIVERED 종결, ADR-004) + {@code orders.order_state=6}
 * (CLAUDE.md §7 종결, Q-A8.e-2 (가)) 동반 UPDATE 후 반환. {@code order_state=6}은 응답 노출 X
 * (delivery 도메인 응답 일관).</p>
 */
public record DeliveryCompleteRes(
        Long orderId,
        Integer deliveryState
) {
}
