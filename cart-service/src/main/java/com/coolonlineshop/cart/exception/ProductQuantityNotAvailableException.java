package com.coolonlineshop.cart.exception;

public class ProductQuantityNotAvailableException extends RuntimeException {

    public ProductQuantityNotAvailableException(Long productId, Integer requestedQuantity, Integer availableQuantity) {
        super("Product with id " + productId + " has only " + availableQuantity
                + " available items, requested " + requestedQuantity);
    }
}
