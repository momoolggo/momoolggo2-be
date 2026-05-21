package com.green.mmg.rider.settlement.sse;

import com.green.mmg.rider.settlement.dto.SettlementRowRes;
import com.green.mmg.rider.settlement.model.SettlementStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SettlementSseListener 단위 (SSE 자동화 트랙, 2026-05-21).
 *
 * <p>이벤트 수신 시 registry.send 호출 검증.</p>
 */
class SettlementSseListenerTest {

    @Test
    @DisplayName("onSettlementUpdated: event.payload → registry.send(riderNo, payload)")
    void listener_invokesRegistrySend() {
        SettlementSseRegistry registry = mock(SettlementSseRegistry.class);
        SettlementSseListener listener = new SettlementSseListener(registry);
        SettlementRowRes payload = new SettlementRowRes(
                1L, 10L, LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 24),
                3, 500, 30000, 0, 3000, 891, 5000, 21109,
                SettlementStatus.PENDING, null, null);

        listener.onSettlementUpdated(new SettlementUpdatedEvent(10L, payload));

        verify(registry).send(10L, payload);
    }
}
