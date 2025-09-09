package com.ocean.shopping.service;

import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import com.ocean.shopping.model.entity.enums.PaymentType;
import com.ocean.shopping.repository.PaymentMethodRepository;
import com.ocean.shopping.repository.PaymentRepository;
import com.ocean.shopping.service.payment.PaymentProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for payment operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final List<PaymentProviderService> paymentProviders;

    /**
     * Create a payment intent for an order
     */
    @Transactional
    public PaymentProviderService.PaymentIntent createPaymentIntent(Order order, PaymentProvider provider) {
        PaymentProviderService providerService = getPaymentProvider(provider);
        
        Map<String, String> metadata = Map.of(
            "order_id", order.getId().toString(),
            "order_number", order.getOrderNumber(),
            "customer_email", order.getCustomerEmail()
        );
        
        return providerService.createPaymentIntent(
            order.getTotalAmount(),
            order.getCurrency(),
            metadata
        );
    }

    /**
     * Process payment for an order
     */
    @Transactional
    public Payment processPayment(Order order, String paymentMethodId, PaymentProvider provider) {
        PaymentProviderService providerService = getPaymentProvider(provider);
        
        // Create payment record
        Payment payment = createPaymentRecord(order, provider, PaymentType.CARD);
        
        try {
            Map<String, String> metadata = Map.of(
                "order_id", order.getId().toString(),
                "payment_id", payment.getId().toString()
            );
            
            PaymentProviderService.PaymentResult result;
            
            if (paymentMethodId.startsWith("pm_")) {
                // Using saved payment method
                PaymentMethod paymentMethod = paymentMethodRepository
                    .findByGatewayPaymentMethodId(paymentMethodId)
                    .orElseThrow(() -> new RuntimeException("Payment method not found"));
                
                payment.setPaymentMethod(paymentMethod);
                result = providerService.processPayment(paymentMethod, order.getTotalAmount(), order.getCurrency(), metadata);
            } else {
                // One-time payment
                result = providerService.confirmPayment(payment.getTransactionId(), paymentMethodId, metadata);
            }
            
            updatePaymentFromResult(payment, result);
            
            if (result.success()) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setCapturedAt(ZonedDateTime.now());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.failureReason());
                payment.setFailedAt(ZonedDateTime.now());
            }
            
            return paymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("Failed to process payment for order: {}", order.getId(), e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment processing failed: " + e.getMessage());
            payment.setFailedAt(ZonedDateTime.now());
            return paymentRepository.save(payment);
        }
    }

    /**
     * Capture an authorized payment
     */
    @Transactional
    public Payment capturePayment(Long paymentId, BigDecimal amount) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment is not in a capturable state");
        }
        
        PaymentProviderService providerService = getPaymentProvider(payment.getProvider());
        
        try {
            PaymentProviderService.PaymentResult result = providerService.capturePayment(
                payment.getGatewayPaymentId(),
                amount != null ? amount : payment.getAmount()
            );
            
            updatePaymentFromResult(payment, result);
            
            if (result.success()) {
                payment.setStatus(PaymentStatus.SUCCEEDED);
                payment.setCapturedAt(ZonedDateTime.now());
                if (amount != null) {
                    payment.setAmount(amount);
                }
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.failureReason());
                payment.setFailedAt(ZonedDateTime.now());
            }
            
            return paymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("Failed to capture payment: {}", paymentId, e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Capture failed: " + e.getMessage());
            payment.setFailedAt(ZonedDateTime.now());
            return paymentRepository.save(payment);
        }
    }

    /**
     * Refund a payment
     */
    @Transactional
    public Payment refundPayment(Long paymentId, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (!payment.isRefundable()) {
            throw new IllegalStateException("Payment is not refundable");
        }
        
        BigDecimal maxRefund = payment.getRefundableAmount();
        if (refundAmount.compareTo(maxRefund) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds refundable amount");
        }
        
        PaymentProviderService providerService = getPaymentProvider(payment.getProvider());
        
        try {
            PaymentProviderService.RefundResult result = providerService.refundPayment(
                payment.getGatewayPaymentId(),
                refundAmount,
                reason
            );
            
            if (result.success()) {
                BigDecimal newRefundedAmount = payment.getRefundedAmount().add(refundAmount);
                payment.setRefundedAmount(newRefundedAmount);
                
                if (newRefundedAmount.compareTo(payment.getAmount()) >= 0) {
                    payment.setStatus(PaymentStatus.REFUNDED);
                } else {
                    payment.setStatus(PaymentStatus.PARTIALLY_REFUNDED);
                }
                
                payment.setRefundedAt(ZonedDateTime.now());
            }
            
            return paymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("Failed to refund payment: {}", paymentId, e);
            throw new RuntimeException("Refund failed: " + e.getMessage(), e);
        }
    }

    /**
     * Cancel a payment
     */
    @Transactional
    public Payment cancelPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
        
        if (payment.getStatus() != PaymentStatus.PENDING && payment.getStatus() != PaymentStatus.PROCESSING) {
            throw new IllegalStateException("Payment cannot be cancelled in current state");
        }
        
        PaymentProviderService providerService = getPaymentProvider(payment.getProvider());
        
        try {
            PaymentProviderService.PaymentResult result = providerService.cancelPayment(payment.getGatewayPaymentId());
            
            if (result.success()) {
                payment.setStatus(PaymentStatus.CANCELLED);
                payment.setCancelledAt(ZonedDateTime.now());
            }
            
            return paymentRepository.save(payment);
            
        } catch (Exception e) {
            log.error("Failed to cancel payment: {}", paymentId, e);
            throw new RuntimeException("Cancel failed: " + e.getMessage(), e);
        }
    }

    /**
     * Save a payment method for future use
     */
    @Transactional
    public PaymentMethod savePaymentMethod(User user, String paymentMethodToken, PaymentProvider provider, boolean setAsDefault) {
        PaymentProviderService providerService = getPaymentProvider(provider);
        
        try {
            PaymentProviderService.PaymentMethodResult result = providerService.savePaymentMethod(user, paymentMethodToken);
            
            if (!result.success()) {
                throw new RuntimeException("Failed to save payment method: " + result.failureReason());
            }
            
            PaymentProviderService.PaymentMethodDetails details = result.details();
            
            // Check for duplicates
            if (details.cardLast4() != null && details.cardBrand() != null) {
                List<PaymentMethod> existing = paymentMethodRepository.findByUserAndCardDetails(
                    user, details.cardLast4(), details.cardBrand()
                );
                if (!existing.isEmpty()) {
                    throw new RuntimeException("Payment method already exists");
                }
            }
            
            // Create PaymentMethod entity
            PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .provider(provider)
                .paymentType(PaymentType.valueOf(details.type().toUpperCase()))
                .gatewayPaymentMethodId(result.paymentMethodId())
                .displayName(details.displayName())
                .cardLast4(details.cardLast4())
                .cardBrand(details.cardBrand())
                .cardExpMonth(details.cardExpMonth())
                .cardExpYear(details.cardExpYear())
                .cardHolderName(details.cardHolderName())
                .bankLast4(details.bankLast4())
                .bankName(details.bankName())
                .accountType(details.accountType())
                .walletEmail(details.walletEmail())
                .isDefault(false)
                .isActive(true)
                .build();
            
            paymentMethod = paymentMethodRepository.save(paymentMethod);
            
            if (setAsDefault) {
                setDefaultPaymentMethod(user, paymentMethod.getId());
            }
            
            return paymentMethod;
            
        } catch (Exception e) {
            log.error("Failed to save payment method for user: {}", user.getId(), e);
            throw new RuntimeException("Failed to save payment method", e);
        }
    }

    /**
     * Set a payment method as default
     */
    @Transactional
    public void setDefaultPaymentMethod(User user, Long paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
            .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        if (!paymentMethod.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Payment method does not belong to user");
        }
        
        // Unset all current defaults
        paymentMethodRepository.unsetAllDefaultPaymentMethods(user);
        
        // Set new default
        paymentMethod.setIsDefault(true);
        paymentMethodRepository.save(paymentMethod);
    }

    /**
     * Remove a payment method
     */
    @Transactional
    public void removePaymentMethod(User user, Long paymentMethodId) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
            .orElseThrow(() -> new RuntimeException("Payment method not found"));
        
        if (!paymentMethod.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Payment method does not belong to user");
        }
        
        PaymentProviderService providerService = getPaymentProvider(paymentMethod.getProvider());
        
        try {
            // Remove from gateway
            boolean removed = providerService.removePaymentMethod(paymentMethod.getGatewayPaymentMethodId());
            
            if (removed) {
                // Soft delete (deactivate)
                paymentMethodRepository.deactivatePaymentMethod(paymentMethodId);
                
                // If this was the default, find another one to set as default
                if (paymentMethod.getIsDefault()) {
                    List<PaymentMethod> activeMethods = paymentMethodRepository
                        .findActivePaymentMethodsByUserId(user.getId());
                    
                    if (!activeMethods.isEmpty()) {
                        activeMethods.get(0).setIsDefault(true);
                        paymentMethodRepository.save(activeMethods.get(0));
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to remove payment method: {}", paymentMethodId, e);
            throw new RuntimeException("Failed to remove payment method", e);
        }
    }

    /**
     * Get user's payment methods
     */
    public List<PaymentMethod> getUserPaymentMethods(User user) {
        return paymentMethodRepository.findActivePaymentMethodsByUserId(user.getId());
    }

    /**
     * Get user's default payment method
     */
    public Optional<PaymentMethod> getUserDefaultPaymentMethod(User user) {
        return paymentMethodRepository.findDefaultPaymentMethodByUserId(user.getId());
    }

    /**
     * Get payments for an order
     */
    public List<Payment> getOrderPayments(Long orderId) {
        return paymentRepository.findByOrder_IdOrderByCreatedAtDesc(orderId);
    }

    /**
     * Get payments by status
     */
    public Page<Payment> getPaymentsByStatus(PaymentStatus status, Pageable pageable) {
        return paymentRepository.findByStatus(status, pageable);
    }

    /**
     * Get user payments
     */
    public Page<Payment> getUserPayments(Long userId, Pageable pageable) {
        return paymentRepository.findRecentPaymentsByUser(userId, pageable);
    }

    /**
     * Get store payments
     */
    public Page<Payment> getStorePayments(Long storeId, Pageable pageable) {
        return paymentRepository.findPaymentsByStore(storeId, pageable);
    }

    private PaymentProviderService getPaymentProvider(PaymentProvider provider) {
        return paymentProviders.stream()
            .filter(p -> p.getProvider() == provider)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Payment provider not found: " + provider));
    }

    private Payment createPaymentRecord(Order order, PaymentProvider provider, PaymentType paymentType) {
        return Payment.builder()
            .order(order)
            .transactionId(generateTransactionId())
            .provider(provider)
            .paymentType(paymentType)
            .status(PaymentStatus.PENDING)
            .amount(order.getTotalAmount())
            .currency(order.getCurrency())
            .refundedAmount(BigDecimal.ZERO)
            .build();
    }

    private void updatePaymentFromResult(Payment payment, PaymentProviderService.PaymentResult result) {
        payment.setGatewayPaymentId(result.transactionId());
        if (result.gatewayResponse() != null) {
            payment.setGatewayResponse(result.gatewayResponse().toString());
        }
    }

    private String generateTransactionId() {
        return "txn_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Process refund for an order by finding the latest successful payment
     */
    @Transactional
    public void processRefund(Long orderId, BigDecimal refundAmount, String reason) {
        log.info("Processing refund for order {} - amount: {}", orderId, refundAmount);
        
        // Find the most recent successful payment for this order
        List<Payment> payments = paymentRepository.findByOrder_IdOrderByCreatedAtDesc(orderId);
        Payment payment = payments.stream()
            .filter(p -> p.getStatus() == PaymentStatus.SUCCEEDED)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No successful payment found for order " + orderId));
        
        // Process the refund
        refundPayment(payment.getId(), refundAmount, reason);
    }
}