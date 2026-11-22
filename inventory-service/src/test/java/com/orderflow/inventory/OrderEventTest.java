package com.orderflow.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrderEventTest {
    @Test
    void withReasonKeepsOtherFields() {
        var e = new OrderEvent("e1", "o1", "p1", 2, BigDecimal.TEN, null).withReason("boom");
        assertEquals("boom", e.reason());
        assertEquals("o1", e.orderId());
    }
}
