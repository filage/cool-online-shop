package com.coolonlineshop.user.service;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.entity.User;
import com.coolonlineshop.user.exception.EmailAlreadyExistsException;
import com.coolonlineshop.user.exception.UserNotFoundException;
import com.coolonlineshop.user.exception.UserProfileAlreadyExistsException;
import com.coolonlineshop.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void createCurrentUserSavesNewUserWithTrustedAuthDataAndReturnsResponse() {
        UserCreateRequest request = new UserCreateRequest(
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userRepository.findByAuthUserId(10L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserResponse response = userService.createCurrentUser(10L, "ivan.user@example.com", request);

        verify(userRepository).findByAuthUserId(10L);
        verify(userRepository).findByEmail("ivan.user@example.com");
        verify(userRepository).save(any(User.class));
        assertEquals(1L, response.id());
        assertEquals(10L, response.authUserId());
        assertEquals("ivan.user@example.com", response.email());
        assertEquals("Ivan", response.firstName());
        assertEquals("User", response.lastName());
        assertEquals("+375291112233", response.phone());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        assertEquals(response.createdAt(), response.updatedAt());
    }

    @Test
    void createCurrentUserThrowsExceptionWhenProfileAlreadyExistsForAuthUser() {
        UserCreateRequest request = new UserCreateRequest(
                "Ivan",
                "User",
                "+375291112233"
        );
        User existingUser = createUser(1L, 10L, "ivan.user@example.com");
        when(userRepository.findByAuthUserId(10L)).thenReturn(Optional.of(existingUser));

        UserProfileAlreadyExistsException exception = assertThrows(
                UserProfileAlreadyExistsException.class,
                () -> userService.createCurrentUser(10L, "ivan.user@example.com", request)
        );

        verify(userRepository).findByAuthUserId(10L);
        verify(userRepository, never()).findByEmail("ivan.user@example.com");
        verify(userRepository, never()).save(any(User.class));
        assertEquals("Profile for auth user 10 already exists", exception.getMessage());
    }

    @Test
    void createCurrentUserThrowsExceptionWhenTrustedEmailAlreadyExists() {
        UserCreateRequest request = new UserCreateRequest(
                "Ivan",
                "User",
                "+375291112233"
        );
        User existingUser = createUser(1L, 11L, "ivan.user@example.com");
        when(userRepository.findByAuthUserId(10L)).thenReturn(Optional.empty());
        when(userRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.of(existingUser));

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createCurrentUser(10L, "ivan.user@example.com", request)
        );

        verify(userRepository).findByAuthUserId(10L);
        verify(userRepository).findByEmail("ivan.user@example.com");
        verify(userRepository, never()).save(any(User.class));
        assertEquals("User with email ivan.user@example.com already exists", exception.getMessage());
    }

    @Test
    void getCurrentUserReturnsUserWhenProfileExists() {
        User user = createUser(1L, 10L, "ivan.user@example.com");
        when(userRepository.findByAuthUserId(10L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getCurrentUser(10L);

        verify(userRepository).findByAuthUserId(10L);
        assertEquals(1L, response.id());
        assertEquals(10L, response.authUserId());
        assertEquals("ivan.user@example.com", response.email());
    }

    @Test
    void getCurrentUserThrowsExceptionWhenProfileDoesNotExist() {
        when(userRepository.findByAuthUserId(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getCurrentUser(999L)
        );

        verify(userRepository).findByAuthUserId(999L);
        assertEquals("Profile for auth user 999 not found", exception.getMessage());
    }

    @Test
    void updateCurrentUserUpdatesExistingUserAndReturnsResponse() {
        LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        User user = new User(
                10L,
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233",
                LocalDateTime.of(2026, 1, 1, 9, 0),
                oldUpdatedAt
        );
        ReflectionTestUtils.setField(user, "id", 1L);
        UserUpdateRequest request = new UserUpdateRequest(
                "Petr",
                "Petrov",
                "+375292223344"
        );
        when(userRepository.findByAuthUserId(10L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateCurrentUser(10L, request);

        verify(userRepository).findByAuthUserId(10L);
        verify(userRepository).save(user);
        assertEquals(1L, response.id());
        assertEquals(10L, response.authUserId());
        assertEquals("ivan.user@example.com", response.email());
        assertEquals("Petr", response.firstName());
        assertEquals("Petrov", response.lastName());
        assertEquals("+375292223344", response.phone());
        assertTrue(response.updatedAt().isAfter(oldUpdatedAt));
    }

    private User createUser(Long id, Long authUserId, String email) {
        User user = new User(
                authUserId,
                email,
                "Ivan",
                "User",
                "+375291112233",
                LocalDateTime.of(2026, 1, 1, 10, 0),
                LocalDateTime.of(2026, 1, 1, 10, 0)
        );
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
