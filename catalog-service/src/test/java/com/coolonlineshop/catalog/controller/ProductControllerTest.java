package com.coolonlineshop.catalog.controller;

import com.coolonlineshop.catalog.dto.ProductPageResponse;
import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.exception.GlobalExceptionHandler;
import com.coolonlineshop.catalog.exception.ProductNotFoundException;
import com.coolonlineshop.catalog.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void getProductsReturnsProducts() throws Exception {
        ProductResponse firstProduct = createProduct(1L, "Wireless Mouse");
        ProductResponse secondProduct = createProduct(2L, "Spring Boot Guide");
        ProductPageResponse response = new ProductPageResponse(
                List.of(firstProduct, secondProduct),
                0,
                20,
                2,
                1
        );
        when(productService.getProducts(PageRequest.of(0, 20))).thenReturn(response);

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[1].name").value("Spring Boot Guide"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getProductByIdReturnsProduct() throws Exception {
        ProductResponse product = createProduct(1L, "Wireless Mouse");

        when(productService.getProductById(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Wireless Mouse"))
                .andExpect(jsonPath("$.description").value("Compact wireless mouse"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.availableQuantity").value(50));
    }

    @Test
    void getProductByIdReturnsNotFoundWhenProductDoesNotExist() throws Exception {
        when(productService.getProductById(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product with id 999 not found"));
    }

    private ProductResponse createProduct(Long id, String name) {
        return new ProductResponse(
                id,
                name,
                "Compact wireless mouse",
                new BigDecimal("29.99"),
                1L,
                50,
                LocalDateTime.parse("2026-06-12T12:00:00"),
                LocalDateTime.parse("2026-06-12T12:00:00")
        );
    }
}
