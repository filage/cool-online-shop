package com.coolonlineshop.order.service;

import com.coolonlineshop.order.dto.OrderCreateRequest;
import com.coolonlineshop.order.dto.OrderItemCreateRequest;
import com.coolonlineshop.order.dto.OrderItemResponse;
import com.coolonlineshop.order.dto.OrderResponse;
import com.coolonlineshop.order.entity.Order;
import com.coolonlineshop.order.entity.OrderItem;
import com.coolonlineshop.order.entity.OrderStatus;
import com.coolonlineshop.order.exception.OrderNotFoundException;
import com.coolonlineshop.order.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderResponse createOrder(OrderCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal totalAmount = calculateTotalAmount(request.items());

        Order order = new Order(
                request.userId(),
                OrderStatus.CREATED,
                totalAmount,
                now,
                now
        );

        request.items()
                .stream()
                .map(this::toOrderItem)
                .forEach(order::addItem);

        Order savedOrder = orderRepository.save(order);

        return toResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));

        return toResponse(order);
    }

    public List<OrderResponse> getOrdersByUserId(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private BigDecimal calculateTotalAmount(List<OrderItemCreateRequest> items) {
        return items.stream()
                .map(item -> item.productPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private OrderItem toOrderItem(OrderItemCreateRequest request) {
        return new OrderItem(
                request.productId(),
                request.productName(),
                request.productPrice(),
                request.quantity()
        );
    }

    private OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getUserId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getItems()
                        .stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductId(),
                item.getProductName(),
                item.getProductPrice(),
                item.getQuantity()
        );
    }
}
