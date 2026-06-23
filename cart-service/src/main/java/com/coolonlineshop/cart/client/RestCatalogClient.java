package com.coolonlineshop.cart.client;

import com.coolonlineshop.cart.exception.CatalogServiceUnavailableException;
import com.coolonlineshop.cart.exception.ProductNotFoundException;
import com.coolonlineshop.cart.exception.ProductQuantityNotAvailableException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
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
    public void validateProductAvailable(Long productId, Integer requestedQuantity) {
        try {
            CatalogProductResponse product = restClient.get()
                    .uri("/products/{productId}", productId)
                    .retrieve()
                    .body(CatalogProductResponse.class);

            if (product == null) {
                throw new IllegalStateException("Catalog service returned empty product response");
            }
            if (requestedQuantity > product.availableQuantity()) {
                throw new ProductQuantityNotAvailableException(
                        productId,
                        requestedQuantity,
                        product.availableQuantity()
                );
            }
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ProductNotFoundException(productId);
            }
            if (exception.getStatusCode().is5xxServerError()) {
                throw new CatalogServiceUnavailableException();
            }

            throw exception;
        } catch (RestClientException exception) {
            throw new CatalogServiceUnavailableException();
        }
    }
}
