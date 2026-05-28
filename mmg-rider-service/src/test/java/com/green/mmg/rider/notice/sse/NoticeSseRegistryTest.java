package com.green.mmg.rider.notice.sse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * NoticeSseRegistry 단위 (2026-05-28 트랙).
 *
 * <p>SettlementSseRegistryTest 박제 일관 — broadcast 패턴만 추가.
 * SseEmitter는 mock — 실 emitter는 HTTP 컨텍스트 의존.</p>
 */
class NoticeSseRegistryTest {

    @Test
    @DisplayName("register: 신규 emitter → size=1 + 동일 emitter 반환")
    void register_new() {
        NoticeSseRegistry registry = new NoticeSseRegistry();
        SseEmitter emitter = mock(SseEmitter.class);

        SseEmitter returned = registry.register(10L, emitter);

        assertThat(returned).isSameAs(emitter);
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("register: 동일 userNo 재등록 → 기존 emitter complete() 호출 + size 그대로 1")
    void register_replacesExisting() {
        NoticeSseRegistry registry = new NoticeSseRegistry();
        SseEmitter previous = mock(SseEmitter.class);
        registry.register(10L, previous);

        SseEmitter latest = mock(SseEmitter.class);
        registry.register(10L, latest);

        verify(previous).complete();
        assertThat(registry.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("broadcast: 2개 emitter 등록 후 → 둘 다 send 호출")
    void broadcast_invokesAllEmitters() throws IOException {
        NoticeSseRegistry registry = new NoticeSseRegistry();
        SseEmitter e1 = mock(SseEmitter.class);
        SseEmitter e2 = mock(SseEmitter.class);
        registry.register(10L, e1);
        registry.register(20L, e2);

        registry.broadcast("payload");

        verify(e1).send(any(SseEmitter.SseEventBuilder.class));
        verify(e2).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(registry.size()).isEqualTo(2);
    }

    @Test
    @DisplayName("broadcast: 등록 emitter 0개 → no-op (예외 X)")
    void broadcast_empty_noop() {
        NoticeSseRegistry registry = new NoticeSseRegistry();

        registry.broadcast("payload");

        assertThat(registry.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("broadcast IOException: 실패 emitter만 cleanup + completeWithError, 정상 emitter는 보존")
    void broadcast_ioException_cleansUpFailedOnly() throws IOException {
        NoticeSseRegistry registry = new NoticeSseRegistry();
        SseEmitter failed = mock(SseEmitter.class);
        SseEmitter healthy = mock(SseEmitter.class);
        doThrow(new IOException("연결 끊김"))
                .when(failed).send(any(SseEmitter.SseEventBuilder.class));
        registry.register(10L, failed);
        registry.register(20L, healthy);

        registry.broadcast("payload");

        verify(failed).completeWithError(any(IOException.class));
        verify(healthy).send(any(SseEmitter.SseEventBuilder.class));
        assertThat(registry.size()).isEqualTo(1);   // failed cleanup → 1개만 남음
    }

    @Test
    @DisplayName("onCompletion 콜백 발화 시 registry cleanup (Spring이 timeout/complete 후 호출)")
    void cleanup_onCompletionCallback() {
        NoticeSseRegistry registry = new NoticeSseRegistry();
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
