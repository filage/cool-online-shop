package com.coolonlineshop.cart.client;

import com.coolonlineshop.cart.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestCatalogClient implements CatalogClient {

    private final RestClient restClient;

    public RestCatalogClient(
            RestClient.Builder restClientBuilder,
            CatalogServiceProperties catalogServiceProperties
    ) {
        this.restClient = restClientBuilder
                .baseUrl(catalogServiceProperties.baseUrl())
                .build();
    }

    @Override
    public void validateProductExists(Long productId) {
        try {
            restClient.get()
                    .uri("/products/{productId}", productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ProductNotFoundException(productId);
            }

            throw exception;
        }
    }
}
