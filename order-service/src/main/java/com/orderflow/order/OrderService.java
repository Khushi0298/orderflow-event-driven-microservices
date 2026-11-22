package com.orderflow.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    OrderService(OrderRepository orders, OutboxRepository outbox, ObjectMapper mapper) {
        this.orders = orders;
        this.outbox = outbox;
        this.mapper = mapper;
    }

    /** Order row and outbox row commit atomically; the OutboxPublisher sends it to Kafka. */
    @Transactional
    public Order create(String productId, int quantity, BigDecimal amount) throws JsonProcessingException {
        Order o = new Order();
        o.setId(UUID.randomUUID().toString());
        o.setProductId(productId);
        o.setQuantity(quantity);
        o.setAmount(amount);
        o.setStatus("PENDING");
        o.setCreatedAt(Instant.now());
        orders.save(o);

        OutboxEvent e = new OutboxEvent();
        e.setTopic("order-created");
        e.setMsgKey(o.getId());
        e.setPayload(mapper.writeValueAsString(
                new OrderEvent(UUID.randomUUID().toString(), o.getId(), productId, quantity, amount, null)));
        outbox.save(e);
        return o;
    }
}
