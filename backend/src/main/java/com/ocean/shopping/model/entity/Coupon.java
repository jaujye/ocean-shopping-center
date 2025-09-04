package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.CouponStatus;
import com.ocean.shopping.model.entity.enums.CouponType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Coupon entity for discount management
 */
@Entity
@Table(name = "coupons", indexes = {
    @Index(name = "idx_coupons_code", columnList = "code", unique = true),
    @Index(name = "idx_coupons_status", columnList = "status"),
    @Index(name = "idx_coupons_store_id", columnList = "store_id"),
    @Index(name = "idx_coupons_valid_from", columnList = "valid_from"),
    @Index(name = "idx_coupons_valid_until", columnList = "valid_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false)
    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Coupon code must contain only uppercase letters, numbers, hyphens and underscores")
    private String code;

    @Column(name = "name", nullable = false)
    @NotBlank(message = "Coupon name is required")
    @Size(max = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    @NotNull(message = "Coupon type is required")
    private CouponType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CouponStatus status = CouponStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store; // null for global coupons

    // Discount configuration
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount percentage must be greater than 0")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100")
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @PositiveOrZero(message = "Discount amount must be positive or zero")
    private BigDecimal discountAmount;

    @Column(name = "minimum_order_amount", precision = 10, scale = 2)
    @PositiveOrZero(message = "Minimum order amount must be positive or zero")
    private BigDecimal minimumOrderAmount;

    @Column(name = "maximum_discount", precision = 10, scale = 2)
    @PositiveOrZero(message = "Maximum discount must be positive or zero")
    private BigDecimal maximumDiscount;

    // Usage limits
    @Column(name = "usage_limit")
    @PositiveOrZero(message = "Usage limit must be positive or zero")
    private Integer usageLimit;

    @Column(name = "usage_limit_per_customer")
    @PositiveOrZero(message = "Usage limit per customer must be positive or zero")
    private Integer usageLimitPerCustomer;

    @Column(name = "times_used")
    @Builder.Default
    private Integer timesUsed = 0;

    // Validity period
    @Column(name = "valid_from", nullable = false)
    @NotNull(message = "Valid from date is required")
    private ZonedDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    @NotNull(message = "Valid until date is required")
    private ZonedDateTime validUntil;

    @Column(name = "currency", nullable = false)
    @Size(min = 3, max = 3)
    @Builder.Default
    private String currency = "USD";

    // Additional configuration
    @Column(name = "applies_to_sale_items")
    @Builder.Default
    private Boolean appliesToSaleItems = true;

    @Column(name = "first_time_customer_only")
    @Builder.Default
    private Boolean firstTimeCustomerOnly = false;

    // Helper methods
    public boolean isActive() {
        ZonedDateTime now = ZonedDateTime.now();
        return status == CouponStatus.ACTIVE 
            && now.isAfter(validFrom) 
            && now.isBefore(validUntil)
            && (usageLimit == null || timesUsed < usageLimit);
    }

    public boolean isExpired() {
        return ZonedDateTime.now().isAfter(validUntil) || status == CouponStatus.EXPIRED;
    }

    public boolean isUsedUp() {
        return usageLimit != null && timesUsed >= usageLimit;
    }

    public boolean canBeUsed() {
        return isActive() && !isExpired() && !isUsedUp();
    }

    public boolean isGlobalCoupon() {
        return store == null;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!canBeUsed() || orderAmount == null) {
            return BigDecimal.ZERO;
        }

        // Check minimum order amount
        if (minimumOrderAmount != null && orderAmount.compareTo(minimumOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        switch (type) {
            case PERCENTAGE:
                if (discountPercentage != null) {
                    discount = orderAmount.multiply(discountPercentage).divide(BigDecimal.valueOf(100));
                }
                break;
            case FIXED_AMOUNT:
                if (discountAmount != null) {
                    discount = discountAmount;
                }
                break;
            case FREE_SHIPPING:
                // Free shipping discount will be handled separately in shipping calculation
                discount = BigDecimal.ZERO;
                break;
            case BUY_ONE_GET_ONE:
                // BOGO logic will be handled in the service layer
                discount = BigDecimal.ZERO;
                break;
        }

        // Apply maximum discount limit
        if (maximumDiscount != null && discount.compareTo(maximumDiscount) > 0) {
            discount = maximumDiscount;
        }

        // Ensure discount doesn't exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    public void incrementUsage() {
        this.timesUsed++;
        if (usageLimit != null && timesUsed >= usageLimit) {
            this.status = CouponStatus.USED_UP;
        }
    }
}