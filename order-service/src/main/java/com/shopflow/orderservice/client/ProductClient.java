package com.shopflow.orderservice.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.math.BigDecimal;

@FeignClient(name = "product-service", url = "${services.product-service.url}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);

    @PatchMapping("/api/products/{id}/decrease-stock")
    void decreaseStock(@PathVariable Long id, @RequestParam int quantity);

    @Data
    class ProductResponse{
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer stock;
    }
}