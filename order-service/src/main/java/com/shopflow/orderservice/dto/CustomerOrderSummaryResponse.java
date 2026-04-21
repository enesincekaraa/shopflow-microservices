package com.shopflow.orderservice.dto;

import com.shopflow.orderservice.model.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CustomerOrderSummaryResponse {

    private Long customerId;
    private String customerName;
    private String customerEmail;

    private int totalOrders;
    private BigDecimal totalSpent;
    private long pendingOrders;
    private long deliveredOrders;

    private List<OrderSummaryItem> orders;

    @Data
    public static class OrderSummaryItem {
        private Long orderId;
        private OrderStatus status;
        private BigDecimal amount;
        private LocalDateTime createdAt;
        private int itemCount;

    }
}
