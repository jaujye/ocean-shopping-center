package com.ocean.shopping.dto.payment;

import com.ocean.shopping.model.entity.Payment;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import com.ocean.shopping.model.entity.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * Payment response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment information response")
public class PaymentResponse {

    @Schema(description = "Payment unique identifier", example = "1")
    private Long id;

    @Schema(description = "Order ID", example = "1")
    private Long orderId;

    @Schema(description = "Order number", example = "ORD-2023-001")
    private String orderNumber;

    @Schema(description = "Payment method ID", example = "1")
    private Long paymentMethodId;

    @Schema(description = "Transaction ID", example = "txn_abc123")
    private String transactionId;

    @Schema(description = "Gateway payment ID", example = "pi_abc123")
    private String gatewayPaymentId;

    @Schema(description = "Payment provider", example = "STRIPE")
    private PaymentProvider provider;

    @Schema(description = "Payment type", example = "CARD")
    private PaymentType paymentType;

    @Schema(description = "Payment status", example = "SUCCEEDED")
    private PaymentStatus status;

    @Schema(description = "Payment amount", example = "99.99")
    private BigDecimal amount;

    @Schema(description = "Currency code", example = "USD")
    private String currency;

    @Schema(description = "Refunded amount", example = "0.00")
    private BigDecimal refundedAmount;

    @Schema(description = "Refundable amount", example = "99.99")
    private BigDecimal refundableAmount;

    @Schema(description = "Gateway transaction fee", example = "2.99")
    private BigDecimal gatewayTransactionFee;

    @Schema(description = "Failure reason", example = "Card was declined")
    private String failureReason;

    @Schema(description = "Payment authorized timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime authorizedAt;

    @Schema(description = "Payment captured timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime capturedAt;

    @Schema(description = "Payment failed timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime failedAt;

    @Schema(description = "Payment refunded timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime refundedAt;

    @Schema(description = "Payment cancelled timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime cancelledAt;

    @Schema(description = "Payment creation timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Payment update timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime updatedAt;

    /**
     * Create PaymentResponse from Payment entity
     */
    public static PaymentResponse fromEntity(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .orderNumber(payment.getOrder().getOrderNumber())
                .paymentMethodId(payment.getPaymentMethod() != null ? payment.getPaymentMethod().getId() : null)
                .transactionId(payment.getTransactionId())
                .gatewayPaymentId(payment.getGatewayPaymentId())
                .provider(payment.getProvider())
                .paymentType(payment.getPaymentType())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .refundedAmount(payment.getRefundedAmount())
                .refundableAmount(payment.getRefundableAmount())
                .gatewayTransactionFee(payment.getGatewayTransactionFee())
                .failureReason(payment.getFailureReason())
                .authorizedAt(payment.getAuthorizedAt())
                .capturedAt(payment.getCapturedAt())
                .failedAt(payment.getFailedAt())
                .refundedAt(payment.getRefundedAt())
                .cancelledAt(payment.getCancelledAt())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}