package com.coolonlineshop.cart.exception;

public class CatalogServiceUnavailableException extends RuntimeException {

    public CatalogServiceUnavailableException() {
        super("Catalog service is unavailable");
    }
}
