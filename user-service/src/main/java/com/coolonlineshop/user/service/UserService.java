package com.coolonlineshop.user.service;

import com.coolonlineshop.user.dto.UserCreateRequest;
import com.coolonlineshop.user.dto.UserResponse;
import com.coolonlineshop.user.dto.UserUpdateRequest;
import com.coolonlineshop.user.entity.User;
import com.coolonlineshop.user.exception.EmailAlreadyExistsException;
import com.coolonlineshop.user.exception.UserNotFoundException;
import com.coolonlineshop.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException(request.email());
        }

        LocalDateTime now = LocalDateTime.now();
        User user = new User(
                request.email(),
                request.firstName(),
                request.lastName(),
                request.phone(),
                now,
                now
        );

        User savedUser = userRepository.save(user);

        return toResponse(savedUser);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

        return toResponse(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));

        return toResponse(user);
    }

    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));

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
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}