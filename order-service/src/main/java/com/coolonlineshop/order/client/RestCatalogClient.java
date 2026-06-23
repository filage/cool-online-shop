package com.coolonlineshop.order.client;

import com.coolonlineshop.order.exception.CatalogServiceUnavailableException;
import com.coolonlineshop.order.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(catalogServiceProperties.connectTimeout());
        requestFactory.setReadTimeout(catalogServiceProperties.readTimeout());

        this.restClient = restClientBuilder
                .baseUrl(catalogServiceProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public CatalogProductResponse getProduct(Long productId) {
        try {
            CatalogProductResponse product = restClient.get()
                    .uri("/products/{productId}", productId)
                    .retrieve()
                    .body(CatalogProductResponse.class);

            if (product == null) {
                throw new CatalogServiceUnavailableException();
            }

            return product;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                throw new ProductNotFoundException(productId);
            }

            throw new CatalogServiceUnavailableException();
        } catch (RestClientException exception) {
            throw new CatalogServiceUnavailableException();
        }
    }
}
