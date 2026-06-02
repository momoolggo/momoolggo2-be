package com.green.mmg.rider.notice.sse;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 commit 후 SSE broadcast — DB 변경 확정 후에만 라이더 화면 갱신 (SettlementSseListener 박제 일관).
 *
 * <p>AFTER_COMMIT 사용 이유 — broadcast 후 DB 롤백 시 라이더 화면과 실제 데이터 불일치 회피.
 * broadcast 실패는 DB 영향 X (별 트랜잭션).</p>
 */
@Component
@RequiredArgsConstructor
public class NoticeSseListener {

    private final NoticeSseRegistry registry;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNoticeBroadcast(NoticeBroadcastEvent event) {
        registry.broadcast(event.payload());
    }
}
