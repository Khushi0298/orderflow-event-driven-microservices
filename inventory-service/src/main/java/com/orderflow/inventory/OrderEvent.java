package com.orderflow.inventory;

import java.math.BigDecimal;

/** Event payload shared by all topics (each service keeps its own copy to stay decoupled). */
public record OrderEvent(String eventId, String orderId, String productId, int quantity,
                         BigDecimal amount, String reason) {
    public OrderEvent withReason(String r) {
        return new OrderEvent(eventId, orderId, productId, quantity, amount, r);
    }
}
