package com.coolonlineshop.user.dto;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        Long authUserId,
        String email,
        String firstName,
        String lastName,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
