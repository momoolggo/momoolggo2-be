package com.green.mmg.main.order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
public class OrderDeliverySseService {
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long orderId) {
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000);

        emitters.computeIfAbsent(orderId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeEmitter(orderId, emitter));
        emitter.onError(e -> removeEmitter(orderId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) {
            removeEmitter(orderId, emitter);
        }

        return emitter;
    }

    public void sendDeliveryStatus(Long orderId, Object data) {
        List<SseEmitter> orderEmitters = emitters.get(orderId);

        if (orderEmitters == null || orderEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : orderEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("delivery-status")
                        .data(data));
            } catch (Exception e) {
                removeEmitter(orderId, emitter);
            }
        }
    }

    private void removeEmitter(Long orderId, SseEmitter emitter) {
        List<SseEmitter> orderEmitters = emitters.get(orderId);

        if (orderEmitters == null) {
            return;
        }

        orderEmitters.remove(emitter);

        if (orderEmitters.isEmpty()) {
            emitters.remove(orderId);
        }
    }
}