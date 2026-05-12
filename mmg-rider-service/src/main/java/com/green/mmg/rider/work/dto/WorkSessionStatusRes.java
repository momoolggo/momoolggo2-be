package com.green.mmg.rider.work.dto;

/**
 * PUT /api/rider/status 응답 — 전환 후 상태 반환.
 */
public record WorkSessionStatusRes(Long riderNo, String status) {
}
