package com.shopflow.orderservice.model;

public enum OrderStatus {
    PENDING,    // Beklemede
    CONFIRMED,  // Onaylandı
    SHIPPED,    // Kargoya verildi
    DELIVERED,  // Teslim edildi
    CANCELLED   // İptal edildi
}
