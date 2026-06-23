package com.coolonlineshop.catalog.security;

import com.coolonlineshop.catalog.exception.CatalogWriteForbiddenException;

public final class CatalogWriteAuthorizer {

    private static final String ADMIN_ROLE = "ADMIN";

    private CatalogWriteAuthorizer() {
    }

    public static void requireAdmin(String userRole) {
        if (!ADMIN_ROLE.equals(userRole)) {
            throw new CatalogWriteForbiddenException();
        }
    }
}
