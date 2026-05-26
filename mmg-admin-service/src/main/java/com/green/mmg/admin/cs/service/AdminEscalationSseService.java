package com.green.mmg.admin.cs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 관리자 챗봇 에스컬레이션 알림 SSE — 학원 발표 시연용 단순 박제.
 * 관리자 화면이 subscribe → main 챗봇 에스컬레이션 발생 시 실시간 푸시.
 *
 * <p>tech-debt: 운영용 분산 환경 / 인증 / 클러스터 분배는 Phase 6+ (Redis Pub/Sub 또는 STOMP).</p>
 */
@Slf4j
@Service
public class AdminEscalationSseService {

    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L;  // 30분
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long adminNo) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emitters.put(adminNo, emitter);
        emitter.onCompletion(() -> emitters.remove(adminNo));
        emitter.onTimeout(() -> emitters.remove(adminNo));
        emitter.onError(e -> emitters.remove(adminNo));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            emitters.remove(adminNo);
        }
        return emitter;
    }

    /** 모든 구독 관리자에게 에스컬레이션 알림 푸시 (best-effort). */
    public void broadcast(Object payload) {
        emitters.forEach((adminNo, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("chatbot-escalated").data(payload));
            } catch (IOException e) {
                log.warn("SSE 푸시 실패 adminNo={} — emitter 제거", adminNo);
                emitters.remove(adminNo);
            }
        });
    }

    int activeSubscribers() {
        return emitters.size();
    }
}
