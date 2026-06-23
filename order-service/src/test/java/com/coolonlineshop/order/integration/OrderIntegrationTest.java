package com.coolonlineshop.order.integration;

import com.coolonlineshop.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://%s:%d/order_test_db".formatted(
                postgres.getHost(),
                postgres.getMappedPort(5432)
        ));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrderStoresOrderAndItemsInDatabase() throws Exception {
        Long orderId = createOrder(1L);

        mockMvc.perform(get("/orders/%d".formatted(orderId))
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(99.97))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].productName").value("Wireless Mouse"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].productId").value(25))
                .andExpect(jsonPath("$.items[1].productName").value("Spring Boot Guide"))
                .andExpect(jsonPath("$.items[1].quantity").value(1));

        assertTrue(orderRepository.findById(orderId).isPresent());
        assertEquals(2, orderRepository.findById(orderId).orElseThrow().getItems().size());
    }

    @Test
    void getOrdersByUserIdReturnsOnlyUserOrders() throws Exception {
        Long firstOrderId = createOrder(1L);
        Long secondOrderId = createOrder(1L);
        createOrder(2L);

        mockMvc.perform(get("/orders/by-user")
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
        Long orderId = createOrder(2L);

        mockMvc.perform(get("/orders/%d".formatted(orderId))
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Order not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Order with id %d not found".formatted(orderId)));
    }

    @Test
    void createOrderReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/orders")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.items").exists());
    }

    private Long createOrder(Long userId) throws Exception {
        String response = mockMvc.perform(post("/orders")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {
                                      "productId": 10,
                                      "productName": "Wireless Mouse",
                                      "productPrice": 29.99,
                                      "quantity": 2
                                    },
                                    {
                                      "productId": 25,
                                      "productName": "Spring Boot Guide",
                                      "productPrice": 39.99,
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return Long.valueOf(response.replaceAll("^\\{\"id\":(\\d+).*", "$1"));
    }
}
