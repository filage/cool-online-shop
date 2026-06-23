package com.coolonlineshop.user.service;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.entity.User;
import com.coolonlineshop.user.exception.EmailAlreadyExistsException;
import com.coolonlineshop.user.exception.UserNotFoundException;
import com.coolonlineshop.user.exception.UserProfileAlreadyExistsException;
import com.coolonlineshop.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createCurrentUser(Long authUserId, String email, UserCreateRequest request) {
        if (userRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new UserProfileAlreadyExistsException(authUserId);
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyExistsException(email);
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                authUserId,
                email,
                request.firstName(),
                request.lastName(),
                request.phone(),
                now,
                now
        );

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public UserResponse getCurrentUser(Long authUserId) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> UserNotFoundException.forAuthUserId(authUserId));

        return toResponse(user);
    }

    public UserResponse updateCurrentUser(Long authUserId, UserUpdateRequest request) {
        User user = userRepository.findByAuthUserId(authUserId)
                .orElseThrow(() -> UserNotFoundException.forAuthUserId(authUserId));

        user.updateProfile(
                request.firstName(),
                request.lastName(),
                request.phone(),
                LocalDateTime.now()
        );

        User updatedUser = userRepository.save(user);

        return toResponse(updatedUser);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getAuthUserId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
