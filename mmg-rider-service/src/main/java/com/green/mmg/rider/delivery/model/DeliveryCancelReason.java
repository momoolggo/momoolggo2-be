package com.green.mmg.rider.delivery.model;

/**
 * R6-cancel: 진행 중 배달 반려 사유 (decision-#34 (가) enum 3종 단독).
 *
 * <p>라이더가 ARRIVED_AT_STORE / AWAITING_PICKUP / PICKED_UP / DELIVERING 상태에서 cancel 시 필수.
 * delivery_log.reason 컬럼에 박제 (다른 transition NULL).</p>
 *
 * <ul>
 *   <li>{@link #ACCIDENT}: 사고 발생</li>
 *   <li>{@link #PERSONAL}: 개인적인 사유</li>
 *   <li>{@link #OTHER}: 기타 (자유 텍스트는 R8 진입 시 확장 결정 — decision-#34 (가) 일관)</li>
 * </ul>
 *
 * <p>R3-a ActorRole 패턴 일관 — `delivery/model/` 위치, NoticeService validation 패턴 일관.</p>
 */
public enum DeliveryCancelReason {
    ACCIDENT,
    PERSONAL,
    OTHER
}
