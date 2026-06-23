package com.coolonlineshop.user.controller;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/me")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createCurrentUser(
            @RequestHeader("X-User-Id") Long authUserId,
            @RequestHeader("X-User-Email") String email,
            @Valid @RequestBody UserCreateRequest request
    ) {
        return userService.createCurrentUser(authUserId, email, request);
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(@RequestHeader("X-User-Id") Long authUserId) {
        return userService.getCurrentUser(authUserId);
    }

    @PutMapping("/me")
    public UserResponse updateCurrentUser(
            @RequestHeader("X-User-Id") Long authUserId,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return userService.updateCurrentUser(authUserId, request);
    }
}
