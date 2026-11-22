package com.orderflow.order;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Pushes order updates to every connected browser (Server-Sent Events, no polling). */
@Component
public class SseHub {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    public void broadcast(Order order) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("order").data(order));
            } catch (Exception ex) {
                emitters.remove(emitter);
            }
        }
    }
}
