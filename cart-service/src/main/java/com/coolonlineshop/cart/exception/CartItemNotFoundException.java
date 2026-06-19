package com.coolonlineshop.cart.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long userId, Long productId) {
        super("Cart item with product id " + productId + " for user id " + userId + " not found");
    }
}
