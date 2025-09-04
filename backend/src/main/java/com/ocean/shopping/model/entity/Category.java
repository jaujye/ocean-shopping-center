package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

/**
 * Product category entity
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_slug", columnList = "slug"),
    @Index(name = "idx_categories_parent_id", columnList = "parent_id"),
    @Index(name = "idx_categories_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Category name is required")
    @Size(max = 255)
    private String name;

    @Column(name = "slug", unique = true, nullable = false)
    @NotBlank(message = "Category slug is required")
    @Size(max = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    @Size(max = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Relationships
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Category> subcategories;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private List<Product> products;

    // Helper methods
    public boolean hasParent() {
        return parent != null;
    }

    public boolean hasSubcategories() {
        return subcategories != null && !subcategories.isEmpty();
    }
}