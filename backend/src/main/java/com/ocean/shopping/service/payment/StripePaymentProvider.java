package com.ocean.shopping.service.payment;

import com.ocean.shopping.model.entity.PaymentMethod;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stripe payment provider implementation
 * 
 * NOTE: This is a simplified implementation for demonstration.
 * In production, use the official Stripe Java SDK.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripePaymentProvider implements PaymentProviderService {

    @Value("${payment.stripe.secret-key}")
    private String secretKey;

    @Value("${payment.stripe.webhook-secret}")
    private String webhookSecret;

    @Value("${payment.stripe.enabled:true}")
    private boolean enabled;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public PaymentIntent createPaymentIntent(BigDecimal amount, String currency, Map<String, String> metadata) {
        if (!enabled) {
            throw new IllegalStateException("Stripe provider is not enabled");
        }

        try {
            // In production, use Stripe SDK:
            // com.stripe.model.PaymentIntent intent = com.stripe.model.PaymentIntent.create(params);
            
            log.info("Creating Stripe payment intent for amount: {} {}", amount, currency);
            
            // Simulate Stripe API call
            String paymentIntentId = "pi_" + System.currentTimeMillis();
            String clientSecret = paymentIntentId + "_secret_" + System.nanoTime();
            
            return new PaymentIntent(
                paymentIntentId,
                clientSecret,
                amount,
                currency.toLowerCase(),
                "requires_payment_method",
                metadata != null ? metadata : Map.of()
            );
            
        } catch (Exception e) {
            log.error("Failed to create Stripe payment intent", e);
            throw new RuntimeException("Failed to create payment intent", e);
        }
    }

    @Override
    public PaymentResult confirmPayment(String paymentIntentId, String paymentMethodId, Map<String, String> metadata) {
        try {
            log.info("Confirming Stripe payment: {} with method: {}", paymentIntentId, paymentMethodId);
            
            // In production, use Stripe SDK:
            // PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            // intent.confirm(params);
            
            // Simulate successful payment confirmation
            return new PaymentResult(
                true,
                paymentIntentId,
                "succeeded",
                BigDecimal.valueOf(100.00), // This would come from the actual intent
                null,
                Map.of(
                    "payment_intent_id", paymentIntentId,
                    "payment_method_id", paymentMethodId,
                    "status", "succeeded"
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to confirm Stripe payment: {}", paymentIntentId, e);
            return new PaymentResult(
                false,
                null,
                "failed",
                BigDecimal.ZERO,
                "Payment confirmation failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public PaymentResult processPayment(PaymentMethod paymentMethod, BigDecimal amount, String currency, Map<String, String> metadata) {
        try {
            log.info("Processing Stripe payment for amount: {} {} using saved method: {}", 
                    amount, currency, paymentMethod.getGatewayPaymentMethodId());
            
            // Create payment intent first
            PaymentIntent intent = createPaymentIntent(amount, currency, metadata);
            
            // Confirm with saved payment method
            return confirmPayment(intent.id(), paymentMethod.getGatewayPaymentMethodId(), metadata);
            
        } catch (Exception e) {
            log.error("Failed to process Stripe payment", e);
            return new PaymentResult(
                false,
                null,
                "failed",
                amount,
                "Payment processing failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public PaymentResult capturePayment(String paymentIntentId, BigDecimal amount) {
        try {
            log.info("Capturing Stripe payment: {} for amount: {}", paymentIntentId, amount);
            
            // In production, use Stripe SDK:
            // PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            // intent.capture(params);
            
            return new PaymentResult(
                true,
                paymentIntentId,
                "succeeded",
                amount,
                null,
                Map.of(
                    "payment_intent_id", paymentIntentId,
                    "captured_amount", amount.toString(),
                    "status", "succeeded"
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to capture Stripe payment: {}", paymentIntentId, e);
            return new PaymentResult(
                false,
                paymentIntentId,
                "failed",
                amount,
                "Capture failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public PaymentResult cancelPayment(String paymentIntentId) {
        try {
            log.info("Canceling Stripe payment: {}", paymentIntentId);
            
            // In production, use Stripe SDK:
            // PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            // intent.cancel();
            
            return new PaymentResult(
                true,
                paymentIntentId,
                "canceled",
                BigDecimal.ZERO,
                null,
                Map.of(
                    "payment_intent_id", paymentIntentId,
                    "status", "canceled"
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to cancel Stripe payment: {}", paymentIntentId, e);
            return new PaymentResult(
                false,
                paymentIntentId,
                "failed",
                BigDecimal.ZERO,
                "Cancellation failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public RefundResult refundPayment(String paymentIntentId, BigDecimal amount, String reason) {
        try {
            log.info("Refunding Stripe payment: {} amount: {} reason: {}", paymentIntentId, amount, reason);
            
            // In production, use Stripe SDK:
            // Refund refund = Refund.create(params);
            
            String refundId = "re_" + System.currentTimeMillis();
            
            return new RefundResult(
                true,
                refundId,
                amount,
                "succeeded",
                null,
                Map.of(
                    "refund_id", refundId,
                    "payment_intent_id", paymentIntentId,
                    "amount", amount.toString(),
                    "reason", reason,
                    "status", "succeeded"
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to refund Stripe payment: {}", paymentIntentId, e);
            return new RefundResult(
                false,
                null,
                amount,
                "failed",
                "Refund failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public CustomerResult createCustomer(User user) {
        try {
            log.info("Creating Stripe customer for user: {}", user.getEmail());
            
            // In production, use Stripe SDK:
            // Customer customer = Customer.create(params);
            
            String customerId = "cus_" + System.currentTimeMillis();
            
            return new CustomerResult(
                true,
                customerId,
                null,
                Map.of(
                    "customer_id", customerId,
                    "email", user.getEmail(),
                    "name", user.getFirstName() + " " + user.getLastName()
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to create Stripe customer for user: {}", user.getEmail(), e);
            return new CustomerResult(
                false,
                null,
                "Customer creation failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public PaymentMethodResult savePaymentMethod(User user, String paymentMethodToken) {
        try {
            log.info("Saving Stripe payment method for user: {}", user.getEmail());
            
            // In production, use Stripe SDK:
            // PaymentMethod paymentMethod = PaymentMethod.retrieve(paymentMethodToken);
            // paymentMethod.attach(params);
            
            PaymentMethodDetails details = new PaymentMethodDetails(
                paymentMethodToken,
                "card", // This would come from actual payment method
                "Visa ending in 4242",
                "4242",
                "visa",
                12,
                2025,
                "John Doe",
                null,
                null,
                null,
                null,
                Map.of()
            );
            
            return new PaymentMethodResult(
                true,
                paymentMethodToken,
                details,
                null,
                Map.of(
                    "payment_method_id", paymentMethodToken,
                    "type", "card"
                )
            );
            
        } catch (Exception e) {
            log.error("Failed to save Stripe payment method for user: {}", user.getEmail(), e);
            return new PaymentMethodResult(
                false,
                null,
                null,
                "Payment method save failed: " + e.getMessage(),
                Map.of("error", e.getMessage())
            );
        }
    }

    @Override
    public boolean removePaymentMethod(String gatewayPaymentMethodId) {
        try {
            log.info("Removing Stripe payment method: {}", gatewayPaymentMethodId);
            
            // In production, use Stripe SDK:
            // PaymentMethod paymentMethod = PaymentMethod.retrieve(gatewayPaymentMethodId);
            // paymentMethod.detach();
            
            return true;
            
        } catch (Exception e) {
            log.error("Failed to remove Stripe payment method: {}", gatewayPaymentMethodId, e);
            return false;
        }
    }

    @Override
    public PaymentMethodDetails getPaymentMethodDetails(String gatewayPaymentMethodId) {
        try {
            log.info("Retrieving Stripe payment method details: {}", gatewayPaymentMethodId);
            
            // In production, use Stripe SDK:
            // PaymentMethod paymentMethod = PaymentMethod.retrieve(gatewayPaymentMethodId);
            
            return new PaymentMethodDetails(
                gatewayPaymentMethodId,
                "card",
                "Visa ending in 4242",
                "4242",
                "visa",
                12,
                2025,
                "John Doe",
                null,
                null,
                null,
                null,
                Map.of()
            );
            
        } catch (Exception e) {
            log.error("Failed to retrieve Stripe payment method details: {}", gatewayPaymentMethodId, e);
            return null;
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String endpoint) {
        try {
            if (webhookSecret == null || webhookSecret.isEmpty()) {
                log.warn("Stripe webhook secret not configured");
                return false;
            }
            
            // Extract timestamp and signature from header
            String[] parts = signature.split(",");
            String timestamp = null;
            String v1Signature = null;
            
            for (String part : parts) {
                String[] keyValue = part.split("=", 2);
                if (keyValue.length == 2) {
                    if ("t".equals(keyValue[0])) {
                        timestamp = keyValue[1];
                    } else if ("v1".equals(keyValue[0])) {
                        v1Signature = keyValue[1];
                    }
                }
            }
            
            if (timestamp == null || v1Signature == null) {
                log.warn("Invalid Stripe webhook signature format");
                return false;
            }
            
            // Create expected signature
            String signedPayload = timestamp + "." + payload;
            String expectedSignature = computeHmacSha256(signedPayload, webhookSecret);
            
            return expectedSignature.equals(v1Signature);
            
        } catch (Exception e) {
            log.error("Failed to verify Stripe webhook signature", e);
            return false;
        }
    }

    @Override
    public WebhookEvent parseWebhookEvent(String payload) {
        try {
            // In production, parse JSON payload properly
            // For now, create a mock webhook event
            return new WebhookEvent(
                "evt_" + System.currentTimeMillis(),
                "payment_intent.succeeded",
                "pi_" + System.currentTimeMillis(),
                "succeeded",
                Map.of("amount", "10000", "currency", "usd")
            );
            
        } catch (Exception e) {
            log.error("Failed to parse Stripe webhook event", e);
            return null;
        }
    }

    private String computeHmacSha256(String data, String key) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(secretKey);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}