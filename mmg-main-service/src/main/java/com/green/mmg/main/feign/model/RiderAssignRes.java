package com.green.mmg.main.feign.model;

import java.time.LocalDateTime;

/**
 * rider → main 배차 응답 — interfaces.md §1.1 (Group 4 W-1 정정 2026-05-17).
 *
 * <p>rider 측 {@code RiderInternalAssignRes} 4 필드 1:1 일관 (case-#34 강제 패턴).
 * {@code riderNo}: 라이더 풀(WAITING_ASSIGN) 시 null, 강제 배차(ASSIGNED) 시 명시.
 * Group 2 DTO 패턴 일관 (Lombok class → record 전환, 분류 B 자율).</p>
 */
public record RiderAssignRes(
        boolean assigned,
        String deliveryNo,
        Long riderNo,
        LocalDateTime assignedAt
) {
}
