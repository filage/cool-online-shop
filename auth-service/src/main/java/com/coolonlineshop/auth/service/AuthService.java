package com.coolonlineshop.auth.service;

import com.coolonlineshop.auth.dto.AuthResponse;
import com.coolonlineshop.auth.dto.LoginRequest;
import com.coolonlineshop.auth.dto.RegisterRequest;
import com.coolonlineshop.auth.entity.AuthUser;
import com.coolonlineshop.auth.entity.Role;
import com.coolonlineshop.auth.exception.EmailAlreadyExistsException;
import com.coolonlineshop.auth.exception.InvalidCredentialsException;
import com.coolonlineshop.auth.repository.AuthUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            AuthUserRepository authUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authUserRepository = authUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        if (authUserRepository.findByEmail(request.email()).isPresent()) {
            throw new EmailAlreadyExistsException(request.email());
        }

        AuthUser authUser = new AuthUser(
                request.email(),
                passwordEncoder.encode(request.password()),
                Role.USER,
                LocalDateTime.now()
        );

        AuthUser savedAuthUser = authUserRepository.save(authUser);

        return toResponse(savedAuthUser);
    }

    public AuthResponse login(LoginRequest request) {
        AuthUser authUser = authUserRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), authUser.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        return toResponse(authUser);
    }

    private AuthResponse toResponse(AuthUser authUser) {
        return new AuthResponse(
                authUser.getId(),
                authUser.getEmail(),
                authUser.getRole()
        );
    }
}
