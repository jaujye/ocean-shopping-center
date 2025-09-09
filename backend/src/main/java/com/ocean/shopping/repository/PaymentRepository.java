package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.Payment;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /**
     * Find payment by transaction ID
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Find payment by gateway payment ID
     */
    Optional<Payment> findByGatewayPaymentId(String gatewayPaymentId);

    /**
     * Find all payments for an order
     */
    List<Payment> findByOrderOrderByCreatedAtDesc(Order order);

    /**
     * Find payments by order ID
     */
    List<Payment> findByOrder_IdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Find payments by status
     */
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);

    /**
     * Find payments by provider
     */
    Page<Payment> findByProvider(PaymentProvider provider, Pageable pageable);

    /**
     * Find payments by status and provider
     */
    Page<Payment> findByStatusAndProvider(PaymentStatus status, PaymentProvider provider, Pageable pageable);

    /**
     * Find succeeded payments for an order
     */
    @Query("SELECT p FROM Payment p WHERE p.order = :order AND p.status = 'SUCCEEDED'")
    List<Payment> findSucceededPaymentsByOrder(@Param("order") Order order);

    /**
     * Find refundable payments for an order
     */
    @Query("SELECT p FROM Payment p WHERE p.order = :order AND p.status = 'SUCCEEDED' AND p.refundedAmount < p.amount")
    List<Payment> findRefundablePaymentsByOrder(@Param("order") Order order);

    /**
     * Find payments within date range
     */
    @Query("SELECT p FROM Payment p WHERE p.createdAt >= :startDate AND p.createdAt <= :endDate ORDER BY p.createdAt DESC")
    Page<Payment> findPaymentsInDateRange(@Param("startDate") ZonedDateTime startDate, 
                                         @Param("endDate") ZonedDateTime endDate, 
                                         Pageable pageable);

    /**
     * Find failed payments that need retry
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.failedAt >= :since ORDER BY p.failedAt DESC")
    List<Payment> findFailedPaymentsSince(@Param("since") ZonedDateTime since);

    /**
     * Count payments by status
     */
    long countByStatus(PaymentStatus status);

    /**
     * Count payments by provider and status
     */
    long countByProviderAndStatus(PaymentProvider provider, PaymentStatus status);

    /**
     * Check if order has any succeeded payments
     */
    @Query("SELECT COUNT(p) > 0 FROM Payment p WHERE p.order = :order AND p.status = 'SUCCEEDED'")
    boolean hasSucceededPayments(@Param("order") Order order);

    /**
     * Get total payment amount for an order
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.order = :order AND p.status = 'SUCCEEDED'")
    java.math.BigDecimal getTotalPaymentAmountByOrder(@Param("order") Order order);

    /**
     * Get total refunded amount for an order
     */
    @Query("SELECT COALESCE(SUM(p.refundedAmount), 0) FROM Payment p WHERE p.order = :order")
    java.math.BigDecimal getTotalRefundedAmountByOrder(@Param("order") Order order);

    /**
     * Find payments that need webhook retry
     */
    @Query("SELECT p FROM Payment p WHERE p.status = 'PROCESSING' AND p.createdAt < :timeoutThreshold")
    List<Payment> findPaymentsNeedingWebhookRetry(@Param("timeoutThreshold") ZonedDateTime timeoutThreshold);

    /**
     * Find recent payments by user
     */
    @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.createdAt DESC")
    Page<Payment> findRecentPaymentsByUser(@Param("userId") UUID userId, Pageable pageable);

    /**
     * Find payments by store
     */
    @Query("SELECT p FROM Payment p WHERE p.order.store.id = :storeId ORDER BY p.createdAt DESC")
    Page<Payment> findPaymentsByStore(@Param("storeId") UUID storeId, Pageable pageable);
}