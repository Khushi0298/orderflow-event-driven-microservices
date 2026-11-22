package com.orderflow.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Listens to the saga's outcome events and updates the order status. */
@Component
public class StatusListener {
    private final OrderRepository orders;
    private final SseHub hub;
    private final ObjectMapper mapper;

    StatusListener(OrderRepository orders, SseHub hub, ObjectMapper mapper) {
        this.orders = orders;
        this.hub = hub;
        this.mapper = mapper;
    }

    @KafkaListener(topics = {"inventory-reserved", "inventory-failed", "payment-completed", "payment-failed"})
    @Transactional
    public void on(ConsumerRecord<String, String> record) throws Exception {
        OrderEvent event = mapper.readValue(record.value(), OrderEvent.class);
        Order order = orders.findById(event.orderId()).orElse(null);
        if (order == null || "CONFIRMED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            return; // unknown order, or a duplicate / late event: ignore (idempotent)
        }
        switch (record.topic()) {
            case "inventory-reserved" -> order.setStatus("STOCK_RESERVED");
            case "payment-completed" -> order.setStatus("CONFIRMED");
            default -> { // inventory-failed, payment-failed
                order.setStatus("CANCELLED");
                order.setReason(event.reason());
            }
        }
        orders.save(order);
        hub.broadcast(order);
    }
}
