package com.orderflow.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentListener {
    /** Mock gateway rule: amounts above this are declined (lets you demo the saga rollback). */
    static final BigDecimal LIMIT = new BigDecimal("100000");

    private final PaymentRepository payments;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;

    PaymentListener(PaymentRepository payments, KafkaTemplate<String, String> kafka, ObjectMapper mapper) {
        this.payments = payments;
        this.kafka = kafka;
        this.mapper = mapper;
    }

    /** Saga step 2: charge the customer. */
    @KafkaListener(topics = "inventory-reserved")
    @Transactional
    public void onInventoryReserved(String message) throws Exception {
        OrderEvent e = mapper.readValue(message, OrderEvent.class);
        if (payments.existsById(e.orderId())) return; // duplicate delivery: already handled

        if (e.amount().compareTo(LIMIT) > 0) {
            payments.save(new Payment(e.orderId(), e.amount(), "FAILED"));
            kafka.send("payment-failed", e.orderId(), mapper.writeValueAsString(e.withReason("Payment declined: amount over limit")));
        } else {
            payments.save(new Payment(e.orderId(), e.amount(), "COMPLETED"));
            kafka.send("payment-completed", e.orderId(), mapper.writeValueAsString(e));
        }
    }
}
