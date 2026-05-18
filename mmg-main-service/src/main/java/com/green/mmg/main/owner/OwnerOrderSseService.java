package com.green.mmg.main.owner;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service

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
        } catch (IOException e) {
            removeEmitter(storeId, emitter);
        }
            return emitter;
    }

    public void sendNewOrder(Long storeId, Object data) {
        List<SseEmitter> storeEmitters = emitters.get(storeId);

        if(storeEmitters == null || storeEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : storeEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("new-order")
                        .data(data));
            } catch (IOException e) {
                removeEmitter(storeId, emitter);
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
