package com.green.mmg.rider.delivery.dto;

import com.green.mmg.rider.delivery.model.DeliveryCancelReason;

/**
 * POST /api/rider/order/{deliveryNo}/cancel 요청 Body — R6-cancel.
 *
 * <p>{@code reason} 필수 (decision-#34 (가) enum 3종 단독 채택).
 * NoticeService validation 패턴 일관 — Service에서 null 검증.</p>
 */
public record DeliveryCancelReq(DeliveryCancelReason reason) {
}
