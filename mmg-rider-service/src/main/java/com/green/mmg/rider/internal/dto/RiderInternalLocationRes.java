package com.green.mmg.rider.internal.dto;

import java.time.LocalDateTime;

/**
 * Main → Rider 위치 조회 응답 — interfaces.md §1.2.
 *
 * <p>R4 시점 = R5 Redis 인프라 부재로 항상 BusinessException(NOT_FOUND) 응답 (TTL 만료 또는 위치 송신 0회).
 * R5 진입 시 LocationService 신설 + Redis GET {@code rider:loc:{riderNo}} 호출 후 본 dto 반환.</p>
 */
public record RiderInternalLocationRes(
        Long riderNo,
        Double lat,
        Double lng,
        LocalDateTime updatedAt
) {
}
