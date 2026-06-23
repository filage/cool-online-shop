package com.coolonlineshop.order.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "catalog-service")
public record CatalogServiceProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
