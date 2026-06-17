package com.coolonlineshop.catalog.controller;

import com.coolonlineshop.catalog.dto.CategoryCreateRequest;
import com.coolonlineshop.catalog.dto.CategoryResponse;
import com.coolonlineshop.catalog.exception.GlobalExceptionHandler;
import com.coolonlineshop.catalog.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@Import(GlobalExceptionHandler.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @Test
    void getCategoriesReturnsCategories() throws Exception {
        CategoryResponse firstCategory = new CategoryResponse(
                1L,
                "Electronics",
                "Electronic devices and accessories"
        );
        CategoryResponse secondCategory = new CategoryResponse(
                2L,
                "Books",
                "Printed and digital books"
        );
        when(categoryService.getCategories()).thenReturn(List.of(firstCategory, secondCategory));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Electronics"))
                .andExpect(jsonPath("$[0].description").value("Electronic devices and accessories"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Books"))
                .andExpect(jsonPath("$[1].description").value("Printed and digital books"));
    }

    @Test
    void createCategoryReturnsCreatedCategory() throws Exception {
        CategoryResponse category = new CategoryResponse(
                3L,
                "Home Office",
                "Products for remote work and home offices"
        );
        when(categoryService.createCategory(any(CategoryCreateRequest.class))).thenReturn(category);

        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Home Office",
                                  "description": "Products for remote work and home offices"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("Home Office"))
                .andExpect(jsonPath("$.description").value("Products for remote work and home offices"));
    }

    @Test
    void createCategoryReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "description": "Products for remote work and home offices"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.name").value("must not be blank"));
    }
}
