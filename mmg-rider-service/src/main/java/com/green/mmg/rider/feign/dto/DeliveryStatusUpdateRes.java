package com.green.mmg.rider.feign.dto;

/**
 * Main → Rider 응답: 배달 상태 변경 결과 (interfaces.md §2.1).
 *
 * <p>Main이 orders.delivery_state UPDATE 후 매핑 결과 반환. previousDeliveryState/newDeliveryState는
 * orders 테이블 1/2/3 매핑 (ADR-004 line 86-96).</p>
 */
public record DeliveryStatusUpdateRes(
        Long orderId,
        Integer previousDeliveryState,
        Integer newDeliveryState
) {
}
