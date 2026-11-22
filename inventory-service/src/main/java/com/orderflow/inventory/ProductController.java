package com.orderflow.inventory;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductRepository repo;

    ProductController(ProductRepository repo) { this.repo = repo; }

    @GetMapping
    public List<Product> list() { return repo.findAll(Sort.by("name")); }

    @Configuration
    static class Config {
        @Bean
        WebMvcConfigurer cors() {
            return new WebMvcConfigurer() {
                @Override
                public void addCorsMappings(CorsRegistry r) {
                    r.addMapping("/api/**").allowedOrigins("*").allowedMethods("*");
                }
            };
        }

        @Bean
        CommandLineRunner seed(ProductRepository repo) {
            return args -> {
                if (repo.count() > 0) return;
                repo.save(new Product("laptop", "Laptop Pro 14", new BigDecimal("60000"), 10));
                repo.save(new Product("phone", "Phone X", new BigDecimal("25000"), 25));
                repo.save(new Product("earbuds", "Wireless Earbuds", new BigDecimal("3000"), 50));
                repo.save(new Product("keyboard", "Mechanical Keyboard", new BigDecimal("4500"), 3));
            };
        }
    }
}
