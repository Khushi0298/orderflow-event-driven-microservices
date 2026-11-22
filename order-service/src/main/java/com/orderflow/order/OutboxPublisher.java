package com.orderflow.order;

import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outbox;
    private final KafkaTemplate<String, String> kafka;

    OutboxPublisher(OutboxRepository outbox, KafkaTemplate<String, String> kafka) {
        this.outbox = outbox;
        this.kafka = kafka;
    }

    /** Polls unsent rows; if Kafka is down they simply stay unsent and are retried (at-least-once). */
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void publish() {
        for (OutboxEvent e : outbox.findTop50BySentFalseOrderByIdAsc()) {
            try {
                kafka.send(e.getTopic(), e.getMsgKey(), e.getPayload()).get(5, TimeUnit.SECONDS);
                e.setSent(true);
            } catch (Exception ex) {
                log.warn("Kafka unavailable, will retry outbox row {}: {}", e.getId(), ex.getMessage());
                return;
            }
        }
    }
}
