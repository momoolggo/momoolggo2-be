package com.green.mmg.rider.delivery.sse;

/**
 * 배차 알림 이벤트 (2026-05-23 자잘 에러 트랙).
 *
 * <p>{@code type=ASSIGNED}: 새 배차 들어옴 (payload=DeliveryWaitingRowRes) — FE 자동 모달 popup 트리거.
 * {@code type=CLAIMED}: 어느 라이더가 잡음 (payload={@code String deliveryNo}) — FE는 같은 deliveryNo의 모달 자동 close + 목록에서 제거.</p>
 */
public record OrderAssignEvent(Type type, Object payload) {
    public enum Type {
        ASSIGNED,
        CLAIMED
    }
}
