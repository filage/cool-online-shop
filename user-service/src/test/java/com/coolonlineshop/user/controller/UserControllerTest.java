package com.coolonlineshop.user.controller;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.exception.EmailAlreadyExistsException;
import com.coolonlineshop.user.exception.GlobalExceptionHandler;
import com.coolonlineshop.user.exception.UserNotFoundException;
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
    void createUserReturnsCreatedUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(response);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan.user@example.com",
                                  "firstName": "Ivan",
                                  "lastName": "User",
                                  "phone": "+375291112233"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Ivan"))
                .andExpect(jsonPath("$.lastName").value("User"))
                .andExpect(jsonPath("$.phone").value("+375291112233"));
    }

    @Test
    void createUserReturnsConflictWhenEmailAlreadyExists() throws Exception {
        when(userService.createUser(any(UserCreateRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("ivan.user@example.com"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "ivan.user@example.com",
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
    void createUserReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-email",
                                  "firstName": "",
                                  "lastName": "",
                                  "phone": "123456789012345678901234567890123456789012345678901"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.email").value("must be a well-formed email address"))
                .andExpect(jsonPath("$.errors.firstName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.lastName").value("must not be blank"))
                .andExpect(jsonPath("$.errors.phone").value("size must be between 0 and 50"));
    }

    @Test
    void getUserByIdReturnsUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userService.getUserById(1L)).thenReturn(response);

        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"));
    }

    @Test
    void getUserByIdReturnsNotFoundWhenUserDoesNotExist() throws Exception {
        when(userService.getUserById(999L)).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(get("/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("User not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("User with id 999 not found"));
    }

    @Test
    void getUserByEmailReturnsUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userService.getUserByEmail("ivan.user@example.com")).thenReturn(response);

        mockMvc.perform(get("/users/by-email")
                        .param("email", "ivan.user@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"));
    }

    @Test
    void updateUserReturnsUpdatedUser() throws Exception {
        UserResponse response = createResponse(
                1L,
                "ivan.user@example.com",
                "Petr",
                "Petrov",
                "+375292223344"
        );
        when(userService.updateUser(eq(1L), any(UserUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(put("/users/1")
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
                .andExpect(jsonPath("$.email").value("ivan.user@example.com"))
                .andExpect(jsonPath("$.firstName").value("Petr"))
                .andExpect(jsonPath("$.lastName").value("Petrov"))
                .andExpect(jsonPath("$.phone").value("+375292223344"));
    }

    @Test
    void updateUserReturnsBadRequestWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/users/1")
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
            String email,
            String firstName,
            String lastName,
            String phone
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 10, 0);

        return new UserResponse(
                id,
                email,
                firstName,
                lastName,
                phone,
                now,
                now
        );
    }
}
