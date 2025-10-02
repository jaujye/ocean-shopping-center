package com.ocean.shopping.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Refund payment request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Refund payment request")
public class RefundPaymentRequest {

    @Schema(description = "Payment ID", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    @NotNull(message = "Payment ID is required")
    private UUID paymentId;

    @Schema(description = "Refund amount (leave null for full refund)", example = "49.99")
    @Positive(message = "Refund amount must be positive")
    private BigDecimal amount;

    @Schema(description = "Reason for refund", example = "Customer requested refund")
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}