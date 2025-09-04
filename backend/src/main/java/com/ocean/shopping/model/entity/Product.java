package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Product entity with search optimization
 */
@Entity
@Table(name = "products", 
    indexes = {
        @Index(name = "idx_products_store_id", columnList = "store_id"),
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_sku", columnList = "sku"),
        @Index(name = "idx_products_active", columnList = "is_active"),
        @Index(name = "idx_products_featured", columnList = "is_featured"),
        @Index(name = "idx_products_price", columnList = "price")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_products_store_slug", columnNames = {"store_id", "slug"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @NotNull(message = "Store is required")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Product name is required")
    @Size(max = 255)
    private String name;

    @Column(name = "slug", nullable = false)
    @NotBlank(message = "Product slug is required")
    @Size(max = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "short_description", columnDefinition = "TEXT")
    private String shortDescription;

    @Column(name = "sku")
    @Size(max = 100)
    private String sku;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be positive or zero")
    private BigDecimal price;

    @Column(name = "compare_at_price", precision = 10, scale = 2)
    @PositiveOrZero(message = "Compare at price must be positive or zero")
    private BigDecimal compareAtPrice;

    @Column(name = "cost", precision = 10, scale = 2)
    @PositiveOrZero(message = "Cost must be positive or zero")
    private BigDecimal cost;

    @Column(name = "track_inventory", nullable = false)
    @Builder.Default
    private Boolean trackInventory = true;

    @Column(name = "inventory_quantity", nullable = false)
    @PositiveOrZero(message = "Inventory quantity must be positive or zero")
    @Builder.Default
    private Integer inventoryQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    @PositiveOrZero(message = "Low stock threshold must be positive or zero")
    @Builder.Default
    private Integer lowStockThreshold = 10;

    @Column(name = "weight", precision = 8, scale = 2)
    @PositiveOrZero(message = "Weight must be positive or zero")
    private BigDecimal weight;

    @Column(name = "dimensions_length", precision = 8, scale = 2)
    @PositiveOrZero(message = "Length must be positive or zero")
    private BigDecimal dimensionsLength;

    @Column(name = "dimensions_width", precision = 8, scale = 2)
    @PositiveOrZero(message = "Width must be positive or zero")
    private BigDecimal dimensionsWidth;

    @Column(name = "dimensions_height", precision = 8, scale = 2)
    @PositiveOrZero(message = "Height must be positive or zero")
    private BigDecimal dimensionsHeight;

    @Column(name = "requires_shipping", nullable = false)
    @Builder.Default
    private Boolean requiresShipping = true;

    @Column(name = "is_digital", nullable = false)
    @Builder.Default
    private Boolean isDigital = false;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "seo_title")
    @Size(max = 255)
    private String seoTitle;

    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;

    // Relationships
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ProductVariant> variants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews;

    // Helper methods
    public boolean isInStock() {
        return !trackInventory || inventoryQuantity > 0;
    }

    public boolean isLowStock() {
        return trackInventory && inventoryQuantity <= lowStockThreshold;
    }

    public boolean hasDiscount() {
        return compareAtPrice != null && compareAtPrice.compareTo(price) > 0;
    }

    public BigDecimal getDiscountAmount() {
        if (hasDiscount()) {
            return compareAtPrice.subtract(price);
        }
        return BigDecimal.ZERO;
    }

    public BigDecimal getDiscountPercentage() {
        if (hasDiscount()) {
            return getDiscountAmount()
                    .divide(compareAtPrice, 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return BigDecimal.ZERO;
    }

    public String getDisplayName() {
        return name != null ? name : "Unnamed Product";
    }
}