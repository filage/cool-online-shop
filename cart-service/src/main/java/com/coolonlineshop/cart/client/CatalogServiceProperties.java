package com.coolonlineshop.cart.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "catalog-service")
public record CatalogServiceProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {

    public CatalogServiceProperties {
        if (connectTimeout == null) {
            connectTimeout = Duration.ofSeconds(5);
        }
        if (readTimeout == null) {
            readTimeout = Duration.ofSeconds(10);
        }
    }
}
