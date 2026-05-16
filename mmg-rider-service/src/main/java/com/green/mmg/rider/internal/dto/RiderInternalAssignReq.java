package com.green.mmg.rider.internal.dto;

/**
 * Main → Rider 배차 요청 Body — interfaces.md §1.1.
 *
 * <p>{@code customerPhone}는 평문 (D7-a, snapshot 보관). storePhone은 가게 전화 snapshot.
 * {@code storeNo / storeName} 등 일부 필드는 Delivery entity 매핑 X (Main 정보 전달용, R5 위치 추적
 * 시 활용 후보). {@code extraFee}도 R7 점주 추가 배달료 정책 확장 시 활용.</p>
 */
public record RiderInternalAssignReq(
        Long orderId,
        Long storeNo,
        String storeName,
        String storeAddress,
        Double storeLat,
        Double storeLng,
        String storePhone,
        String deliveryAddress,
        Double deliveryLat,
        Double deliveryLng,
        String customerPhone,
        Integer baseFee,
        Integer extraFee
) {
}
