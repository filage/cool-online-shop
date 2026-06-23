package com.coolonlineshop.order.dto;

import com.coolonlineshop.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        OrderStatus status,
        BigDecimal totalAmount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> items
) {
}
