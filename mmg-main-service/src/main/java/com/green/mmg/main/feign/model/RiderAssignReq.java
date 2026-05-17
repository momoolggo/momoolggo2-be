package com.green.mmg.main.feign.model;

/**
 * main → rider 배차 요청 Body — interfaces.md §1.1 (case-#33-후속 정정 2026-05-17, Q-A9.a (β+δ)).
 *
 * <p>rider 측 {@code RiderInternalAssignReq} 14 필드 1:1 일관 (case-#34 강제 패턴).
 * {@code riderNo}: null/0 = 라이더 풀 (WAITING_ASSIGN, 라이더가 R6 GET /api/rider/order/waiting로 선착순 수락).
 * 명시 = 강제 배차 (admin 시연 호환). team-handoff §8 박제 우선 (R6 종결 결과 반영).</p>
 *
 * <p>{@code storeId}→{@code storeNo} 명칭 정정 (Q-A8.c, interfaces.md 박제 일관).
 * {@code customerPhone}: D7-a 평문 박제 (tech-debt 등재). 신설 6 필드:
 * storeName/storeAddress/storePhone/customerPhone/baseFee/extraFee.</p>
 *
 * <p>Group 2 DTO 패턴 일관 (Lombok @Getter @Setter class → record 전환, 분류 B 자율).</p>
 */
public record RiderAssignReq(
        Long orderId,
        Long riderNo,
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
