package com.ocean.shopping.dto.payment;

import com.ocean.shopping.model.entity.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Process payment request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Process payment request")
public class ProcessPaymentRequest {

    @Schema(description = "Order ID", example = "1", required = true)
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Schema(description = "Payment method ID or token", example = "pm_abc123", required = true)
    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    @Schema(description = "Payment provider", example = "STRIPE", required = true)
    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @Schema(description = "Payment intent ID (if confirming existing intent)", example = "pi_abc123")
    private String paymentIntentId;

    @Schema(description = "Whether to save payment method for future use", example = "false")
    @Builder.Default
    private Boolean savePaymentMethod = false;

    @Schema(description = "Whether to set as default payment method", example = "false")
    @Builder.Default
    private Boolean setAsDefault = false;

    @Schema(description = "Additional metadata")
    private Map<String, String> metadata;
}