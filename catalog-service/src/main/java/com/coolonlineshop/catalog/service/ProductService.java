package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.entity.Product;
import com.coolonlineshop.catalog.exception.ProductNotFoundException;
import com.coolonlineshop.catalog.repository.ProductRepository;
import org.springframework.stereotype.Service;

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
