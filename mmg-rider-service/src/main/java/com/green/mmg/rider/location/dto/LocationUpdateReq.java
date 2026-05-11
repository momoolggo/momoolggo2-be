package com.green.mmg.rider.location.dto;

/**
 * PUT /api/rider/location 요청 Body — ADR-005 박제 일관.
 *
 * <p>위도/경도만 받음 — updatedAt은 서버 시각 (LocationService.publishLocation).</p>
 */
public record LocationUpdateReq(Double lat, Double lng) {
}
