package com.coolonlineshop.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemCreateRequest(
        @NotNull @Positive Long productId,
        @NotBlank @Size(max = 255) String productName,
        @NotNull @Positive BigDecimal productPrice,
        @NotNull @Positive Integer quantity
) {
}
