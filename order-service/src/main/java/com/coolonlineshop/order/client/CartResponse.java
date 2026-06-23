package com.coolonlineshop.order.client;

import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItemResponse> items
) {
}
