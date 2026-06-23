package com.coolonlineshop.auth.dto;

import com.coolonlineshop.auth.entity.Role;

public record CurrentUserResponse(
        Long userId,
        String email,
        Role role
) {
}
