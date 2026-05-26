package com.green.mmg.main.internal.dto;

import java.time.LocalDateTime;

/**
 * rider → main 배달 상태 변경 알림 Body — interfaces.md §2.1.
 *
 * <p>{@code deliveryStatus}: 7값 enum String (ADR-004) — WAITING_ASSIGN/ASSIGNED/ARRIVED_AT_STORE/
 * AWAITING_PICKUP/PICKED_UP/DELIVERING/DELIVERED. main 측은 String 수신 후 ADR-004 매핑(1/2/3) 적용.
 * 신규 enum 도입 X (영역 분리 + case-#34 일관).</p>
 *
 * <p>{@code riderNo}: 변경 주체. main은 감사/추적용으로 활용 (orders.rider_no UPDATE는 별 영역, §8 배차).</p>
 */
public record DeliveryStatusUpdateReq(
        String deliveryStatus,
        Long riderNo,
        LocalDateTime changedAt
) {
}
