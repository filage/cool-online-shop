package com.coolonlineshop.catalog.controller;

import com.coolonlineshop.catalog.dto.CategoryCreateRequest;
import com.coolonlineshop.catalog.dto.CategoryResponse;
import com.coolonlineshop.catalog.security.CatalogWriteAuthorizer;
import com.coolonlineshop.catalog.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryService.getCategories();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @Valid @RequestBody CategoryCreateRequest request
    ) {
        CatalogWriteAuthorizer.requireAdmin(userRole);

        return categoryService.createCategory(request);
    }
}
