package com.coolonlineshop.catalog.controller;

import com.coolonlineshop.catalog.dto.ProductCreateRequest;
import com.coolonlineshop.catalog.dto.ProductUpdateRequest;
import com.coolonlineshop.catalog.dto.ProductPageResponse;
import com.coolonlineshop.catalog.dto.ProductResponse;
import com.coolonlineshop.catalog.exception.InvalidPageRequestException;
import com.coolonlineshop.catalog.security.CatalogWriteAuthorizer;
import com.coolonlineshop.catalog.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/products")
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ProductPageResponse getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePagination(page, size);

        return productService.getProducts(PageRequest.of(page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody ProductCreateRequest request
    ) {
        CatalogWriteAuthorizer.requireAdmin(userRole);

        return productService.createProduct(request);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        CatalogWriteAuthorizer.requireAdmin(userRole);

        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable Long id
    ) {
        CatalogWriteAuthorizer.requireAdmin(userRole);

        productService.deleteProduct(id);
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    private void validatePagination(int page, int size) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (page < 0) {
            errors.put("page", "must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            errors.put("size", "must be between 1 and " + MAX_PAGE_SIZE);
        }

        if (!errors.isEmpty()) {
            throw new InvalidPageRequestException(errors);
        }
    }
}
