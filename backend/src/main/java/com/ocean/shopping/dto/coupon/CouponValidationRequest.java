package com.ocean.shopping.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Coupon validation request DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Coupon validation request")
public class CouponValidationRequest {

    @Schema(description = "Coupon code to validate", example = "SAVE20", required = true)
    @NotBlank(message = "Coupon code is required")
    @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
    private String code;

    @Schema(description = "Order amount to calculate discount", example = "100.00", required = true)
    @PositiveOrZero(message = "Order amount must be positive or zero")
    private BigDecimal orderAmount;

    @Schema(description = "Store ID for store-specific coupons", example = "1")
    private Long storeId;

    @Schema(description = "Customer email for usage validation", example = "customer@example.com")
    private String customerEmail;

    @Schema(description = "Currency code", example = "USD")
    @Size(min = 3, max = 3)
    private String currency = "USD";
}