package com.green.mmg.rider.rider.model;

/**
 * 라이더 상태 (ADR-008 + Figma 정정 7).
 *
 * <ul>
 *   <li>{@link #PENDING} — 가입 직후, admin 승인 대기 (D11 auto-approve true 시 ACTIVE 즉시 전환)</li>
 *   <li>{@link #ACTIVE} — 배달 가능 ("배달중" UI 라벨)</li>
 *   <li>{@link #EATING} — 식사중, 신규 배차 차단 (D8-a, "식사중" UI 라벨)</li>
 *   <li>{@link #SUSPENDED} — admin 제재로 이용 제한</li>
 * </ul>
 */
public enum RiderStatus {
    PENDING,
    ACTIVE,
    EATING,
    SUSPENDED
}
