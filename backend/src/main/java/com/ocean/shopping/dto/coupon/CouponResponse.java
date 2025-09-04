package com.ocean.shopping.dto.coupon;

import com.ocean.shopping.model.entity.enums.CouponStatus;
import com.ocean.shopping.model.entity.enums.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Coupon response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Coupon information response")
public class CouponResponse {

    @Schema(description = "Coupon ID", example = "1")
    private Long id;

    @Schema(description = "Coupon code", example = "SAVE20")
    private String code;

    @Schema(description = "Coupon name", example = "20% Off Spring Sale")
    private String name;

    @Schema(description = "Coupon description", example = "Get 20% off on all spring items")
    private String description;

    @Schema(description = "Coupon type")
    private CouponType type;

    @Schema(description = "Coupon status")
    private CouponStatus status;

    @Schema(description = "Store ID (null for global coupons)", example = "1")
    private Long storeId;

    @Schema(description = "Store name (null for global coupons)", example = "Ocean Store")
    private String storeName;

    @Schema(description = "Discount percentage", example = "20.00")
    private BigDecimal discountPercentage;

    @Schema(description = "Fixed discount amount", example = "10.00")
    private BigDecimal discountAmount;

    @Schema(description = "Minimum order amount required", example = "50.00")
    private BigDecimal minimumOrderAmount;

    @Schema(description = "Maximum discount limit", example = "100.00")
    private BigDecimal maximumDiscount;

    @Schema(description = "Total usage limit")
    private Integer usageLimit;

    @Schema(description = "Usage limit per customer")
    private Integer usageLimitPerCustomer;

    @Schema(description = "Times already used")
    private Integer timesUsed;

    @Schema(description = "Valid from date")
    private ZonedDateTime validFrom;

    @Schema(description = "Valid until date")
    private ZonedDateTime validUntil;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Applies to sale items")
    private Boolean appliesToSaleItems;

    @Schema(description = "For first-time customers only")
    private Boolean firstTimeCustomerOnly;

    @Schema(description = "Whether coupon is currently active")
    private boolean isActive;

    @Schema(description = "Whether coupon has expired")
    private boolean isExpired;

    @Schema(description = "Whether coupon is used up")
    private boolean isUsedUp;

    @Schema(description = "Created date")
    private ZonedDateTime createdAt;

    @Schema(description = "Last updated date")
    private ZonedDateTime updatedAt;
}