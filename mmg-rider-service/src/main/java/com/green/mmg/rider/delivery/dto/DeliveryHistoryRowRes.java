package com.green.mmg.rider.delivery.dto;

import com.green.mmg.rider.delivery.model.Delivery;

import java.time.LocalDateTime;

/**
 * GET /api/rider/order/completed 응답 row — R9 배달내역.
 *
 * <p>DeliveryWaitingRowRes(진행 중) vs DeliveryHistoryRowRes(완료): deliveredAt 명시 + 결제 합계 노출.
 * Figma 170148 박제 일관 (배달내역 5건 카드).</p>
 */
public record DeliveryHistoryRowRes(
        String deliveryNo,
        String orderId,
        String pickupAddress,
        String deliveryAddress,
        Integer baseFee,
        Integer extraFee,
        Integer totalFee,
        LocalDateTime deliveredAt
) {
    public static DeliveryHistoryRowRes from(Delivery d) {
        int base = d.getBaseFee() != null ? d.getBaseFee() : 0;
        int extra = d.getExtraFee() != null ? d.getExtraFee() : 0;
        return new DeliveryHistoryRowRes(
                d.getDeliveryNo(),
                d.getOrderId(),
                d.getPickupAddress(),
                d.getDeliveryAddress(),
                base, extra, base + extra,
                d.getDeliveredAt()
        );
    }
}
