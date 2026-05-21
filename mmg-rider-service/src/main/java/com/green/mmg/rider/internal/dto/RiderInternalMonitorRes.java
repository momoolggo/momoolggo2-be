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
        List<DeliveryRow> deliveries,
        int totalPages,
        long totalElements
) {
    public record Summary(long waiting, long assigned, long delivering, long completed) {}

    public record DeliveryRow(
            String deliveryNo,
            Long orderId,
            Long riderNo,
            String status,
            Integer baseFee,
            Integer extraFee,
            LocalDateTime assignedAt,
            LocalDateTime deliveredAt,
            String storeName,        // 정산 시연 UX 트랙 #6 (2026-05-21) — delivery.store_name 스냅샷
            Integer elapsedMinutes,
            Double distanceKm,       // 정산 시연 UX 트랙 #7 (2026-05-21) — Haversine 직선 km
            String riderPhone        // 정산 시연 UX 트랙 #9 (2026-05-21, 옵션 A) — rider.phone 스냅샷
    ) {}
}
