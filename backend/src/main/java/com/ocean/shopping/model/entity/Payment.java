package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import com.ocean.shopping.model.entity.enums.PaymentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Payment entity for transaction tracking
 */
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_order_id", columnList = "order_id"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_provider", columnList = "provider"),
    @Index(name = "idx_payments_transaction_id", columnList = "transaction_id"),
    @Index(name = "idx_payments_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    private PaymentMethod paymentMethod;

    @Column(name = "transaction_id", unique = true, nullable = false)
    @NotBlank(message = "Transaction ID is required")
    @Size(max = 255)
    private String transactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    @NotNull(message = "Payment provider is required")
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be positive or zero")
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "gateway_payment_id")
    @Size(max = 255)
    private String gatewayPaymentId;

    @Column(name = "gateway_customer_id")
    @Size(max = 255)
    private String gatewayCustomerId;

    @Column(name = "gateway_transaction_fee", precision = 10, scale = 4)
    private BigDecimal gatewayTransactionFee;

    @Column(name = "failure_reason")
    @Size(max = 500)
    private String failureReason;

    @Column(name = "gateway_response", columnDefinition = "TEXT")
    private String gatewayResponse;

    @Column(name = "refunded_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    // Timestamps for payment lifecycle
    @Column(name = "authorized_at")
    private ZonedDateTime authorizedAt;

    @Column(name = "captured_at")
    private ZonedDateTime capturedAt;

    @Column(name = "failed_at")
    private ZonedDateTime failedAt;

    @Column(name = "refunded_at")
    private ZonedDateTime refundedAt;

    @Column(name = "cancelled_at")
    private ZonedDateTime cancelledAt;

    // Helper methods
    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public boolean isProcessing() {
        return status == PaymentStatus.PROCESSING;
    }

    public boolean isSucceeded() {
        return status == PaymentStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == PaymentStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == PaymentStatus.CANCELLED;
    }

    public boolean isRefunded() {
        return status == PaymentStatus.REFUNDED;
    }

    public boolean isPartiallyRefunded() {
        return status == PaymentStatus.PARTIALLY_REFUNDED;
    }

    public BigDecimal getRefundableAmount() {
        if (!isSucceeded()) {
            return BigDecimal.ZERO;
        }
        return amount.subtract(refundedAmount);
    }

    public boolean isRefundable() {
        return getRefundableAmount().compareTo(BigDecimal.ZERO) > 0;
    }
}