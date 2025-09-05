package com.ocean.shopping.controller.webhook;

import com.ocean.shopping.model.entity.Payment;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import com.ocean.shopping.repository.PaymentRepository;
import com.ocean.shopping.service.payment.PaymentProviderService;
import com.ocean.shopping.service.payment.StripePaymentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.Optional;

/**
 * Stripe webhook controller for handling payment events
 */
@RestController
@RequestMapping("/api/webhooks/stripe")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Stripe Webhooks", description = "Stripe webhook endpoints for payment event processing")
public class StripeWebhookController {

    private final StripePaymentProvider stripePaymentProvider;
    private final PaymentRepository paymentRepository;

    @Operation(summary = "Handle Stripe webhook events", 
               description = "Process Stripe webhook events for payment status updates")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid webhook signature or payload",
                     content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error during webhook processing",
                     content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    @Transactional
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Received Stripe webhook");
        
        try {
            // Verify webhook signature
            boolean isValid = stripePaymentProvider.verifyWebhookSignature(payload, signature, "/api/webhooks/stripe");
            
            if (!isValid) {
                log.warn("Invalid Stripe webhook signature");
                return ResponseEntity.badRequest().body("Invalid webhook signature");
            }

            // Parse webhook event
            PaymentProviderService.WebhookEvent event = stripePaymentProvider.parseWebhookEvent(payload);
            
            if (event == null) {
                log.warn("Failed to parse Stripe webhook event");
                return ResponseEntity.badRequest().body("Invalid webhook payload");
            }

            log.info("Processing Stripe webhook event: {} for object: {}", event.type(), event.objectId());

            // Process the event
            processWebhookEvent(event);

            log.info("Successfully processed Stripe webhook event: {}", event.id());
            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            log.error("Error processing Stripe webhook", e);
            return ResponseEntity.internalServerError().body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * Process different types of webhook events
     */
    private void processWebhookEvent(PaymentProviderService.WebhookEvent event) {
        switch (event.type()) {
            case "payment_intent.succeeded":
                handlePaymentSucceeded(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentFailed(event);
                break;
            case "payment_intent.canceled":
                handlePaymentCanceled(event);
                break;
            case "payment_intent.requires_action":
                handlePaymentRequiresAction(event);
                break;
            case "payment_intent.processing":
                handlePaymentProcessing(event);
                break;
            case "charge.dispute.created":
                handleChargeDispute(event);
                break;
            case "invoice.payment_succeeded":
                handleInvoicePaymentSucceeded(event);
                break;
            case "invoice.payment_failed":
                handleInvoicePaymentFailed(event);
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted(event);
                break;
            default:
                log.info("Unhandled Stripe webhook event type: {}", event.type());
        }
    }

    /**
     * Handle successful payment
     */
    private void handlePaymentSucceeded(PaymentProviderService.WebhookEvent event) {
        Optional<Payment> paymentOpt = findPaymentByGatewayId(event.objectId());
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for successful payment event: {}", event.objectId());
            return;
        }

        Payment payment = paymentOpt.get();
        
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            log.info("Payment {} already marked as succeeded", payment.getId());
            return;
        }

        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setCapturedAt(ZonedDateTime.now());
        
        // Update gateway response with additional data from webhook
        if (event.data() != null && !event.data().isEmpty()) {
            payment.setGatewayResponse(event.data().toString());
        }
        
        paymentRepository.save(payment);
        
        log.info("Payment {} marked as succeeded", payment.getId());
    }

    /**
     * Handle failed payment
     */
    private void handlePaymentFailed(PaymentProviderService.WebhookEvent event) {
        Optional<Payment> paymentOpt = findPaymentByGatewayId(event.objectId());
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for failed payment event: {}", event.objectId());
            return;
        }

        Payment payment = paymentOpt.get();
        
        if (payment.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment {} already marked as failed", payment.getId());
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailedAt(ZonedDateTime.now());
        
        // Extract failure reason from webhook data
        if (event.data() != null && event.data().containsKey("last_payment_error")) {
            Object error = event.data().get("last_payment_error");
            payment.setFailureReason(error.toString());
        }
        
        if (event.data() != null && !event.data().isEmpty()) {
            payment.setGatewayResponse(event.data().toString());
        }
        
        paymentRepository.save(payment);
        
        log.info("Payment {} marked as failed", payment.getId());
    }

    /**
     * Handle canceled payment
     */
    private void handlePaymentCanceled(PaymentProviderService.WebhookEvent event) {
        Optional<Payment> paymentOpt = findPaymentByGatewayId(event.objectId());
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for canceled payment event: {}", event.objectId());
            return;
        }

        Payment payment = paymentOpt.get();
        
        if (payment.getStatus() == PaymentStatus.CANCELLED) {
            log.info("Payment {} already marked as cancelled", payment.getId());
            return;
        }

        payment.setStatus(PaymentStatus.CANCELLED);
        payment.setCancelledAt(ZonedDateTime.now());
        
        if (event.data() != null && !event.data().isEmpty()) {
            payment.setGatewayResponse(event.data().toString());
        }
        
        paymentRepository.save(payment);
        
        log.info("Payment {} marked as cancelled", payment.getId());
    }

    /**
     * Handle payment requiring action
     */
    private void handlePaymentRequiresAction(PaymentProviderService.WebhookEvent event) {
        Optional<Payment> paymentOpt = findPaymentByGatewayId(event.objectId());
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for requires_action event: {}", event.objectId());
            return;
        }

        Payment payment = paymentOpt.get();
        payment.setStatus(PaymentStatus.PROCESSING);
        
        if (event.data() != null && !event.data().isEmpty()) {
            payment.setGatewayResponse(event.data().toString());
        }
        
        paymentRepository.save(payment);
        
        log.info("Payment {} requires action", payment.getId());
    }

    /**
     * Handle payment processing
     */
    private void handlePaymentProcessing(PaymentProviderService.WebhookEvent event) {
        Optional<Payment> paymentOpt = findPaymentByGatewayId(event.objectId());
        
        if (paymentOpt.isEmpty()) {
            log.warn("Payment not found for processing event: {}", event.objectId());
            return;
        }

        Payment payment = paymentOpt.get();
        
        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            payment.setStatus(PaymentStatus.PROCESSING);
            payment.setAuthorizedAt(ZonedDateTime.now());
            
            if (event.data() != null && !event.data().isEmpty()) {
                payment.setGatewayResponse(event.data().toString());
            }
            
            paymentRepository.save(payment);
            
            log.info("Payment {} is now processing", payment.getId());
        }
    }

    /**
     * Handle charge dispute (chargeback)
     */
    private void handleChargeDispute(PaymentProviderService.WebhookEvent event) {
        // This would typically involve creating a dispute record and notifying relevant parties
        log.warn("Charge dispute created for payment: {}", event.objectId());
        
        // For now, just log the event
        // In a real implementation, you would:
        // 1. Find the related payment
        // 2. Create a dispute record
        // 3. Notify the store owner
        // 4. Update payment status if needed
    }

    /**
     * Handle successful invoice payment (for subscriptions or recurring payments)
     */
    private void handleInvoicePaymentSucceeded(PaymentProviderService.WebhookEvent event) {
        log.info("Invoice payment succeeded: {}", event.objectId());
        // Handle subscription or recurring payment success
    }

    /**
     * Handle failed invoice payment
     */
    private void handleInvoicePaymentFailed(PaymentProviderService.WebhookEvent event) {
        log.warn("Invoice payment failed: {}", event.objectId());
        // Handle subscription or recurring payment failure
    }

    /**
     * Handle subscription deletion
     */
    private void handleSubscriptionDeleted(PaymentProviderService.WebhookEvent event) {
        log.info("Subscription deleted: {}", event.objectId());
        // Handle subscription cancellation
    }

    /**
     * Find payment by gateway payment ID
     */
    private Optional<Payment> findPaymentByGatewayId(String gatewayPaymentId) {
        // Try both gateway payment ID and transaction ID
        Optional<Payment> payment = paymentRepository.findByGatewayPaymentId(gatewayPaymentId);
        
        if (payment.isEmpty()) {
            payment = paymentRepository.findByTransactionId(gatewayPaymentId);
        }
        
        return payment;
    }
}