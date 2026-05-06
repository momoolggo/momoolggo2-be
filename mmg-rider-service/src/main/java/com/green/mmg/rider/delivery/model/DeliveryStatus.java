package com.green.mmg.rider.delivery.model;

/**
 * 배달 상태 (ADR-004, 7개 상태 + 화이트리스트).
 *
 * <ul>
 *   <li>{@link #WAITING_ASSIGN} — 배차 대기 (가용 라이더에 할당 대기, rider_no NULL)</li>
 *   <li>{@link #ASSIGNED} — 배차 완료, 라이더 수락 대기</li>
 *   <li>{@link #ARRIVED_AT_STORE} — 가게 도착 (Figma 정정 2)</li>
 *   <li>{@link #AWAITING_PICKUP} — 픽업 대기 (Figma 정정 2)</li>
 *   <li>{@link #PICKED_UP} — 픽업 완료</li>
 *   <li>{@link #DELIVERING} — 이동 중</li>
 *   <li>{@link #DELIVERED} — 배달 완료 (terminal)</li>
 * </ul>
 *
 * <p>화이트리스트 전이 규칙 + orders.delivery_state 매핑은 R3 DeliveryService 진입 시 도입.</p>
 */
public enum DeliveryStatus {
    WAITING_ASSIGN,
    ASSIGNED,
    ARRIVED_AT_STORE,
    AWAITING_PICKUP,
    PICKED_UP,
    DELIVERING,
    DELIVERED
}
