package com.coolonlineshop.order.exception;

public class ProductQuantityNotAvailableException extends RuntimeException {

    public ProductQuantityNotAvailableException(Long productId, Integer requestedQuantity, Integer availableQuantity) {
        super("Product with id %d has only %d items available, requested %d".formatted(
                productId,
                availableQuantity,
                requestedQuantity
        ));
    }
}
