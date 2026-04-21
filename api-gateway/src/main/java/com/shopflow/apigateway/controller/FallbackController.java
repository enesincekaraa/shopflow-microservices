package com.shopflow.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/product")
    public ResponseEntity<Map<String,Object>> productFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status",503,
                        "message","Ürün servisi şu an kullanılamıyor. Lütfen daha sonra tekrar deneyiniz.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }
    @GetMapping("/customer")
    public ResponseEntity<Map<String, Object>> customerFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "message", "Müşteri servisi şu an kullanılamıyor. Lütfen daha sonra tekrar deneyin.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

    @GetMapping("/order")
    public ResponseEntity<Map<String, Object>> orderFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "status", 503,
                        "message", "Sipariş servisi şu an kullanılamıyor. Lütfen daha sonra tekrar deneyin.",
                        "timestamp", LocalDateTime.now().toString()
                ));
    }

}
