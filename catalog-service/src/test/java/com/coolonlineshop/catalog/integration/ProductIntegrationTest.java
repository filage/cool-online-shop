package com.coolonlineshop.catalog.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProductIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("server.port", () -> 0);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void getProductByIdReturnsProductFromMigratedDatabase() throws Exception {
        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.description").value("Compact wireless mouse"))
                .andExpect(jsonPath("$.price").value(29.99))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.availableQuantity").value(50));
    }

    @Test
    void getProductByIdReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product with id 999 not found"));
    }

    @Test
    void createProductCreatesProductWhenCategoryExists() throws Exception {
        mockMvc.perform(post("/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mechanical Keyboard",
                                  "description": "Compact mechanical keyboard",
                                  "price": 89.99,
                                  "categoryId": 1,
                                  "availableQuantity": 15
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.description").value("Compact mechanical keyboard"))
                .andExpect(jsonPath("$.price").value(89.99))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.availableQuantity").value(15));
    }

    @Test
    void createProductReturnsForbiddenWhenUserRoleIsNotAdmin() throws Exception {
        mockMvc.perform(post("/products")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mechanical Keyboard",
                                  "description": "Compact mechanical keyboard",
                                  "price": 89.99,
                                  "categoryId": 1,
                                  "availableQuantity": 15
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Catalog write forbidden"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.detail").value("Catalog writes require ADMIN role"));
    }

    @Test
    void createProductReturnsNotFoundWhenCategoryDoesNotExist() throws Exception {
        mockMvc.perform(post("/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Mechanical Keyboard",
                                  "description": "Compact mechanical keyboard",
                                  "price": 89.99,
                                  "categoryId": 999,
                                  "availableQuantity": 15
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Category not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Category with id 999 not found"));
    }

    @Test
    void deleteProductSoftDeletesProduct() throws Exception {
        long productId = createProductAndReturnId();

        mockMvc.perform(delete("/products/" + productId)
                        .header("X-User-Role", "ADMIN"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product with id " + productId + " not found"));

        mockMvc.perform(get("/products?page=0&size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + productId + ")]").isEmpty());
    }

    private long createProductAndReturnId() throws Exception {
        String response = mockMvc.perform(post("/products")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Soft Delete Test Product",
                                  "description": "Product created by integration test",
                                  "price": 19.99,
                                  "categoryId": 1,
                                  "availableQuantity": 3
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number productId = JsonPath.read(response, "$.id");

        return productId.longValue();
    }
}
