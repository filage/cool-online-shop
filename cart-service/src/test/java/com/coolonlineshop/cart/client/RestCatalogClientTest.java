package com.coolonlineshop.cart.client;

import com.coolonlineshop.cart.exception.CatalogServiceUnavailableException;
import com.coolonlineshop.cart.exception.ProductNotFoundException;
import com.coolonlineshop.cart.exception.ProductQuantityNotAvailableException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestCatalogClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void validateProductAvailableSucceedsWhenRequestedQuantityIsAvailable() throws IOException {
        startServer();
        server.createContext("/products/10", exchange -> writeJsonResponse(
                exchange,
                200,
                """
                        {
                          "id": 10,
                          "availableQuantity": 5
                        }
                        """
        ));
        RestCatalogClient client = newClient(Duration.ofMillis(500), Duration.ofMillis(500));

        assertDoesNotThrow(() -> client.validateProductAvailable(10L, 5));
    }

    @Test
    void validateProductAvailableThrowsProductQuantityNotAvailableWhenRequestedQuantityIsTooHigh()
            throws IOException {
        startServer();
        server.createContext("/products/10", exchange -> writeJsonResponse(
                exchange,
                200,
                """
                        {
                          "id": 10,
                          "availableQuantity": 5
                        }
                        """
        ));
        RestCatalogClient client = newClient(Duration.ofMillis(500), Duration.ofMillis(500));

        ProductQuantityNotAvailableException exception = assertThrows(
                ProductQuantityNotAvailableException.class,
                () -> client.validateProductAvailable(10L, 7)
        );

        assertEquals("Product with id 10 has only 5 available items, requested 7", exception.getMessage());
    }

    @Test
    void validateProductAvailableThrowsProductNotFoundWhenCatalogReturnsNotFound() throws IOException {
        startServer();
        server.createContext("/products/999", exchange -> writeJsonResponse(
                exchange,
                404,
                """
                        {
                          "title": "Product not found"
                        }
                        """
        ));
        RestCatalogClient client = newClient(Duration.ofMillis(500), Duration.ofMillis(500));

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> client.validateProductAvailable(999L, 1)
        );

        assertEquals("Product with id 999 not found", exception.getMessage());
    }

    @Test
    void validateProductAvailableThrowsCatalogServiceUnavailableWhenCatalogReturnsServerError()
            throws IOException {
        startServer();
        server.createContext("/products/10", exchange -> writeJsonResponse(
                exchange,
                500,
                """
                        {
                          "title": "Internal Server Error"
                        }
                        """
        ));
        RestCatalogClient client = newClient(Duration.ofMillis(500), Duration.ofMillis(500));

        CatalogServiceUnavailableException exception = assertThrows(
                CatalogServiceUnavailableException.class,
                () -> client.validateProductAvailable(10L, 1)
        );

        assertEquals("Catalog service is unavailable", exception.getMessage());
    }

    @Test
    void validateProductAvailableThrowsCatalogServiceUnavailableWhenCatalogReturnsEmptyBody()
            throws IOException {
        startServer();
        server.createContext("/products/10", exchange -> writeJsonResponse(exchange, 200, ""));
        RestCatalogClient client = newClient(Duration.ofMillis(500), Duration.ofMillis(500));

        CatalogServiceUnavailableException exception = assertThrows(
                CatalogServiceUnavailableException.class,
                () -> client.validateProductAvailable(10L, 1)
        );

        assertEquals("Catalog service is unavailable", exception.getMessage());
    }

    @Test
    void validateProductAvailableThrowsCatalogServiceUnavailableWhenCatalogResponseTimesOut()
            throws IOException {
        startServer();
        server.createContext("/products/10", exchange -> {
            try {
                Thread.sleep(500);
                writeJsonResponse(
                        exchange,
                        200,
                        """
                                {
                                  "id": 10,
                                  "availableQuantity": 5
                                }
                                """
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        RestCatalogClient client = newClient(Duration.ofMillis(500), Duration.ofMillis(50));

        CatalogServiceUnavailableException exception = assertThrows(
                CatalogServiceUnavailableException.class,
                () -> client.validateProductAvailable(10L, 1)
        );

        assertEquals("Catalog service is unavailable", exception.getMessage());
    }

    private void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    private RestCatalogClient newClient(Duration connectTimeout, Duration readTimeout) {
        return new RestCatalogClient(
                RestClient.builder(),
                new CatalogServiceProperties(baseUrl(), connectTimeout, readTimeout)
        );
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void writeJsonResponse(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
