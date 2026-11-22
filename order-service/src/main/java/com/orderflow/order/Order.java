package com.orderflow.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order {
    @Id private String id;
    private String productId;
    private int quantity;
    private BigDecimal amount;
    /** PENDING -> STOCK_RESERVED -> CONFIRMED, or CANCELLED at any point. */
    private String status;
    private String reason;
    private Instant createdAt;
}
