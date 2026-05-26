package com.green.mmg.rider.delivery.dto;

import com.green.mmg.rider.delivery.model.DeliveryStatus;

import java.time.LocalDateTime;

/**
 * Service 상태 전환 결과 — Controller가 Main 동기화 호출 시 활용 (R4 패턴 일관).
 *
 * <p>{@code riderNo}: unassignRider 호출 전 snapshot (reject 시 NULL 처리 *전* 값으로 Main에 알림).
 * Main이 어떤 라이더가 변경했는지 추적 가능.</p>
 */
public record DeliveryTransitionResult(
        Long orderId,
        DeliveryStatus newStatus,
        Long riderNo,
        LocalDateTime changedAt
) {
}
