package com.coolonlineshop.cart.dto;

public record CartItemResponse(
        Long userId,
        Long productId,
        Integer quantity
) {
}
