package com.green.mmg.rider.delivery.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 commit 후 broadcast — DB 변경 확정 후에만 라이더 화면 갱신 (2026-05-23 자잘 에러 트랙).
 *
 * <p>SettlementSseListener 패턴 일관 — AFTER_COMMIT으로 DB 롤백 시 화면-실 데이터 불일치 회피.</p>
 */
@Component
@RequiredArgsConstructor
public class OrderAssignSseListener {

    private final OrderAssignSseRegistry registry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderAssign(OrderAssignEvent event) {
        String eventName = switch (event.type()) {
            case ASSIGNED -> "order-assigned";
            case CLAIMED -> "order-claimed";
        };
        registry.broadcast(eventName, event.payload());
    }
}
