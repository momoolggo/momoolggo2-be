package com.green.mmg.rider.settlement.model;

/**
 * 정산 상태 — ADR-002 line 171 + ADR-007 D10-b (admin 수동 confirm).
 *
 * <ul>
 *   <li>{@link #PENDING}: 주간 집계 직후 INSERT 시점 default — admin 검토 대기</li>
 *   <li>{@link #CONFIRMED}: admin이 검토 후 confirm 버튼 클릭 시 전환 (confirmed_by_admin_no/confirmed_at 기록)</li>
 * </ul>
 *
 * <p>DeliveryStatus 재사용 X — 의미 다름 (배달 상태 vs 정산 상태).</p>
 */
public enum SettlementStatus {
    PENDING,
    CONFIRMED
}
