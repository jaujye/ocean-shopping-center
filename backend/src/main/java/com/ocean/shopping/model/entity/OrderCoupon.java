package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

/**
 * OrderCoupon entity to track applied coupons in orders
 */
@Entity
@Table(name = "order_coupons", indexes = {
    @Index(name = "idx_order_coupons_order_id", columnList = "order_id"),
    @Index(name = "idx_order_coupons_coupon_id", columnList = "coupon_id"),
    @Index(name = "idx_order_coupons_coupon_code", columnList = "coupon_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderCoupon extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    @NotNull(message = "Coupon is required")
    private Coupon coupon;

    @Column(name = "coupon_code", nullable = false)
    @NotNull(message = "Coupon code is required")
    @Size(max = 50)
    private String couponCode; // Store the code for historical reference

    @Column(name = "coupon_name", nullable = false)
    @NotNull(message = "Coupon name is required")
    @Size(max = 255)
    private String couponName; // Store the name for historical reference

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Discount amount is required")
    @PositiveOrZero(message = "Discount amount must be positive or zero")
    private BigDecimal discountAmount;

    @Column(name = "original_order_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Original order amount is required")
    @PositiveOrZero(message = "Original order amount must be positive or zero")
    private BigDecimal originalOrderAmount;

    @Column(name = "currency", nullable = false)
    @Size(min = 3, max = 3)
    @Builder.Default
    private String currency = "USD";

    // Helper methods
    public BigDecimal getDiscountPercentage() {
        if (originalOrderAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return discountAmount
            .multiply(BigDecimal.valueOf(100))
            .divide(originalOrderAmount, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean hasDiscount() {
        return discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }
}