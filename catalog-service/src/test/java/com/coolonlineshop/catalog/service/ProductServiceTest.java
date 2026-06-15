package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.entity.Product;
import com.coolonlineshop.catalog.exception.ProductNotFoundException;
import com.coolonlineshop.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void getProductByIdReturnsProductResponse() {
        Product product = createProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById(1L);

        assertEquals(1L, response.id());
        assertEquals("Wireless Mouse", response.name());
        assertEquals("Compact wireless mouse", response.description());
        assertEquals(new BigDecimal("29.99"), response.price());
        assertEquals(1L, response.categoryId());
        assertEquals(50, response.availableQuantity());
        assertEquals(LocalDateTime.parse("2026-06-12T12:00:00"), response.createdAt());
        assertEquals(LocalDateTime.parse("2026-06-12T12:00:00"), response.updatedAt());
    }

    @Test
    void getProductByIdThrowsExceptionWhenProductDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(999L)
        );

        assertEquals("Product with id 999 not found", exception.getMessage());
    }

    private Product createProduct() {
        Product product = new Product();
        LocalDateTime timestamp = LocalDateTime.parse("2026-06-12T12:00:00");

        ReflectionTestUtils.setField(product, "id", 1L);
        ReflectionTestUtils.setField(product, "name", "Wireless Mouse");
        ReflectionTestUtils.setField(product, "description", "Compact wireless mouse");
        ReflectionTestUtils.setField(product, "price", new BigDecimal("29.99"));
        ReflectionTestUtils.setField(product, "categoryId", 1L);
        ReflectionTestUtils.setField(product, "availableQuantity", 50);
        ReflectionTestUtils.setField(product, "createdAt", timestamp);
        ReflectionTestUtils.setField(product, "updatedAt", timestamp);

        return product;
    }
}
