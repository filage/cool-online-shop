package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.CategoryCreateRequest;
import com.coolonlineshop.catalog.dto.CategoryResponse;
import com.coolonlineshop.catalog.entity.Category;
import com.coolonlineshop.catalog.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryResponse createCategory(CategoryCreateRequest request) {
        Category category = new Category(
                request.name(),
                request.description()
        );

        Category savedCategory = categoryRepository.save(category);

        return toResponse(savedCategory);
    }

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
