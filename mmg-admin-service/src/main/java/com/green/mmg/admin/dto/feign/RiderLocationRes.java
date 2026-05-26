package com.green.mmg.admin.dto.feign;

import java.time.LocalDateTime;

/**
 * Admin ← Rider 라이더 위치 응답 — interfaces.md §1.3 (Group 10 신설, 2026-05-17).
 *
 * <p>case-#34 일관: rider Provider {@code RiderInternalLocationRes} 1:1 매핑.
 * Admin 배달 관제 지도용 다건 조회 — TTL 살아있는 라이더만 (결정 (가)).</p>
 */
public record RiderLocationRes(
        Long riderNo,
        Double lat,
        Double lng,
        LocalDateTime updatedAt
) {
}
