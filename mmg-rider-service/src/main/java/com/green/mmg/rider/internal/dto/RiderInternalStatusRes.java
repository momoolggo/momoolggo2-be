package com.green.mmg.rider.internal.dto;

/**
 * Main/Admin → Rider 상태 확인 응답 — interfaces.md §1.3.
 *
 * <p>{@code status}는 RiderStatus enum의 .name() 직렬화 (PENDING/ACTIVE/EATING/SUSPENDED).
 * {@code currentDeliveryNo}는 진행 중 배달이 없으면 null (DeliveryRepository.findFirstByRiderNoAndStatusInOrderByAssignedAtDesc).</p>
 */
public record RiderInternalStatusRes(
        Long riderNo,
        String status,
        String currentDeliveryNo
) {
}
