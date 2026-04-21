package com.shopflow.orderservice.service;

import com.shopflow.orderservice.client.CustomerClient;
import com.shopflow.orderservice.client.ProductClient;
import com.shopflow.orderservice.dto.CustomerOrderSummaryResponse;
import com.shopflow.orderservice.dto.OrderRequest;
import com.shopflow.orderservice.dto.OrderResponse;
import com.shopflow.orderservice.exception.ResourceNotFoundException;
import com.shopflow.orderservice.exception.BadRequestException;
import com.shopflow.orderservice.model.Order;
import com.shopflow.orderservice.model.OrderItem;
import com.shopflow.orderservice.model.OrderStatus;
import com.shopflow.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final CustomerClient customerClient;

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        log.info("Müşteri kontrol ediliyor: {}", request.getCustomerId());
        CustomerClient.CustomerResponse customer = customerClient.getCustomerById(request.getCustomerId());

        // 2. Ürünleri kontrol et ve sipariş kalemlerini oluştur
        Order order = new Order();
        order.setCustomerId(request.getCustomerId());
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {

            // product-service'e sor
            log.info("Ürün kontrol ediliyor: {}", itemReq.getProductId());
            ProductClient.ProductResponse product = productClient.getProductById(itemReq.getProductId());

            // Stok yeterli mi?
            if (product.getStock() < itemReq.getQuantity()) {
                throw new BadRequestException(
                        product.getName() + " için yeterli stok yok. Mevcut: " + product.getStock()
                );
            }

            // Sipariş kalemi oluştur
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setQuantity(itemReq.getQuantity());
            item.setPriceAtOrder(product.getPrice());  // anlık fiyatı kaydet

            items.add(item);
            total = total.add(product.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity())));
        }

        order.setItems(items);
        order.setTotalAmount(total);

        // 3. Siparişi kaydet
        Order saved = orderRepository.save(order);

        // 4. Stokları azalt — kayıt başarılıysa
        for (OrderRequest.OrderItemRequest itemReq : request.getItems()) {
            productClient.decreaseStock(itemReq.getProductId(), itemReq.getQuantity());
        }

        log.info("Sipariş oluşturuldu: {}", saved.getId());
        return toResponse(saved, customer);
    }

    @Transactional(readOnly = true)

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı: " + id));
        CustomerClient.CustomerResponse customer = customerClient.getCustomerById(order.getCustomerId());
        return toResponse(order, customer);
    }

    @Transactional(readOnly = true)

    public List<OrderResponse> getOrdersByCustomer(Long customerId) {
        CustomerClient.CustomerResponse customer = customerClient.getCustomerById(customerId);
        return orderRepository.findByCustomerId(customerId)
                .stream()
                .map(o -> toResponse(o, customer))
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı: " + id));
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        CustomerClient.CustomerResponse customer = customerClient.getCustomerById(saved.getCustomerId());
        return toResponse(saved, customer);
    }



    @Transactional(readOnly = true)
    public CustomerOrderSummaryResponse getCustomerOrderSummary(Long customerId) {

        var customer = customerClient.getCustomerById(customerId);

        List<Order> orders = orderRepository.findByCustomerId(customerId);

        BigDecimal totalSpent = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = orders.stream()
                .filter(o->o.getStatus().equals(OrderStatus.PENDING))
                .count();

        long deliveredOrders = orders.stream()
                .filter(o->o.getStatus().equals(OrderStatus.DELIVERED))
                .count();

        List<CustomerOrderSummaryResponse.OrderSummaryItem> summaryItems = orders.stream()
                .map(order -> {
                    var item = new CustomerOrderSummaryResponse.OrderSummaryItem();
                    item.setOrderId(order.getId());
                    item.setStatus(order.getStatus());
                    item.setAmount(order.getTotalAmount());
                    item.setCreatedAt(order.getCreatedAt());
                    item.setItemCount(order.getItems().size());
                    return item;
                }).toList();

        CustomerOrderSummaryResponse response = new CustomerOrderSummaryResponse();
        response.setCustomerId(customer.getId());
        response.setCustomerName(customer.getName());
        response.setCustomerEmail(customer.getEmail());
        response.setTotalOrders(orders.size());
        response.setTotalSpent(totalSpent);
        response.setPendingOrders(pendingOrders);
        response.setDeliveredOrders(deliveredOrders);
        response.setOrders(summaryItems);
        return response;

    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sipariş bulunamadı: " + orderId));

        if (order.getStatus() ==OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED)
        {
            throw new BadRequestException(
                    "Kargoya verilmiş veya teslim edilmiş sipariş iptal edilemez");
        }

        if (order.getStatus()==OrderStatus.CANCELLED){
            throw new BadRequestException("Sipariş zaten iptal edilmiş");
        }
        order.setStatus(OrderStatus.CANCELLED);
        Order saved =  orderRepository.save(order);

        for(OrderItem item :order.getItems()){
            try {
                productClient.increaseStock(item.getProductId(), item.getQuantity());
                log.info("Stok iade edildi — ürün: {}, miktar: {}",
                        item.getProductId(), item.getQuantity());
            }catch (Exception e){
                log.error("Stok iadesi başarısız — ürün: {}", item.getProductId());

            }
        }
        var customer = customerClient.getCustomerById(saved.getCustomerId());
        return toResponse(saved, customer);
    }


    private OrderResponse toResponse(Order order, CustomerClient.CustomerResponse customer) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(customer.getName());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());

        if (order.getItems() ==  null || order.getItems().isEmpty()) {
            response.setItems(List.of());
            return response;
        }

        List<Long> productsIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .toList();

        List<ProductClient.ProductResponse> products;
        try {
            products = productClient.getProductByIds(productsIds);
        }catch (Exception e){
            log.warn("Ürün bilgileri alınamadı: {}", e.getMessage());
            products = List.of();
        }

        Map<Long, ProductClient.ProductResponse> productMap = products.stream()
                .collect(Collectors.toMap(
                        ProductClient.ProductResponse::getId, p -> p
                ));

        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> {
                    OrderResponse.OrderItemResponse ir = new OrderResponse.OrderItemResponse();
                    ir.setProductId(item.getProductId());
                    ir.setQuantity(item.getQuantity());
                    ir.setPriceAtOrder(item.getPriceAtOrder()); // sipariş anındaki fiyat

                    // Map'ten O(1) ile bul — Feign çağrısı yok
                    ProductClient.ProductResponse product = productMap.get(item.getProductId());
                    ir.setProductName(product != null ? product.getName() : "Ürün bilgisi yok");
                    return ir;
                })
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;


    }

}