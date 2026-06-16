package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.ProductCreateRequest;
import com.coolonlineshop.catalog.dto.ProductPageResponse;
import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.dto.ProductUpdateRequest;
import com.coolonlineshop.catalog.entity.Product;
import com.coolonlineshop.catalog.exception.ProductNotFoundException;
import com.coolonlineshop.catalog.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return toResponse(product);
    }

    public ProductPageResponse getProducts(Pageable pageable) {
        Page<Product> productPage = productRepository.findAll(pageable);

        return new ProductPageResponse(
                productPage.getContent().stream().map(this::toResponse).toList(),
                productPage.getNumber(),
                productPage.getSize(),
                productPage.getTotalElements(),
                productPage.getTotalPages()
        );
    }

    public ProductResponse createProduct(ProductCreateRequest request) {
        LocalDateTime now = LocalDateTime.now();
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.categoryId(),
                request.availableQuantity(),
                now,
                now
        );

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        product.update(
                request.name(),
                request.description(),
                request.price(),
                request.categoryId(),
                request.availableQuantity(),
                LocalDateTime.now()
        );
        Product updatedProduct = productRepository.save(product);

        return toResponse(updatedProduct);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getCategoryId(),
                product.getAvailableQuantity(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
