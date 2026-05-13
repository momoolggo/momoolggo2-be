package com.green.mmg.admin.dto.feign;

import java.time.LocalDateTime;
import java.util.List;

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
            LocalDateTime deliveredAt,
            String storeName,
            Integer elapsedMinutes,
            Double distanceKm
    ) {}
}