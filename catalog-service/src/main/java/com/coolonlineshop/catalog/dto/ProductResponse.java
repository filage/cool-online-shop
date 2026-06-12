package com.coolonlineshop.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        Integer availableQuantity,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
