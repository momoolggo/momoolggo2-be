package com.green.mmg.main.internal.dto;

import java.time.LocalDateTime;

/**
 * rider → main 배달 완료 처리 Body — interfaces.md §2.2.
 *
 * <p>{@code deliveredMethod}: DIRECT / CUSTOMER_REQUEST / CUSTOMER_ABSENT (정정 10, ADR-004).
 * {@code deliveredPhotoUrl}: 사진 URL nullable (Q-A5 (나) Phase 6+ tech-debt — files/upload 미구현 시 null).
 * {@code completedAt}: 수신 후 무시 (Q-A8.e-1 (나), orders.completed_at 컬럼 부재 — tech-debt 등재).</p>
 */
public record DeliveryCompleteReq(
        String deliveryNo,
        Long riderNo,
        String deliveredMethod,
        String deliveredPhotoUrl,
        LocalDateTime completedAt
) {
}
