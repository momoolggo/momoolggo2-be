package com.green.mmg.rider.internal.dto;

import java.time.LocalDateTime;

/**
 * Main → Rider 배차 요청 응답 — interfaces.md §1.1.
 *
 * <p>{@code assigned} = true 항상 (실패 시 BusinessException + GlobalExceptionHandler 동적 매핑).
 * {@code deliveryNo}는 Service 자동 생성 (5자리 timestamp + 3자리 영문 형식, interfaces.md 박제 예시 일관).</p>
 */
public record RiderInternalAssignRes(
        boolean assigned,
        String deliveryNo,
        Long riderNo,
        LocalDateTime assignedAt
) {
}
