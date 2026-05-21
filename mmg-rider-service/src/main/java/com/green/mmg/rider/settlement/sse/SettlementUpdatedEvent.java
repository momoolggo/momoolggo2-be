package com.green.mmg.rider.settlement.sse;

import com.green.mmg.rider.settlement.dto.SettlementRowRes;

/**
 * recalculateThisWeek 호출 후 발행 — 트랜잭션 commit 시 SSE 리스너가 푸시 (SSE 자동화 트랙, 2026-05-21).
 *
 * <p>{@code payload}는 DTO 변환된 상태로 발행 (entity 의존성 회피, AFTER_COMMIT 시점 안전).</p>
 */
public record SettlementUpdatedEvent(Long riderNo, SettlementRowRes payload) {
}
