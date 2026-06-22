package com.coolonlineshop.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        BigDecimal productPrice,
        Integer quantity
) {
}
