package com.coolonlineshop.order.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "cart-service")
public record CartServiceProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout
) {
}
