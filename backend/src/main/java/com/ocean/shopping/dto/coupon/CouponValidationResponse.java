package com.ocean.shopping.dto.coupon;

import com.ocean.shopping.model.entity.enums.CouponType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Coupon validation response DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Coupon validation response")
public class CouponValidationResponse {

    @Schema(description = "Whether coupon is valid", example = "true")
    private boolean valid;

    @Schema(description = "Coupon code", example = "SAVE20")
    private String code;

    @Schema(description = "Coupon name", example = "20% Off Spring Sale")
    private String name;

    @Schema(description = "Coupon type")
    private CouponType type;

    @Schema(description = "Discount amount calculated", example = "20.00")
    private BigDecimal discountAmount;

    @Schema(description = "Original order amount", example = "100.00")
    private BigDecimal originalAmount;

    @Schema(description = "Final amount after discount", example = "80.00")
    private BigDecimal finalAmount;

    @Schema(description = "Discount percentage applied", example = "20.00")
    private BigDecimal discountPercentage;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Whether coupon provides free shipping")
    private boolean freeShipping;

    @Schema(description = "Error message if invalid")
    private String errorMessage;

    @Schema(description = "Additional information about the coupon")
    private String message;

    // Helper factory methods
    public static CouponValidationResponse valid(String code, String name, CouponType type, 
                                               BigDecimal discountAmount, BigDecimal originalAmount, 
                                               String currency, boolean freeShipping) {
        BigDecimal finalAmount = originalAmount.subtract(discountAmount);
        BigDecimal discountPercentage = originalAmount.compareTo(BigDecimal.ZERO) > 0 
            ? discountAmount.multiply(BigDecimal.valueOf(100)).divide(originalAmount, 2, java.math.RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return CouponValidationResponse.builder()
            .valid(true)
            .code(code)
            .name(name)
            .type(type)
            .discountAmount(discountAmount)
            .originalAmount(originalAmount)
            .finalAmount(finalAmount)
            .discountPercentage(discountPercentage)
            .currency(currency)
            .freeShipping(freeShipping)
            .message("Coupon applied successfully")
            .build();
    }

    public static CouponValidationResponse invalid(String code, String errorMessage) {
        return CouponValidationResponse.builder()
            .valid(false)
            .code(code)
            .errorMessage(errorMessage)
            .discountAmount(BigDecimal.ZERO)
            .build();
    }
}