package com.shopflow.orderservice.client;

import lombok.Data;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@FeignClient(name = "product-service", url = "${services.product-service.url}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ProductResponse getProductById(@PathVariable Long id);

    @PatchMapping("/api/products/{id}/decrease-stock")
    void decreaseStock(@PathVariable Long id, @RequestParam int quantity);

    @PatchMapping("/api/products/{id}/increase-stock")
    void increaseStock(@PathVariable Long id, @RequestParam int quantity);

    @PostMapping("/api/products/batch")
    List<ProductResponse> getProductByIds(@RequestBody List<Long> ids);

    @Data
    class ProductResponse{
        private Long id;
        private String name;
        private BigDecimal price;
        private Integer stock;
    }
}