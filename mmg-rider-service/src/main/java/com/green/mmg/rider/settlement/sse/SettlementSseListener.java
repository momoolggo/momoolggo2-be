package com.green.mmg.rider.settlement.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 commit 후 SSE 푸시 — DB 변경 확정 후에만 라이더 화면 갱신 (SSE 자동화 트랙, 2026-05-21).
 *
 * <p>AFTER_COMMIT 사용 이유 — 푸시 후 DB 롤백 시 라이더 화면과 실제 데이터 불일치 회피.
 * 푸시 실패는 DB 영향 X (별 트랜잭션).</p>
 */
@Component
@RequiredArgsConstructor
public class SettlementSseListener {

    private final SettlementSseRegistry registry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSettlementUpdated(SettlementUpdatedEvent event) {
        registry.send(event.riderNo(), event.payload());
    }
}
