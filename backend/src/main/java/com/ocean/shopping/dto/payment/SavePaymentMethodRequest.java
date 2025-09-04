package com.ocean.shopping.dto.payment;

import com.ocean.shopping.model.entity.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Save payment method request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Save payment method request")
public class SavePaymentMethodRequest {

    @Schema(description = "Payment method token from gateway", example = "pm_abc123", required = true)
    @NotBlank(message = "Payment method token is required")
    private String paymentMethodToken;

    @Schema(description = "Payment provider", example = "STRIPE", required = true)
    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @Schema(description = "Whether to set as default payment method", example = "false")
    @Builder.Default
    private Boolean setAsDefault = false;
}