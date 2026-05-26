package com.green.mmg.rider.settlement.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 라이더별 SSE Emitter 단일 보관소 (SSE 자동화 트랙, 2026-05-21).
 *
 * <p>같은 라이더 재접속 시 기존 emitter 종료 후 신규 등록 (다중 탭/네트워크 단절 후 재연결 안전).
 * timeout / 완료 / 오류 시 자동 cleanup.</p>
 */
@Slf4j
@Component
public class SettlementSseRegistry {

    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long riderNo, SseEmitter emitter) {
        SseEmitter previous = emitters.put(riderNo, emitter);
        if (previous != null) {
            previous.complete();
        }
        emitter.onCompletion(() -> emitters.remove(riderNo, emitter));
        emitter.onTimeout(() -> emitters.remove(riderNo, emitter));
        emitter.onError(e -> emitters.remove(riderNo, emitter));
        return emitter;
    }

    public void send(Long riderNo, Object payload) {
        SseEmitter emitter = emitters.get(riderNo);
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event().name("settlement-updated").data(payload));
        } catch (IOException e) {
            log.warn("SSE send 실패 — riderNo={} cleanup", riderNo);
            emitters.remove(riderNo, emitter);
            emitter.completeWithError(e);
        }
    }

    /** 테스트/모니터링용 — 활성 emitter 수 */
    public int size() {
        return emitters.size();
    }
}
