package com.green.mmg.main.owner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Slf4j
public class OwnerOrderSseService {
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long storeId) {
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000); // 1시간

        emitters.computeIfAbsent(storeId, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(storeId, emitter));
        emitter.onTimeout(() -> removeEmitter(storeId, emitter));
        emitter.onError(e -> removeEmitter(storeId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) {
            removeEmitter(storeId, emitter);
            log.debug("사장 주문 SSE 연결 확인 실패 storeId={} error={}", storeId, e.getMessage());
        }
            return emitter;
    }

    public void sendNewOrder(Long storeId, Object data) {
        sendEvent(storeId, "new-order", data);
    }

    /**
     * 2026-05-25 9건 트랙 — 라이더 배차 수락/픽업/배달완료 시 사장 OrderList 자동 갱신.
     */
    public void sendOrderStateChanged(Long storeId, Object data) {
        sendEvent(storeId, "order-state-changed", data);
    }

    private void sendEvent(Long storeId, String eventName, Object data) {
        List<SseEmitter> storeEmitters = emitters.get(storeId);
        if (storeEmitters == null || storeEmitters.isEmpty()) return;
        for (SseEmitter emitter : storeEmitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                removeEmitter(storeId, emitter);
                log.debug("사장 SSE 전송 실패 storeId={} event={} error={}", storeId, eventName, e.getMessage());
            }
        }
    }

    private void removeEmitter(Long storeId, SseEmitter emitter) {
        List<SseEmitter> storeEmitters = emitters.get(storeId);

        if (storeEmitters == null) {
            return;
        }
        storeEmitters.remove(emitter);

        if (storeEmitters.isEmpty()) {
            emitters.remove(storeId);
        }
    }
}
