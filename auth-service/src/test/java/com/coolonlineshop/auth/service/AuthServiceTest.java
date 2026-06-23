package com.coolonlineshop.auth.service;

import com.coolonlineshop.auth.dto.AuthResponse;
import com.coolonlineshop.auth.dto.LoginRequest;
import com.coolonlineshop.auth.dto.RegisterRequest;
import com.coolonlineshop.auth.entity.AuthUser;
import com.coolonlineshop.auth.entity.Role;
import com.coolonlineshop.auth.exception.EmailAlreadyExistsException;
import com.coolonlineshop.auth.exception.InvalidCredentialsException;
import com.coolonlineshop.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registerSavesNewAuthUserWithHashedPasswordAndUserRole() {
        RegisterRequest request = new RegisterRequest(
                "ivan.user@example.com",
                "password123"
        );
        when(authUserRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashed-password");
        when(authUserRepository.save(any(AuthUser.class))).thenAnswer(invocation -> {
            AuthUser authUser = invocation.getArgument(0);
            ReflectionTestUtils.setField(authUser, "id", 1L);
            return authUser;
        });
        when(jwtService.generateToken(any(AuthUser.class))).thenReturn("access-token");

        AuthResponse response = authService.register(request);

        verify(authUserRepository).findByEmail("ivan.user@example.com");
        verify(passwordEncoder).encode("password123");
        verify(authUserRepository).save(any(AuthUser.class));
        verify(jwtService).generateToken(any(AuthUser.class));
        assertEquals(1L, response.userId());
        assertEquals("ivan.user@example.com", response.email());
        assertEquals(Role.USER, response.role());
        assertEquals("access-token", response.accessToken());
    }

    @Test
    void registerThrowsExceptionWhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "ivan.user@example.com",
                "password123"
        );
        AuthUser existingAuthUser = createAuthUser(1L, "ivan.user@example.com", "hashed-password");
        when(authUserRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.of(existingAuthUser));

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(authUserRepository).findByEmail("ivan.user@example.com");
        verify(passwordEncoder, never()).encode(any());
        verify(authUserRepository, never()).save(any(AuthUser.class));
        assertEquals("Auth user with email ivan.user@example.com already exists", exception.getMessage());
    }

    @Test
    void loginReturnsAuthResponseWhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest(
                "ivan.user@example.com",
                "password123"
        );
        AuthUser authUser = createAuthUser(1L, "ivan.user@example.com", "hashed-password");
        when(authUserRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("password123", "hashed-password")).thenReturn(true);
        when(jwtService.generateToken(authUser)).thenReturn("access-token");

        AuthResponse response = authService.login(request);

        verify(authUserRepository).findByEmail("ivan.user@example.com");
        verify(passwordEncoder).matches("password123", "hashed-password");
        verify(jwtService).generateToken(authUser);
        assertEquals(1L, response.userId());
        assertEquals("ivan.user@example.com", response.email());
        assertEquals(Role.USER, response.role());
        assertEquals("access-token", response.accessToken());
    }

    @Test
    void loginThrowsExceptionWhenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest(
                "missing@example.com",
                "password123"
        );
        when(authUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(authUserRepository).findByEmail("missing@example.com");
        verify(passwordEncoder, never()).matches(any(), any());
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void loginThrowsExceptionWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest(
                "ivan.user@example.com",
                "wrongpassword"
        );
        AuthUser authUser = createAuthUser(1L, "ivan.user@example.com", "hashed-password");
        when(authUserRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.of(authUser));
        when(passwordEncoder.matches("wrongpassword", "hashed-password")).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(authUserRepository).findByEmail("ivan.user@example.com");
        verify(passwordEncoder).matches("wrongpassword", "hashed-password");
        assertEquals("Invalid email or password", exception.getMessage());
    }

    private AuthUser createAuthUser(Long id, String email, String passwordHash) {
        AuthUser authUser = new AuthUser(
                email,
                passwordHash,
                Role.USER,
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
        ReflectionTestUtils.setField(authUser, "id", id);
        assertNotNull(authUser.getCreatedAt());
        return authUser;
    }
}
