package com.green.mmg.rider.delivery.dto;

import java.time.LocalDateTime;

/**
 * GET /api/rider/order/waiting / inprogress 목록 row dto — interfaces.md §6.2.
 *
 * <p>R4 RiderInternalMonitorRes.DeliveryRow 패턴 일관 — 라이더 측 노출용 필드 별도 박제.
 * 가게 정보(주소/위경도/전화) + 손님 정보(주소/위경도/전화) + 배달료 + 시각.</p>
 */
public record DeliveryWaitingRowRes(
        String deliveryNo,
        String orderId,
        String status,
        String pickupAddress,
        Double pickupLat,
        Double pickupLng,
        String pickupPhone,
        String deliveryAddress,
        Double deliveryLat,
        Double deliveryLng,
        String customerPhone,
        Integer baseFee,
        Integer extraFee,
        LocalDateTime assignedAt
) {
}
