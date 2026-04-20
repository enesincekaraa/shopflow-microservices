package com.shopflow.orderservice.service;

import com.shopflow.orderservice.client.CustomerClient;
import com.shopflow.orderservice.client.ProductClient;
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

    private OrderResponse toResponse(Order order,
                                     CustomerClient.CustomerResponse customer) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCustomerId(order.getCustomerId());
        response.setCustomerName(customer.getName());
        response.setStatus(order.getStatus());
        response.setTotalAmount(order.getTotalAmount());
        response.setCreatedAt(order.getCreatedAt());

        // items null veya boşsa boş liste dön
        if (order.getItems() == null || order.getItems().isEmpty()) {
            response.setItems(List.of());
            return response;
        }

        List<OrderResponse.OrderItemResponse> itemResponses = order.getItems()
                .stream()
                .map(item -> {
                    OrderResponse.OrderItemResponse ir = new OrderResponse.OrderItemResponse();
                    ir.setProductId(item.getProductId());
                    ir.setQuantity(item.getQuantity());
                    ir.setPriceAtOrder(item.getPriceAtOrder());
                    // Feign çağrısını try-catch ile sar
                    try {
                        var product = productClient.getProductById(item.getProductId());
                        ir.setProductName(product.getName());
                    } catch (Exception e) {
                        ir.setProductName("Ürün bilgisi alınamadı");
                    }
                    return ir;
                })
                .collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }

}