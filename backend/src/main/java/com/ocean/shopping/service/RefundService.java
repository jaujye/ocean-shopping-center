package com.ocean.shopping.service;

import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.Payment;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import com.ocean.shopping.repository.OrderRepository;
import com.ocean.shopping.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service for handling refunds and payment reversals
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RefundService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    /**
     * Process full refund for an order
     */
    @Transactional
    public void processFullRefund(UUID orderId, String reason) {
        log.info("Processing full refund for order ID: {} with reason: {}", orderId, reason);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        validateRefundEligibility(order);

        List<Payment> refundablePayments = paymentRepository.findRefundablePaymentsByOrder(order);
        if (refundablePayments.isEmpty()) {
            throw new BadRequestException("No refundable payments found for this order");
        }

        BigDecimal totalRefundAmount = BigDecimal.ZERO;

        try {
            // Process refunds for each payment
            for (Payment payment : refundablePayments) {
                BigDecimal refundableAmount = payment.getRefundableAmount();
                if (refundableAmount.compareTo(BigDecimal.ZERO) > 0) {
                    processPaymentRefund(payment, refundableAmount, reason);
                    totalRefundAmount = totalRefundAmount.add(refundableAmount);
                }
            }

            // Update order status
            order.setStatus(OrderStatus.REFUNDED);
            order.setInternalNotes(appendNote(order.getInternalNotes(), 
                "Full refund processed: " + totalRefundAmount + " " + order.getCurrency() + ". Reason: " + reason));
            orderRepository.save(order);

            // Send refund confirmation email
            notificationService.sendRefundConfirmationEmail(
                order.getCustomerEmail(),
                order.getBillingFullName(),
                order,
                totalRefundAmount,
                reason
            );

            log.info("Successfully processed full refund of {} for order {}", totalRefundAmount, order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error processing full refund for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw new BadRequestException("Failed to process refund: " + e.getMessage());
        }
    }

    /**
     * Process partial refund for an order
     */
    @Transactional
    public void processPartialRefund(UUID orderId, BigDecimal refundAmount, String reason) {
        log.info("Processing partial refund of {} for order ID: {}", refundAmount, orderId);

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        validateRefundEligibility(order);
        validatePartialRefundAmount(order, refundAmount);

        List<Payment> refundablePayments = paymentRepository.findRefundablePaymentsByOrder(order);
        if (refundablePayments.isEmpty()) {
            throw new BadRequestException("No refundable payments found for this order");
        }

        try {
            BigDecimal remainingRefundAmount = refundAmount;

            // Distribute refund across payments (LIFO - last payment first)
            for (int i = refundablePayments.size() - 1; i >= 0 && remainingRefundAmount.compareTo(BigDecimal.ZERO) > 0; i--) {
                Payment payment = refundablePayments.get(i);
                BigDecimal paymentRefundableAmount = payment.getRefundableAmount();
                
                if (paymentRefundableAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal refundForThisPayment = remainingRefundAmount.min(paymentRefundableAmount);
                    processPaymentRefund(payment, refundForThisPayment, reason);
                    remainingRefundAmount = remainingRefundAmount.subtract(refundForThisPayment);
                }
            }

            // Update order status if partially refunded
            if (order.getStatus() != OrderStatus.REFUNDED) {
                order.setStatus(OrderStatus.PARTIALLY_REFUNDED);
            }
            order.setInternalNotes(appendNote(order.getInternalNotes(), 
                "Partial refund processed: " + refundAmount + " " + order.getCurrency() + ". Reason: " + reason));
            orderRepository.save(order);

            // Send partial refund confirmation email
            notificationService.sendRefundConfirmationEmail(
                order.getCustomerEmail(),
                order.getBillingFullName(),
                order,
                refundAmount,
                reason
            );

            log.info("Successfully processed partial refund of {} for order {}", refundAmount, order.getOrderNumber());

        } catch (Exception e) {
            log.error("Error processing partial refund for order {}: {}", order.getOrderNumber(), e.getMessage(), e);
            throw new BadRequestException("Failed to process partial refund: " + e.getMessage());
        }
    }

    /**
     * Process refund for a specific payment
     */
    @Transactional
    public void processPaymentRefund(UUID paymentId, BigDecimal refundAmount, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        validatePaymentRefundEligibility(payment);
        
        if (refundAmount.compareTo(payment.getRefundableAmount()) > 0) {
            throw new BadRequestException("Refund amount cannot exceed refundable amount");
        }

        processPaymentRefund(payment, refundAmount, reason);
    }

    /**
     * Check if order is eligible for refund
     */
    public boolean isRefundEligible(UUID orderId) {
        try {
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
            
            validateRefundEligibility(order);
            
            List<Payment> refundablePayments = paymentRepository.findRefundablePaymentsByOrder(order);
            return !refundablePayments.isEmpty();
            
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get total refundable amount for an order
     */
    public BigDecimal getRefundableAmount(UUID orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        List<Payment> refundablePayments = paymentRepository.findRefundablePaymentsByOrder(order);
        
        return refundablePayments.stream()
            .map(Payment::getRefundableAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Cancel refund (if supported by payment provider)
     */
    @Transactional
    public void cancelRefund(UUID paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found: " + paymentId));

        if (!payment.isRefunded() && !payment.isPartiallyRefunded()) {
            throw new BadRequestException("Payment is not in refunded state");
        }

        // Note: Actual refund cancellation depends on payment provider capabilities
        // This is a placeholder implementation
        log.warn("Refund cancellation requested for payment {} but not implemented for provider {}", 
                paymentId, payment.getProvider());
        
        throw new BadRequestException("Refund cancellation not supported for this payment method");
    }

    // Private helper methods

    private void processPaymentRefund(Payment payment, BigDecimal refundAmount, String reason) {
        try {
            // Process refund through payment service (which handles provider calls and updates)
            Payment updatedPayment = paymentService.refundPayment(payment.getId(), refundAmount, reason);
            
            // Additional internal notes if needed
            String metadata = updatedPayment.getMetadata() != null ? updatedPayment.getMetadata() : "";
            if (!metadata.contains(reason)) {
                metadata += "\nRefund reason: " + reason;
                updatedPayment.setMetadata(metadata);
                paymentRepository.save(updatedPayment);
            }

            log.info("Successfully processed refund of {} for payment {}", refundAmount, payment.getTransactionId());

        } catch (Exception e) {
            log.error("Error processing refund for payment {}: {}", payment.getTransactionId(), e.getMessage(), e);
            throw new BadRequestException("Failed to process payment refund: " + e.getMessage());
        }
    }

    private void validateRefundEligibility(Order order) {
        if (order.getStatus() == OrderStatus.CANCELLED && order.getCancelledAt() == null) {
            throw new BadRequestException("Order is cancelled but cancellation date is not set");
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            throw new BadRequestException("Cannot refund pending orders - cancel the order instead");
        }

        // Check if refund window has expired (e.g., 30 days)
        if (order.getDeliveredAt() != null) {
            ZonedDateTime refundDeadline = order.getDeliveredAt().plusDays(30);
            if (ZonedDateTime.now().isAfter(refundDeadline)) {
                throw new BadRequestException("Refund window has expired (30 days after delivery)");
            }
        }
    }

    private void validatePaymentRefundEligibility(Payment payment) {
        if (!payment.isSucceeded()) {
            throw new BadRequestException("Can only refund succeeded payments");
        }

        if (!payment.isRefundable()) {
            throw new BadRequestException("Payment has no refundable amount");
        }
    }

    private void validatePartialRefundAmount(Order order, BigDecimal refundAmount) {
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Refund amount must be positive");
        }

        BigDecimal totalRefundableAmount = getRefundableAmount(order.getId());
        if (refundAmount.compareTo(totalRefundableAmount) > 0) {
            throw new BadRequestException("Refund amount cannot exceed total refundable amount: " + totalRefundableAmount);
        }
    }

    private String appendNote(String existingNotes, String newNote) {
        if (existingNotes == null || existingNotes.trim().isEmpty()) {
            return newNote;
        }
        return existingNotes + "\n" + newNote;
    }
}