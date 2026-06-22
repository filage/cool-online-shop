package com.coolonlineshop.cart.client;

public interface CatalogClient {

    void validateProductAvailable(Long productId, Integer requestedQuantity);
}
