package com.orderflow.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    record CreateOrder(String productId, int quantity, BigDecimal amount) {}

    private final OrderService service;
    private final OrderRepository repo;
    private final SseHub hub;

    OrderController(OrderService service, OrderRepository repo, SseHub hub) {
        this.service = service;
        this.repo = repo;
        this.hub = hub;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Order create(@RequestBody CreateOrder req) throws JsonProcessingException {
        if (req.quantity() <= 0) throw new IllegalArgumentException("quantity must be positive");
        Order order = service.create(req.productId(), req.quantity(), req.amount());
        hub.broadcast(order);
        return order;
    }

    @GetMapping
    public List<Order> list() {
        return repo.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @GetMapping("/stream")
    public SseEmitter stream() {
        return hub.subscribe();
    }

    @Configuration
    static class Cors {
        @Bean
        WebMvcConfigurer corsConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addCorsMappings(CorsRegistry r) {
                    r.addMapping("/api/**").allowedOrigins("*").allowedMethods("*");
                }
            };
        }
    }
}
