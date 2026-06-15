package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.ProductPageResponse;
import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.entity.Product;
import com.coolonlineshop.catalog.exception.ProductNotFoundException;
import com.coolonlineshop.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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
        Product product = createProduct(1L, "Wireless Mouse");
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
    void getProductsReturnsProductResponses() {
        Product firstProduct = createProduct(1L, "Wireless Mouse");
        Product secondProduct = createProduct(2L, "Spring Boot Guide");
        PageRequest pageable = PageRequest.of(0, 20);
        when(productRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(firstProduct, secondProduct), pageable, 2));

        ProductPageResponse response = productService.getProducts(pageable);

        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(2, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals(2, response.items().size());
        assertEquals(1L, response.items().get(0).id());
        assertEquals("Wireless Mouse", response.items().get(0).name());
        assertEquals(2L, response.items().get(1).id());
        assertEquals("Spring Boot Guide", response.items().get(1).name());
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

    private Product createProduct(Long id, String name) {
        Product product = new Product();
        LocalDateTime timestamp = LocalDateTime.parse("2026-06-12T12:00:00");

        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "name", name);
        ReflectionTestUtils.setField(product, "description", "Compact wireless mouse");
        ReflectionTestUtils.setField(product, "price", new BigDecimal("29.99"));
        ReflectionTestUtils.setField(product, "categoryId", 1L);
        ReflectionTestUtils.setField(product, "availableQuantity", 50);
        ReflectionTestUtils.setField(product, "createdAt", timestamp);
        ReflectionTestUtils.setField(product, "updatedAt", timestamp);

        return product;
    }
}
