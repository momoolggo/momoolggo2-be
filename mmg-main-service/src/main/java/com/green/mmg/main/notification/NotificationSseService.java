package com.green.mmg.main.notification;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationSseService {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userNo) {
        SseEmitter emitter = new SseEmitter(60L * 60 * 1000);

        emitters.computeIfAbsent(userNo, key -> new CopyOnWriteArrayList<>())
                .add(emitter);

        emitter.onCompletion(() -> removeEmitter(userNo, emitter));
        emitter.onTimeout(() -> removeEmitter(userNo, emitter));
        emitter.onError(e -> removeEmitter(userNo, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
        } catch (Exception e) {
            removeEmitter(userNo, emitter);
        }

        return emitter;
    }

    public void sendNotification(Long userNo, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userNo);

        if (userEmitters == null || userEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : userEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(data));
            } catch (Exception e) {
                removeEmitter(userNo, emitter);
            }
        }
    }

    private void removeEmitter(Long userNo, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userNo);

        if (userEmitters == null) {
            return;
        }

        userEmitters.remove(emitter);

        if (userEmitters.isEmpty()) {
            emitters.remove(userNo);
        }
    }
}