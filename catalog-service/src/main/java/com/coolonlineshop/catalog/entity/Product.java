package com.coolonlineshop.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "available_quantity", nullable = false)
    private Integer availableQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    public Product() {
    }

    public Product(
            String name,
            String description,
            BigDecimal price,
            Long categoryId,
            Integer availableQuantity,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.availableQuantity = availableQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void update(
            String name,
            String description,
            BigDecimal price,
            Long categoryId,
            Integer availableQuantity,
            LocalDateTime updatedAt
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.categoryId = categoryId;
        this.availableQuantity = availableQuantity;
        this.updatedAt = updatedAt;
    }

    public void markDeleted(LocalDateTime updatedAt) {
        this.deleted = true;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public Integer getAvailableQuantity() {
        return availableQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
