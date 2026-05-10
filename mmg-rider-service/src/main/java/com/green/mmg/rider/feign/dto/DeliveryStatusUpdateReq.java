package com.green.mmg.rider.feign.dto;

import java.time.LocalDateTime;

/**
 * Rider → Main: 배달 상태 변경 알림 (interfaces.md §2.1).
 *
 * <p>{@code deliveryStatus}는 ADR-004 7-state enum의 {@code .name()} 직렬화.
 * Main 측 매핑(orders.delivery_state 1/2/3)은 ADR-004 line 86-96 표 인용.</p>
 */
public record DeliveryStatusUpdateReq(
        String deliveryStatus,
        Long riderNo,
        LocalDateTime changedAt
) {
}
