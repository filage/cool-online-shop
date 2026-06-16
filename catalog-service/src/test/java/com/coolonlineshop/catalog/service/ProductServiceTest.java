package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.ProductCreateRequest;
import com.coolonlineshop.catalog.dto.ProductPageResponse;
import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.dto.ProductUpdateRequest;
import com.coolonlineshop.catalog.entity.Product;
import com.coolonlineshop.catalog.exception.ProductNotFoundException;
import com.coolonlineshop.catalog.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    void createProductSavesProductAndReturnsProductResponse() {
        ProductCreateRequest request = new ProductCreateRequest(
                "Mechanical Keyboard",
                "Compact mechanical keyboard",
                new BigDecimal("89.99"),
                1L,
                15
        );
        Product savedProduct = createProduct(3L, "Mechanical Keyboard");
        ReflectionTestUtils.setField(savedProduct, "description", "Compact mechanical keyboard");
        ReflectionTestUtils.setField(savedProduct, "price", new BigDecimal("89.99"));
        ReflectionTestUtils.setField(savedProduct, "availableQuantity", 15);
        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product productToSave = productCaptor.getValue();
        assertEquals("Mechanical Keyboard", productToSave.getName());
        assertEquals("Compact mechanical keyboard", productToSave.getDescription());
        assertEquals(new BigDecimal("89.99"), productToSave.getPrice());
        assertEquals(1L, productToSave.getCategoryId());
        assertEquals(15, productToSave.getAvailableQuantity());
        assertNotNull(productToSave.getCreatedAt());
        assertEquals(productToSave.getCreatedAt(), productToSave.getUpdatedAt());

        assertEquals(3L, response.id());
        assertEquals("Mechanical Keyboard", response.name());
        assertEquals("Compact mechanical keyboard", response.description());
        assertEquals(new BigDecimal("89.99"), response.price());
        assertEquals(1L, response.categoryId());
        assertEquals(15, response.availableQuantity());
    }

    @Test
    void updateProductUpdatesProductAndReturnsProductResponse() {
        Product product = createProduct(1L, "Wireless Mouse");
        LocalDateTime createdAt = product.getCreatedAt();
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated Mouse",
                "Updated wireless mouse",
                new BigDecimal("34.99"),
                1L,
                40
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.updateProduct(1L, request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product productToSave = productCaptor.getValue();
        assertEquals(1L, productToSave.getId());
        assertEquals("Updated Mouse", productToSave.getName());
        assertEquals("Updated wireless mouse", productToSave.getDescription());
        assertEquals(new BigDecimal("34.99"), productToSave.getPrice());
        assertEquals(1L, productToSave.getCategoryId());
        assertEquals(40, productToSave.getAvailableQuantity());
        assertEquals(createdAt, productToSave.getCreatedAt());
        assertNotNull(productToSave.getUpdatedAt());

        assertEquals(1L, response.id());
        assertEquals("Updated Mouse", response.name());
        assertEquals("Updated wireless mouse", response.description());
        assertEquals(new BigDecimal("34.99"), response.price());
        assertEquals(1L, response.categoryId());
        assertEquals(40, response.availableQuantity());
        assertEquals(createdAt, response.createdAt());
    }

    @Test
    void updateProductThrowsExceptionWhenProductDoesNotExist() {
        ProductUpdateRequest request = new ProductUpdateRequest(
                "Updated Mouse",
                "Updated wireless mouse",
                new BigDecimal("34.99"),
                1L,
                40
        );
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(999L, request)
        );

        assertEquals("Product with id 999 not found", exception.getMessage());
        verify(productRepository, never()).save(any(Product.class));
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
