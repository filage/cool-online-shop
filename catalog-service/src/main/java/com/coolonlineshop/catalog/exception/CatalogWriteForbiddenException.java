package com.coolonlineshop.catalog.exception;

public class CatalogWriteForbiddenException extends RuntimeException {

    public CatalogWriteForbiddenException() {
        super("Catalog writes require ADMIN role");
    }
}
