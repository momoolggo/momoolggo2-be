package com.green.mmg.rider.settlement.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * SettlementSseRegistry 단위 (SSE 자동화 트랙, 2026-05-21).
 *
 * <p>SseEmitter는 mock — 실 emitter는 HTTP 컨텍스트 의존 (complete/onCompletion 콜백).
 * register / 재등록 시 기존 complete / send / 미등록 skip / IOException cleanup.</p>
 */
class SettlementSseRegistryTest {

    @Test
    @DisplayName("register: 신규 emitter → size=1 + 동일 emitter 반환")
    void register_new() {
        SettlementSseRegistry registry = new SettlementSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);

        SseEmitter returned = registry.register(10L, emitter);

        assertThat(returned).isSameAs(emitter);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("register: 동일 riderNo 재등록 → 기존 emitter complete() 호출 + size 그대로 1")
    void register_replacesExisting() {
        SettlementSseRegistry registry = new SettlementSseRegistry();
        SseEmitter previous = mock(SseEmitter.class);
        registry.register(10L, previous);

        SseEmitter latest = mock(SseEmitter.class);
        registry.register(10L, latest);

        verify(previous).complete();
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("send: 등록된 emitter에 SseEmitter.send 호출")
    void send_invokesEmitterSend() throws IOException {
        SettlementSseRegistry registry = new SettlementSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);
        registry.register(10L, emitter);

        registry.send(10L, "payload");

        verify(emitter).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("send: 미등록 riderNo → no-op (예외 X, size 변경 X)")
    void send_noEmitter_skips() {
        SettlementSseRegistry registry = new SettlementSseRegistry();

        registry.send(99L, "payload");

        assertThat(registry.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("send IOException: emitter cleanup + completeWithError")
    void send_ioException_cleansUp() throws IOException {
        SettlementSseRegistry registry = new SettlementSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);
        doThrow(new IOException("연결 끊김"))
                .when(emitter).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        registry.register(10L, emitter);

        registry.send(10L, "payload");

        verify(emitter).completeWithError(org.mockito.ArgumentMatchers.any(IOException.class));
        assertThat(registry.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("onCompletion 콜백 발화 시 registry cleanup (Spring이 timeout/complete 후 호출)")
    void cleanup_onCompletionCallback() {
        SettlementSseRegistry registry = new SettlementSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

        registry.register(10L, emitter);
        verify(emitter).onCompletion(captor.capture());
        assertThat(registry.size()).isEqualTo(1);

        // Spring이 response 닫힌 후 호출하는 콜백을 직접 trigger
        captor.getValue().run();

        assertThat(registry.size()).isEqualTo(0);
    }
}
