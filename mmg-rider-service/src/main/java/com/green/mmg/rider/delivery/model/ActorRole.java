package com.green.mmg.rider.delivery.model;

/**
 * delivery_log 변경 주체 역할.
 *
 * <ul>
 *   <li>{@link #RIDER} — 라이더 본인 (배달 흐름 정상)</li>
 *   <li>{@link #SYSTEM} — 자동 처리 (예: 최초 INSERT, 자동 재할당)</li>
 *   <li>{@link #ADMIN} — 관리자 강제 변경 (Phase 5-R7 CANCELLED 등)</li>
 * </ul>
 *
 * <p>R2-b tech-debt 처리 — DeliveryLog.actor_role String → enum 마이그레이션 (2026-05-10).
 * 학원 DB 데이터 0건이라 마이그레이션 영향 0.</p>
 */
public enum ActorRole {
    RIDER,
    SYSTEM,
    ADMIN
}
