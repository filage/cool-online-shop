package com.coolonlineshop.order.repository;

import com.coolonlineshop.order.entity.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Order> findById(Long id);

    @EntityGraph(attributePaths = "items")
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
}
