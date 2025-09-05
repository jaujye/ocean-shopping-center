package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * Product variant entity for size, color, etc.
 */
@Entity
@Table(name = "product_variants", indexes = {
    @Index(name = "idx_product_variants_product_id", columnList = "product_id"),
    @Index(name = "idx_product_variants_sku", columnList = "sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Variant name is required")
    @Size(max = 255)
    private String name;

    @Column(name = "sku")
    @Size(max = 100)
    private String sku;

    @Column(name = "price", precision = 10, scale = 2)
    @PositiveOrZero(message = "Price must be positive or zero")
    private BigDecimal price;

    @Column(name = "inventory_quantity", nullable = false)
    @PositiveOrZero(message = "Inventory quantity must be positive or zero")
    @Builder.Default
    private Integer inventoryQuantity = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}