package com.coolonlineshop.cart.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog-service")
public record CatalogServiceProperties(
        String baseUrl
) {
}
