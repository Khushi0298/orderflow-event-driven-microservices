package com.orderflow.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class InventoryListener {
    private final ProductRepository products;
    private final ReservationRepository reservations;
    private final ProcessedEventRepository processed;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    InventoryListener(ProductRepository products, ReservationRepository reservations,
                      ProcessedEventRepository processed, KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.products = products;
        this.reservations = reservations;
        this.processed = processed;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    /** Saga step 1: reserve stock, then announce the result. */
    @KafkaListener(topics = "order-created")
    @Transactional
    public void onOrderCreated(String message) throws Exception {
        OrderEvent e = mapper.readValue(message, OrderEvent.class);
        if (!markProcessed("created:" + e.orderId())) return;

        Product p = products.findById(e.productId()).orElse(null);
        if (p == null || p.getStock() < e.quantity()) {
            String reason = p == null ? "Unknown product" : "Only " + p.getStock() + " in stock";
            send("inventory-failed", e.withReason(reason));
            return;
        }
        p.setStock(p.getStock() - e.quantity());
        reservations.save(new Reservation(e.orderId(), e.productId(), e.quantity()));
        send("inventory-reserved", e);
    }

    /** Compensating action: payment failed, so put the stock back. */
    @KafkaListener(topics = "payment-failed")
    @Transactional
    public void onPaymentFailed(String message) throws Exception {
        OrderEvent e = mapper.readValue(message, OrderEvent.class);
        if (!markProcessed("release:" + e.orderId())) return;

        reservations.findById(e.orderId()).filter(r -> !r.isReleased()).ifPresent(r -> {
            products.findById(r.getProductId()).ifPresent(p -> p.setStock(p.getStock() + r.getQuantity()));
            r.setReleased(true);
        });
    }

    private boolean markProcessed(String key) {
        if (processed.existsById(key)) return false;
        processed.save(new ProcessedEvent(key));
        return true;
    }

    private void send(String topic, OrderEvent e) throws Exception {
        kafka.send(topic, e.orderId(), mapper.writeValueAsString(e));
    }
}
