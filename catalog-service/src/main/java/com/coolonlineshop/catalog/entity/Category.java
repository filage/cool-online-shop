package com.coolonlineshop.catalog.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.Column;

@Entity
@Table(name = "categories")
public class Category {
    
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }
}
