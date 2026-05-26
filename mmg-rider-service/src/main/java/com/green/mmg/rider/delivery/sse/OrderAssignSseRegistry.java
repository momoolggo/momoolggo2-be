package com.green.mmg.rider.delivery.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 라이더 배차 알림 SSE Emitter 보관소 — broadcast 패턴 (2026-05-23 자잘 에러 트랙).
 *
 * <p>Settlement는 1:1 (riderNo → emitter) push, 본 Registry는 1:N broadcast — 새 배차/수락 알림을 활성 라이더 전원에게 동시 전송.
 * 같은 라이더 재접속 시 기존 emitter 종료 후 신규 등록 (다중 탭/네트워크 단절 후 재연결 안전, SettlementSseRegistry 패턴 일관).</p>
 */
@Slf4j
@Component
public class OrderAssignSseRegistry {

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

    /** 활성 라이더 전원에게 동일 payload 전송. IOException 발생 emitter는 즉시 cleanup. */
    public void broadcast(String eventName, Object payload) {
        emitters.forEach((riderNo, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException e) {
                log.warn("SSE broadcast 실패 — riderNo={} cleanup", riderNo);
                emitters.remove(riderNo, emitter);
                emitter.completeWithError(e);
            }
        });
    }

    /** 테스트/모니터링용 — 활성 emitter 수 */
    public int size() {
        return emitters.size();
    }
}
