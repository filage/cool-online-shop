package com.coolonlineshop.catalog.repository;

import com.coolonlineshop.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
