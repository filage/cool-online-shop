package com.coolonlineshop.user.repository;

import com.coolonlineshop.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByAuthUserId(Long authUserId);
}
