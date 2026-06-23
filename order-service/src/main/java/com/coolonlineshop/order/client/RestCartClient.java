package com.coolonlineshop.order.client;

import com.coolonlineshop.order.exception.CartServiceUnavailableException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class RestCartClient implements CartClient {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final RestClient restClient;

    public RestCartClient(
            RestClient.Builder restClientBuilder,
            CartServiceProperties cartServiceProperties
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(cartServiceProperties.connectTimeout());
        requestFactory.setReadTimeout(cartServiceProperties.readTimeout());

        this.restClient = restClientBuilder
                .baseUrl(cartServiceProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public CartResponse getCart(Long userId) {
        try {
            CartResponse cart = restClient.get()
                    .uri("/cart")
                    .header(USER_ID_HEADER, userId.toString())
                    .retrieve()
                    .body(CartResponse.class);

            if (cart == null) {
                throw new CartServiceUnavailableException();
            }

            return cart;
        } catch (RestClientResponseException exception) {
            throw new CartServiceUnavailableException();
        } catch (RestClientException exception) {
            throw new CartServiceUnavailableException();
        }
    }

    @Override
    public void clearCart(Long userId) {
        try {
            restClient.delete()
                    .uri("/cart")
                    .header(USER_ID_HEADER, userId.toString())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new CartServiceUnavailableException();
        } catch (RestClientException exception) {
            throw new CartServiceUnavailableException();
        }
    }
}
