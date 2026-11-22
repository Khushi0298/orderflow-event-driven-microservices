package com.orderflow.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.repository.JpaRepository;

@Entity
@Getter @Setter @NoArgsConstructor
class Product {
    @Id private String id;
    private String name;
    private BigDecimal price;
    private int stock;

    Product(String id, String name, BigDecimal price, int stock) {
        this.id = id; this.name = name; this.price = price; this.stock = stock;
    }
}

@Entity
@Getter @Setter @NoArgsConstructor
class Reservation {
    @Id private String orderId;
    private String productId;
    private int quantity;
    private boolean released;

    Reservation(String orderId, String productId, int quantity) {
        this.orderId = orderId; this.productId = productId; this.quantity = quantity;
    }
}

/** One row per handled event: the primary key makes reprocessing a no-op (idempotent consumer). */
@Entity
@Getter @Setter @NoArgsConstructor
class ProcessedEvent {
    @Id private String id;
    ProcessedEvent(String id) { this.id = id; }
}

interface ProductRepository extends JpaRepository<Product, String> {}
interface ReservationRepository extends JpaRepository<Reservation, String> {}
interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {}
