package com.coolonlineshop.user.controller;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.exception.EmailAlreadyExistsException;
import com.coolonlineshop.user.exception.GlobalExceptionHandler;
import com.coolonlineshop.user.exception.UserNotFoundException;
import com.coolonlineshop.user.exception.UserProfileAlreadyExistsException;
import com.coolonlineshop.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createCurrentUserReturnsCreatedUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                10L,
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userService.createCurrentUser(
                eq(10L),
                eq("ivan.user@example.com"),
                any(UserCreateRequest.class)
        )).thenReturn(response);

        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "attacker@example.com",
                                  "firstName": "Ivan",
                                  "lastName": "User",
                                  "phone": "+375291112233"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUserId").value(10))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("+375291112233"));
    }

    @Test
    void createCurrentUserReturnsConflictWhenProfileAlreadyExists() throws Exception {
        when(userService.createCurrentUser(eq(10L), eq("ivan.user@example.com"), any(UserCreateRequest.class)))
                .thenThrow(new UserProfileAlreadyExistsException(10L));

        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ivan",
                                  "lastName": "User",
                                  "phone": "+375291112233"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User profile already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Profile for auth user 10 already exists"));
    }

    @Test
    void createCurrentUserReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(userService.createCurrentUser(eq(10L), eq("ivan.user@example.com"), any(UserCreateRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("ivan.user@example.com"));

        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Ivan",
                                  "lastName": "User",
                                  "phone": "+375291112233"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Email already exists"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("User with email ivan.user@example.com already exists"));
    }

    @Test
    void createCurrentUserReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/users/me")
                        .header("X-User-Id", "10")
                        .header("X-User-Email", "ivan.user@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "",
                                  "phone": "123456789012345678901234567890123456789012345678901"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.phone").value("size must be between 0 and 50"));
    }

    @Test
    void getCurrentUserReturnsUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                10L,
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userService.getCurrentUser(10L)).thenReturn(response);

        mockMvc.perform(get("/users/me")
                        .header("X-User-Id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUserId").value(10))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"));
    }

    @Test
    void getCurrentUserReturnsNotFoundWhenProfileDoesNotExist() throws Exception {
        when(userService.getCurrentUser(999L)).thenThrow(UserNotFoundException.forAuthUserId(999L));

        mockMvc.perform(get("/users/me")
                        .header("X-User-Id", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Profile for auth user 999 not found"));
    }

    @Test
    void updateCurrentUserReturnsUpdatedUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                10L,
                "ivan.user@example.com",
                "Petr",
                "Petrov",
                "+375292223344"
        );
        when(userService.updateCurrentUser(eq(10L), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/me")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Petr",
                                  "lastName": "Petrov",
                                  "phone": "+375292223344"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authUserId").value(10))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Petr"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.phone").value("+375292223344"));
    }

    @Test
    void updateCurrentUserReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/users/me")
                        .header("X-User-Id", "10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "",
                                  "lastName": "",
                                  "phone": "+375292223344"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"));
    }

    private UserResponse createResponse(
            Long id,
            Long authUserId,
            String email,
            String firstName,
            String lastName,
            String phone
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        return new UserResponse(
                id,
                authUserId,
                email,
                firstName,
                lastName,
                phone,
                now,
                now
        );
    }
}
