package com.ocean.shopping.dto.payment;

import com.ocean.shopping.model.entity.PaymentMethod;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * PaymentMethod response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment method information response")
public class PaymentMethodResponse {

    @Schema(description = "Payment method unique identifier", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "User ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private String userId;

    @Schema(description = "Payment provider", example = "STRIPE")
    private PaymentProvider provider;

    @Schema(description = "Payment type", example = "CARD")
    private PaymentType paymentType;

    @Schema(description = "Display name", example = "Visa ending in 4242")
    private String displayName;

    @Schema(description = "Card last 4 digits", example = "4242")
    private String cardLast4;

    @Schema(description = "Card brand", example = "visa")
    private String cardBrand;

    @Schema(description = "Card expiration month", example = "12")
    private Integer cardExpMonth;

    @Schema(description = "Card expiration year", example = "2025")
    private Integer cardExpYear;

    @Schema(description = "Formatted card expiry", example = "12/2025")
    private String formattedExpiry;

    @Schema(description = "Card holder name", example = "John Doe")
    private String cardHolderName;

    @Schema(description = "Masked card number", example = "**** **** **** 4242")
    private String maskedCardNumber;

    @Schema(description = "Bank account last 4 digits", example = "1234")
    private String bankLast4;

    @Schema(description = "Bank name", example = "Chase Bank")
    private String bankName;

    @Schema(description = "Account type", example = "checking")
    private String accountType;

    @Schema(description = "Masked account number", example = "****1234")
    private String maskedAccountNumber;

    @Schema(description = "Wallet email", example = "user@example.com")
    private String walletEmail;

    @Schema(description = "Is default payment method", example = "true")
    private Boolean isDefault;

    @Schema(description = "Is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Is card expired", example = "false")
    private Boolean isCardExpired;

    @Schema(description = "Expires at timestamp", example = "2025-12-31T23:59:59Z")
    private ZonedDateTime expiresAt;

    @Schema(description = "Creation timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Update timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime updatedAt;

    /**
     * Create PaymentMethodResponse from PaymentMethod entity
     */
    public static PaymentMethodResponse fromEntity(PaymentMethod paymentMethod) {
        return PaymentMethodResponse.builder()
                .id(paymentMethod.getId())
                .userId(paymentMethod.getUser().getId().toString())
                .provider(paymentMethod.getProvider())
                .paymentType(paymentMethod.getPaymentType())
                .displayName(paymentMethod.getDisplayName())
                .cardLast4(paymentMethod.getCardLast4())
                .cardBrand(paymentMethod.getCardBrand())
                .cardExpMonth(paymentMethod.getCardExpMonth())
                .cardExpYear(paymentMethod.getCardExpYear())
                .formattedExpiry(paymentMethod.getFormattedExpiry())
                .cardHolderName(paymentMethod.getCardHolderName())
                .maskedCardNumber(paymentMethod.getMaskedCardNumber())
                .bankLast4(paymentMethod.getBankLast4())
                .bankName(paymentMethod.getBankName())
                .accountType(paymentMethod.getAccountType())
                .maskedAccountNumber(paymentMethod.getMaskedAccountNumber())
                .walletEmail(paymentMethod.getWalletEmail())
                .isDefault(paymentMethod.getIsDefault())
                .isActive(paymentMethod.getIsActive())
                .isCardExpired(paymentMethod.isCardExpired())
                .expiresAt(paymentMethod.getExpiresAt())
                .createdAt(paymentMethod.getCreatedAt())
                .updatedAt(paymentMethod.getUpdatedAt())
                .build();
    }

    /**
     * Create a masked response that hides sensitive information
     */
    public static PaymentMethodResponse fromEntityMasked(PaymentMethod paymentMethod) {
        PaymentMethodResponse response = fromEntity(paymentMethod);
        // Additional masking if needed
        response.setCardHolderName(maskCardHolderName(paymentMethod.getCardHolderName()));
        return response;
    }

    private static String maskCardHolderName(String cardHolderName) {
        if (cardHolderName == null || cardHolderName.length() <= 2) {
            return cardHolderName;
        }
        // Show first and last character, mask the rest
        return cardHolderName.charAt(0) + "*".repeat(cardHolderName.length() - 2) + 
               cardHolderName.charAt(cardHolderName.length() - 1);
    }
}