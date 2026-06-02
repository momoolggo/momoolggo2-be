package com.green.mmg.rider.notice.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 라이더 공지 SSE Emitter 보관소 (2026-05-28 트랙).
 *
 * <p>SettlementSseRegistry 박제 일관 — 사용자(userNo)별 단일 emitter, 재접속 시 기존 close 후 신규 등록.
 * timeout / 완료 / 오류 시 자동 cleanup.</p>
 *
 * <p>SettlementSseRegistry와 차이점: notice는 ALL/RIDER 가시성으로 broadcast(모든 emitter)
 * 사용자 1명 send는 SPECIFIC 미래 확장(R9) 대비 보존.</p>
 */
@Slf4j
@Component
public class NoticeSseRegistry {

    private final ConcurrentMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(Long userNo, SseEmitter emitter) {
        SseEmitter previous = emitters.put(userNo, emitter);
        if (previous != null) {
            previous.complete();
        }
        emitter.onCompletion(() -> emitters.remove(userNo, emitter));
        emitter.onTimeout(() -> emitters.remove(userNo, emitter));
        emitter.onError(e -> emitters.remove(userNo, emitter));
        return emitter;
    }

    /** ALL/RIDER 타겟 — 모든 접속 라이더에게 push. 송신 실패 emitter는 cleanup. */
    public void broadcast(Object payload) {
        emitters.forEach((userNo, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notice-new").data(payload));
            } catch (IOException e) {
                log.warn("notice SSE send 실패 — userNo={} cleanup", userNo);
                emitters.remove(userNo, emitter);
                emitter.completeWithError(e);
            }
        });
    }

    /** 테스트/모니터링용 — 활성 emitter 수 */
    public int size() {
        return emitters.size();
    }
}
