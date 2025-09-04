package com.ocean.shopping.dto.payment;

import com.ocean.shopping.model.entity.enums.PaymentProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment intent creation request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment intent creation request")
public class PaymentIntentRequest {

    @Schema(description = "Order ID", example = "1", required = true)
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Schema(description = "Payment provider", example = "STRIPE", required = true)
    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @Schema(description = "Payment amount", example = "99.99")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Additional metadata")
    private Map<String, String> metadata;
}