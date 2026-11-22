package com.orderflow.payment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

/** orderId is the primary key, so an order can never be charged twice. */
@Entity
@Getter @Setter @NoArgsConstructor
class Payment {
    @Id private String orderId;
    private BigDecimal amount;
    private String status;

    Payment(String orderId, BigDecimal amount, String status) {
        this.orderId = orderId; this.amount = amount; this.status = status;
    }
}

interface PaymentRepository extends JpaRepository<Payment, String> {}
