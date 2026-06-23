package com.coolonlineshop.auth.dto;

import com.coolonlineshop.auth.entity.Role;

public record AuthResponse(
        Long userId,
        String email,
        Role role,
        String accessToken
) {
}
