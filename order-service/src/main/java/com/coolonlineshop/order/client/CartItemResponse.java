package com.coolonlineshop.order.client;

public record CartItemResponse(
        Long userId,
        Long productId,
        Integer quantity
) {
}
