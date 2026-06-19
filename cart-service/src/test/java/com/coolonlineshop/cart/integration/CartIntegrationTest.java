package com.coolonlineshop.cart.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CartIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void configureRedis(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @BeforeEach
    void cleanRedis() {
        Set<String> keys = redisTemplate.keys("cart:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    void addItemCreatesCartItemInRedis() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 10,
                                  "quantity": 2
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(2));
    }

    @Test
    void addItemIncreasesQuantityWhenSameProductAlreadyExists() throws Exception {
        addItem(1L, 10L, 2);

        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "productId": 10,
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void addItemReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": null,
                                  "productId": 10,
                                  "quantity": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.userId").value("must not be null"))
                .andExpect(jsonPath("$.errors.quantity").value("must be greater than 0"));
    }

    @Test
    void getCartReturnsItemsFromRedis() throws Exception {
        addItem(1L, 10L, 2);
        addItem(1L, 25L, 1);

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items[0].userId").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].userId").value(1))
                .andExpect(jsonPath("$.items[1].productId").value(25))
                .andExpect(jsonPath("$.items[1].quantity").value(1));
    }

    @Test
    void updateItemQuantityUpdatesExistingCartItem() throws Exception {
        addItem(1L, 10L, 2);

        mockMvc.perform(put("/cart/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10))
                .andExpect(jsonPath("$.quantity").value(5));

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].productId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(5));
    }

    @Test
    void updateItemQuantityReturnsNotFoundWhenCartItemDoesNotExist() throws Exception {
        mockMvc.perform(put("/cart/1/items/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 5
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Cart item not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cart item with product id 999 for user id 1 not found"));
    }

    @Test
    void updateItemQuantityReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/cart/1/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.quantity").value("must be greater than 0"));
    }

    @Test
    void deleteItemRemovesCartItem() throws Exception {
        addItem(1L, 10L, 2);

        mockMvc.perform(delete("/cart/1/items/10"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void deleteItemReturnsNotFoundWhenCartItemDoesNotExist() throws Exception {
        mockMvc.perform(delete("/cart/1/items/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Cart item not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Cart item with product id 999 for user id 1 not found"));
    }

    @Test
    void clearCartRemovesAllCartItems() throws Exception {
        addItem(1L, 10L, 2);
        addItem(1L, 25L, 1);

        mockMvc.perform(delete("/cart/1"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.items").isEmpty());
    }

    private void addItem(Long userId, Long productId, Integer quantity) throws Exception {
        mockMvc.perform(post("/cart/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": %d,
                                  "productId": %d,
                                  "quantity": %d
                                }
                                """.formatted(userId, productId, quantity)))
                .andExpect(status().isCreated());
    }
}
