package com.orderflow.order;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Row written in the same DB transaction as the order (transactional outbox). */
@Entity
@Table(name = "outbox")
@Getter @Setter @NoArgsConstructor
public class OutboxEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    private String topic;
    private String msgKey;
    @Column(columnDefinition = "text") private String payload;
    private boolean sent;
    private Instant createdAt = Instant.now();
}
