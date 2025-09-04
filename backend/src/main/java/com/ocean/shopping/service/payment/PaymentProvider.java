package com.ocean.shopping.service.payment;

import com.ocean.shopping.model.entity.Payment;
import com.ocean.shopping.model.entity.PaymentMethod;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.PaymentProvider;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Interface for payment gateway providers
 */
public interface PaymentProviderService {

    /**
     * Get the provider type this service handles
     */
    PaymentProvider getProvider();

    /**
     * Create a payment intent for the given amount
     */
    PaymentIntent createPaymentIntent(BigDecimal amount, String currency, Map<String, String> metadata);

    /**
     * Confirm a payment with the given payment method
     */
    PaymentResult confirmPayment(String paymentIntentId, String paymentMethodId, Map<String, String> metadata);

    /**
     * Process a direct payment (for saved payment methods)
     */
    PaymentResult processPayment(PaymentMethod paymentMethod, BigDecimal amount, String currency, Map<String, String> metadata);

    /**
     * Capture an authorized payment
     */
    PaymentResult capturePayment(String paymentIntentId, BigDecimal amount);

    /**
     * Cancel/void a payment
     */
    PaymentResult cancelPayment(String paymentIntentId);

    /**
     * Refund a payment
     */
    RefundResult refundPayment(String paymentIntentId, BigDecimal amount, String reason);

    /**
     * Create a customer in the gateway
     */
    CustomerResult createCustomer(User user);

    /**
     * Save a payment method for future use
     */
    PaymentMethodResult savePaymentMethod(User user, String paymentMethodToken);

    /**
     * Remove a saved payment method
     */
    boolean removePaymentMethod(String gatewayPaymentMethodId);

    /**
     * Get payment method details from gateway
     */
    PaymentMethodDetails getPaymentMethodDetails(String gatewayPaymentMethodId);

    /**
     * Verify webhook signature
     */
    boolean verifyWebhookSignature(String payload, String signature, String endpoint);

    /**
     * Parse webhook event
     */
    WebhookEvent parseWebhookEvent(String payload);

    /**
     * Data classes for payment operations
     */
    record PaymentIntent(
            String id,
            String clientSecret,
            BigDecimal amount,
            String currency,
            String status,
            Map<String, String> metadata
    ) {}

    record PaymentResult(
            boolean success,
            String transactionId,
            String status,
            BigDecimal amount,
            String failureReason,
            Map<String, Object> gatewayResponse
    ) {}

    record RefundResult(
            boolean success,
            String refundId,
            BigDecimal amount,
            String status,
            String failureReason,
            Map<String, Object> gatewayResponse
    ) {}

    record CustomerResult(
            boolean success,
            String customerId,
            String failureReason,
            Map<String, Object> gatewayResponse
    ) {}

    record PaymentMethodResult(
            boolean success,
            String paymentMethodId,
            PaymentMethodDetails details,
            String failureReason,
            Map<String, Object> gatewayResponse
    ) {}

    record PaymentMethodDetails(
            String id,
            String type,
            String displayName,
            String cardLast4,
            String cardBrand,
            Integer cardExpMonth,
            Integer cardExpYear,
            String cardHolderName,
            String bankLast4,
            String bankName,
            String accountType,
            String walletEmail,
            Map<String, Object> metadata
    ) {}

    record WebhookEvent(
            String id,
            String type,
            String objectId,
            String status,
            Map<String, Object> data
    ) {}
}