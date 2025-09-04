package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.ZonedDateTime;

/**
 * PaymentMethod entity for storing user payment methods
 */
@Entity
@Table(name = "payment_methods", indexes = {
    @Index(name = "idx_payment_methods_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_methods_provider", columnList = "provider"),
    @Index(name = "idx_payment_methods_is_default", columnList = "is_default"),
    @Index(name = "idx_payment_methods_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @Column(name = "gateway_payment_method_id", nullable = false)
    @NotBlank(message = "Gateway payment method ID is required")
    @Size(max = 255)
    private String gatewayPaymentMethodId;

    @Column(name = "gateway_customer_id")
    @Size(max = 255)
    private String gatewayCustomerId;

    @Column(name = "display_name", nullable = false)
    @NotBlank(message = "Display name is required")
    @Size(max = 100)
    private String displayName;

    // Card-specific fields (masked/tokenized)
    @Column(name = "card_last4")
    @Size(min = 4, max = 4)
    private String cardLast4;

    @Column(name = "card_brand")
    @Size(max = 20)
    private String cardBrand;

    @Column(name = "card_exp_month")
    private Integer cardExpMonth;

    @Column(name = "card_exp_year")
    private Integer cardExpYear;

    @Column(name = "card_holder_name")
    @Size(max = 100)
    private String cardHolderName;

    // Bank account fields (for ACH/bank transfers)
    @Column(name = "bank_last4")
    @Size(min = 4, max = 4)
    private String bankLast4;

    @Column(name = "bank_name")
    @Size(max = 100)
    private String bankName;

    @Column(name = "account_type")
    @Size(max = 20)
    private String accountType; // checking, savings

    // Digital wallet fields
    @Column(name = "wallet_email")
    @Size(max = 255)
    private String walletEmail;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    // Helper methods
    public boolean isCard() {
        return paymentType == PaymentType.CARD;
    }

    public boolean isBankTransfer() {
        return paymentType == PaymentType.BANK_TRANSFER;
    }

    public boolean isDigitalWallet() {
        return paymentType == PaymentType.DIGITAL_WALLET;
    }

    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return ZonedDateTime.now().isAfter(expiresAt);
    }

    public boolean isCardExpired() {
        if (!isCard() || cardExpMonth == null || cardExpYear == null) {
            return false;
        }
        
        ZonedDateTime now = ZonedDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();
        
        if (cardExpYear < currentYear) {
            return true;
        }
        
        if (cardExpYear == currentYear && cardExpMonth < currentMonth) {
            return true;
        }
        
        return false;
    }

    public String getMaskedCardNumber() {
        if (!isCard() || cardLast4 == null) {
            return null;
        }
        return "**** **** **** " + cardLast4;
    }

    public String getMaskedAccountNumber() {
        if (!isBankTransfer() || bankLast4 == null) {
            return null;
        }
        return "****" + bankLast4;
    }

    public String getFormattedExpiry() {
        if (!isCard() || cardExpMonth == null || cardExpYear == null) {
            return null;
        }
        return String.format("%02d/%d", cardExpMonth, cardExpYear);
    }
}