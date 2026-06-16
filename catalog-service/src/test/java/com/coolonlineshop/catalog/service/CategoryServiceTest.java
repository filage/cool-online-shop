package com.coolonlineshop.catalog.service;

import com.coolonlineshop.catalog.dto.CategoryResponse;
import com.coolonlineshop.catalog.entity.Category;
import com.coolonlineshop.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void getCategoriesReturnsCategoryResponses() {
        Category firstCategory = createCategory(1L, "Electronics", "Electronic devices and accessories");
        Category secondCategory = createCategory(2L, "Books", "Printed and digital books");
        when(categoryRepository.findAll()).thenReturn(List.of(firstCategory, secondCategory));

        List<CategoryResponse> response = categoryService.getCategories();

        assertEquals(2, response.size());
        assertEquals(1L, response.get(0).id());
        assertEquals("Electronics", response.get(0).name());
        assertEquals("Electronic devices and accessories", response.get(0).description());
        assertEquals(2L, response.get(1).id());
        assertEquals("Books", response.get(1).name());
        assertEquals("Printed and digital books", response.get(1).description());
    }

    private Category createCategory(Long id, String name, String description) {
        Category category = new Category();

        ReflectionTestUtils.setField(category, "id", id);
        ReflectionTestUtils.setField(category, "name", name);
        ReflectionTestUtils.setField(category, "description", description);

        return category;
    }
}
