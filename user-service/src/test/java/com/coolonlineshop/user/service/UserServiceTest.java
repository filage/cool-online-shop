package com.coolonlineshop.user.service;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.entity.User;
import com.coolonlineshop.user.exception.EmailAlreadyExistsException;
import com.coolonlineshop.user.exception.UserNotFoundException;
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
    void createUserSavesNewUserAndReturnsResponse() {
        UserCreateRequest request = new UserCreateRequest(
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        when(userRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserResponse response = userService.createUser(request);

        verify(userRepository).findByEmail("ivan.user@example.com");
        verify(userRepository).save(any(User.class));
        assertEquals(1L, response.id());
        assertEquals("ivan.user@example.com", response.email());
        assertEquals("Ivan", response.firstName());
        assertEquals("User", response.lastName());
        assertEquals("+375291112233", response.phone());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
        assertEquals(response.createdAt(), response.updatedAt());
    }

    @Test
    void createUserThrowsExceptionWhenEmailAlreadyExists() {
        UserCreateRequest request = new UserCreateRequest(
                "ivan.user@example.com",
                "Ivan",
                "User",
                "+375291112233"
        );
        User existingUser = createUser(1L, "ivan.user@example.com");
        when(userRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.of(existingUser));

        EmailAlreadyExistsException exception = assertThrows(
                EmailAlreadyExistsException.class,
                () -> userService.createUser(request)
        );

        verify(userRepository).findByEmail("ivan.user@example.com");
        verify(userRepository, never()).save(any(User.class));
        assertEquals("User with email ivan.user@example.com already exists", exception.getMessage());
    }

    @Test
    void getUserByIdReturnsUserWhenUserExists() {
        User user = createUser(1L, "ivan.user@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserById(1L);

        verify(userRepository).findById(1L);
        assertEquals(1L, response.id());
        assertEquals("ivan.user@example.com", response.email());
    }

    @Test
    void getUserByIdThrowsExceptionWhenUserDoesNotExist() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserNotFoundException exception = assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(999L)
        );

        verify(userRepository).findById(999L);
        assertEquals("User with id 999 not found", exception.getMessage());
    }

    @Test
    void getUserByEmailReturnsUserWhenUserExists() {
        User user = createUser(1L, "ivan.user@example.com");
        when(userRepository.findByEmail("ivan.user@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.getUserByEmail("ivan.user@example.com");

        verify(userRepository).findByEmail("ivan.user@example.com");
        assertEquals(1L, response.id());
        assertEquals("ivan.user@example.com", response.email());
    }

    @Test
    void updateUserUpdatesExistingUserAndReturnsResponse() {
        LocalDateTime oldUpdatedAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        User user = new User(
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = userService.updateUser(1L, request);

        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
        assertEquals(1L, response.id());
        assertEquals("ivan.user@example.com", response.email());
        assertEquals("Petr", response.firstName());
        assertEquals("Petrov", response.lastName());
        assertEquals("+375292223344", response.phone());
        assertTrue(response.updatedAt().isAfter(oldUpdatedAt));
    }

    private User createUser(Long id, String email) {
        User user = new User(
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
