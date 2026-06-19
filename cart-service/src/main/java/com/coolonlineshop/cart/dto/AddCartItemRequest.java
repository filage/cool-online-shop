package com.coolonlineshop.cart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartItemRequest(
        @NotNull @Positive Long userId,
        @NotNull @Positive Long productId,
        @NotNull @Positive Integer quantity
) {
}
