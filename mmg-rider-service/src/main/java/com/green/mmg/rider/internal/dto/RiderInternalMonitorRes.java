package com.green.mmg.rider.internal.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin → Rider 모니터 응답 — GET /internal/rider/monitor.
 *
 * <p>{@code summary}: delivery.status 4그룹 카운트.
 * <ul>
 *   <li>{@code waiting} = WAITING_ASSIGN</li>
 *   <li>{@code assigned} = ASSIGNED + ARRIVED_AT_STORE + AWAITING_PICKUP</li>
 *   <li>{@code delivering} = PICKED_UP + DELIVERING</li>
 *   <li>{@code completed} = DELIVERED</li>
 * </ul></p>
 *
 * <p>{@code deliveries}: status 그룹 필터 + page (size 고정 20, page 0-base).</p>
 */
public record RiderInternalMonitorRes(
        Summary summary,
        List<DeliveryRow> deliveries
) {
    public record Summary(long waiting, long assigned, long delivering, long completed) {}

    public record DeliveryRow(
            String deliveryNo,
            String orderId,
            Long riderNo,
            String status,
            Integer baseFee,
            Integer extraFee,
            LocalDateTime assignedAt,
            LocalDateTime deliveredAt
    ) {}
}
