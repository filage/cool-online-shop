package com.coolonlineshop.order.integration;

import com.coolonlineshop.order.client.CartClient;
import com.coolonlineshop.order.client.CartItemResponse;
import com.coolonlineshop.order.client.CartResponse;
import com.coolonlineshop.order.client.CatalogClient;
import com.coolonlineshop.order.client.CatalogProductResponse;
import com.coolonlineshop.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class OrderIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> postgres = new GenericContainer<>("postgres:16-alpine")
            .withEnv("POSTGRES_DB", "order_test_db")
            .withEnv("POSTGRES_USER", "postgres")
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withExposedPorts(5432);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private CartClient cartClient;

    @MockitoBean
    private CatalogClient catalogClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/order_test_db".formatted(
                postgres.getHost(),
                postgres.getMappedPort(5432)
        ));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
        registry.add("cart-service.base-url", () -> "http://cart-service.test");
        registry.add("cart-service.connect-timeout", () -> "5s");
        registry.add("cart-service.read-timeout", () -> "10s");
        registry.add("catalog-service.base-url", () -> "http://catalog-service.test");
        registry.add("catalog-service.connect-timeout", () -> "5s");
        registry.add("catalog-service.read-timeout", () -> "10s");
    }

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void checkoutStoresOrderAndItemsInDatabaseThenClearsCart() throws Exception {
        Long orderId = checkout(1L);

        mockMvc.perform(get("/orders/%d".formatted(orderId))
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(99.97))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.items[0].productPrice").value(29.99))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].productId").value(25))
                .andExpect(jsonPath("$.items[1].productName").value("Spring Boot Guide"))
                .andExpect(jsonPath("$.items[1].productPrice").value(39.99))
                .andExpect(jsonPath("$.items[1].quantity").value(1));

        verify(cartClient).clearCart(1L);
        assertTrue(orderRepository.findById(orderId).isPresent());
        assertEquals(2, orderRepository.findById(orderId).orElseThrow().getItems().size());
    }

    @Test
    void getOrdersByUserIdReturnsOnlyUserOrders() throws Exception {
        Long firstOrderId = checkout(1L);
        Long secondOrderId = checkout(1L);
        checkout(2L);

        mockMvc.perform(get("/orders")
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[1].userId").value(1));

        assertTrue(orderRepository.findById(firstOrderId).isPresent());
        assertTrue(orderRepository.findById(secondOrderId).isPresent());
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(get("/orders/999")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order with id 999 not found"));
    }

    @Test
    void getOrderByIdReturnsNotFoundWhenOrderBelongsToAnotherUser() throws Exception {
        Long orderId = checkout(2L);

        mockMvc.perform(get("/orders/%d".formatted(orderId))
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order with id %d not found".formatted(orderId)));
    }

    @Test
    void checkoutReturnsConflictWhenCartIsEmpty() throws Exception {
        when(cartClient.getCart(1L)).thenReturn(new CartResponse(1L, List.of()));

        mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Cart is empty"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Cart is empty"));
    }

    private Long checkout(Long userId) throws Exception {
        when(cartClient.getCart(userId)).thenReturn(new CartResponse(
                userId,
                List.of(
                        new CartItemResponse(userId, 10L, 2),
                        new CartItemResponse(userId, 25L, 1)
                )
        ));
        when(catalogClient.getProduct(10L)).thenReturn(new CatalogProductResponse(
                10L,
                "Wireless Mouse",
                new BigDecimal("29.99"),
                10
        ));
        when(catalogClient.getProduct(25L)).thenReturn(new CatalogProductResponse(
                25L,
                "Spring Boot Guide",
                new BigDecimal("39.99"),
                5
        ));

        String response = mockMvc.perform(post("/orders/checkout")
                        .header("X-User-Id", userId.toString()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.valueOf(response.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
    }
}
