package com.coolonlineshop.catalog.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryCreateRequest(
        @NotBlank String name,
        String description
) {
}
