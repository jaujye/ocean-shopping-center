package com.ocean.shopping.dto.payment;

import com.ocean.shopping.service.payment.PaymentProviderService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Payment intent response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment intent response")
public class PaymentIntentResponse {

    @Schema(description = "Payment intent ID", example = "pi_abc123")
    private String id;

    @Schema(description = "Client secret for frontend", example = "pi_abc123_secret_xyz")
    private String clientSecret;

    @Schema(description = "Payment amount", example = "99.99")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Payment intent status", example = "requires_payment_method")
    private String status;

    @Schema(description = "Additional metadata")
    private Map<String, String> metadata;

    /**
     * Create PaymentIntentResponse from PaymentIntent record
     */
    public static PaymentIntentResponse fromPaymentIntent(PaymentProviderService.PaymentIntent paymentIntent) {
        return PaymentIntentResponse.builder()
                .id(paymentIntent.id())
                .clientSecret(paymentIntent.clientSecret())
                .amount(paymentIntent.amount())
                .currency(paymentIntent.currency())
                .status(paymentIntent.status())
                .metadata(paymentIntent.metadata())
                .build();
    }
}