package com.coolonlineshop.order.client;

public interface CartClient {

    CartResponse getCart(Long userId);

    void clearCart(Long userId);
}
