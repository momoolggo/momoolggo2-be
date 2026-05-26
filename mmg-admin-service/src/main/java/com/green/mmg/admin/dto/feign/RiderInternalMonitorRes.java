package com.green.mmg.admin.dto.feign;

import java.time.LocalDateTime;
import java.util.List;

public record RiderInternalMonitorRes(
        Summary summary,
        List<DeliveryRow> deliveries,
        int totalPages,
        long totalElements
) {
    public record Summary(long waiting, long assigned, long delivering, long completed) {}

    public record DeliveryRow(
            String deliveryNo,
            Long orderId,             // case-#34 정정 (rider Provider Long 박제 정합)
            Long riderNo,
            String status,
            Integer baseFee,
            Integer extraFee,
            LocalDateTime assignedAt,
            LocalDateTime deliveredAt,
            String storeName,
            Integer elapsedMinutes,
            Double distanceKm,
            String riderPhone         // 정산 시연 UX 트랙 #9 (2026-05-21)
    ) {}
}