package com.coolonlineshop.order.client;

import java.math.BigDecimal;

public record CatalogProductResponse(
        Long id,
        String name,
        BigDecimal price,
        Integer availableQuantity
) {
}
