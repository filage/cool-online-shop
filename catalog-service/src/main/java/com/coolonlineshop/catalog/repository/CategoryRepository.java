package com.coolonlineshop.catalog.repository;

import com.coolonlineshop.catalog.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
