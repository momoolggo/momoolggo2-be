package com.green.mmg.rider.internal.dto;

/**
 * Main → Rider 배차 요청 Body — interfaces.md §1.1 (case-#33-후속 정정 2026-05-17, Q-A9.a (β+δ)).
 *
 * <p>{@code riderNo}: null/0 = 라이더 풀 모델 (WAITING_ASSIGN 생성, 라이더가 R6 GET /api/rider/order/waiting로 선착순 수락).
 * 명시 = 강제 배차 (admin 시연 호환). team-handoff §8 박제 우선 적용 (R6 종결 결과 반영).
 * {@code customerPhone}는 평문 (D7-a, snapshot 보관). storePhone은 가게 전화 snapshot.
 * {@code storeNo / storeName} 등 일부 필드는 Delivery entity 매핑 X (Main 정보 전달용, R5 위치 추적
 * 시 활용 후보).</p>
 *
 * <p><b>baseFee / extraFee 정책 정정 (2026-05-19, figma-analysis #21):</b><br>
 * rider-service가 좌표 기반 Haversine으로 base=1500 고정 + extra=ceil(km)*1000 동적 산출.
 * main이 전달한 {@code baseFee} / {@code extraFee} 값은 <b>무시</b>됨. main 측은 계산 부담 없이
 * 0 또는 임의 값 전달 가능 (team-handoff §10 등재).</p>
 */
public record RiderInternalAssignReq(
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
