package com.green.mmg.rider.delivery.sse;

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
 * OrderAssignSseRegistry 단위 (자잘 에러 트랙, 2026-05-23).
 *
 * <p>SettlementSseRegistryTest 패턴 일관 — register / 재등록 시 기존 complete / broadcast 모두 송신 /
 * IOException emitter cleanup / onCompletion 콜백 cleanup.</p>
 */
class OrderAssignSseRegistryTest {

    @Test
    @DisplayName("register: 신규 emitter → size=1 + 동일 emitter 반환")
    void register_new() {
        OrderAssignSseRegistry registry = new OrderAssignSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);

        SseEmitter returned = registry.register(10L, emitter);

        assertThat(returned).isSameAs(emitter);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("register: 동일 riderNo 재등록 → 기존 emitter complete() 호출 + size 그대로 1")
    void register_replacesExisting() {
        OrderAssignSseRegistry registry = new OrderAssignSseRegistry();
        SseEmitter previous = mock(SseEmitter.class);
        registry.register(10L, previous);

        SseEmitter latest = mock(SseEmitter.class);
        registry.register(10L, latest);

        verify(previous).complete();
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("broadcast: 모든 등록 emitter에 send 호출 (활성 2명 → 각각 1회)")
    void broadcast_sendsToAll() throws IOException {
        OrderAssignSseRegistry registry = new OrderAssignSseRegistry();
        SseEmitter e10 = mock(SseEmitter.class);
        SseEmitter e20 = mock(SseEmitter.class);
        registry.register(10L, e10);
        registry.register(20L, e20);

        registry.broadcast("order-assigned", "payload");

        verify(e10).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        verify(e20).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("broadcast: 등록 emitter 0개 → no-op (예외 X)")
    void broadcast_empty_skips() {
        OrderAssignSseRegistry registry = new OrderAssignSseRegistry();

        registry.broadcast("order-assigned", "payload");

        assertThat(registry.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("broadcast IOException: 실패 emitter만 cleanup + 정상 emitter는 유지")
    void broadcast_ioException_cleansFailedOnly() throws IOException {
        OrderAssignSseRegistry registry = new OrderAssignSseRegistry();
        SseEmitter ok = mock(SseEmitter.class);
        SseEmitter broken = mock(SseEmitter.class);
        doThrow(new IOException("연결 끊김"))
                .when(broken).send(org.mockito.ArgumentMatchers.any(SseEmitter.SseEventBuilder.class));
        registry.register(10L, ok);
        registry.register(20L, broken);

        registry.broadcast("order-assigned", "payload");

        verify(broken).completeWithError(org.mockito.ArgumentMatchers.any(IOException.class));
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("onCompletion 콜백 발화 시 registry cleanup (Spring이 timeout/complete 후 호출)")
    void cleanup_onCompletionCallback() {
        OrderAssignSseRegistry registry = new OrderAssignSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);

        registry.register(10L, emitter);
        verify(emitter).onCompletion(captor.capture());
        assertThat(registry.size()).isEqualTo(1);

        captor.getValue().run();

        assertThat(registry.size()).isEqualTo(0);
    }
}
