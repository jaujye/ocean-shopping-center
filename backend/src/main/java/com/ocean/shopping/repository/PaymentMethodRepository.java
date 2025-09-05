package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.PaymentMethod;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.PaymentProvider;
import com.ocean.shopping.model.entity.enums.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    /**
     * Find all active payment methods for a user
     */
    List<PaymentMethod> findByUserAndIsActiveOrderByIsDefaultDescCreatedAtDesc(User user, Boolean isActive);

    /**
     * Find all active payment methods for a user by user ID
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user.id = :userId AND pm.isActive = true ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    List<PaymentMethod> findActivePaymentMethodsByUserId(@Param("userId") Long userId);

    /**
     * Find user's default payment method
     */
    Optional<PaymentMethod> findByUserAndIsDefaultAndIsActive(User user, Boolean isDefault, Boolean isActive);

    /**
     * Find user's default payment method by user ID
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user.id = :userId AND pm.isDefault = true AND pm.isActive = true")
    Optional<PaymentMethod> findDefaultPaymentMethodByUserId(@Param("userId") Long userId);

    /**
     * Find payment method by gateway payment method ID
     */
    Optional<PaymentMethod> findByGatewayPaymentMethodId(String gatewayPaymentMethodId);

    /**
     * Find payment methods by user and provider
     */
    List<PaymentMethod> findByUserAndProviderAndIsActiveOrderByCreatedAtDesc(User user, PaymentProvider provider, Boolean isActive);

    /**
     * Find payment methods by user and type
     */
    List<PaymentMethod> findByUserAndPaymentTypeAndIsActiveOrderByCreatedAtDesc(User user, PaymentType paymentType, Boolean isActive);

    /**
     * Find payment methods by user, provider, and type
     */
    List<PaymentMethod> findByUserAndProviderAndPaymentTypeAndIsActiveOrderByCreatedAtDesc(
            User user, PaymentProvider provider, PaymentType paymentType, Boolean isActive);

    /**
     * Count active payment methods for a user
     */
    long countByUserAndIsActive(User user, Boolean isActive);

    /**
     * Check if user has any active payment methods
     */
    @Query("SELECT COUNT(pm) > 0 FROM PaymentMethod pm WHERE pm.user = :user AND pm.isActive = true")
    boolean hasActivePaymentMethods(@Param("user") User user);

    /**
     * Find expired card payment methods
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.paymentType = 'CARD' AND pm.isActive = true " +
           "AND ((pm.cardExpYear < :currentYear) OR (pm.cardExpYear = :currentYear AND pm.cardExpMonth < :currentMonth))")
    List<PaymentMethod> findExpiredCardPaymentMethods(@Param("currentYear") int currentYear, @Param("currentMonth") int currentMonth);

    /**
     * Find payment methods expiring soon (within 30 days)
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.expiresAt IS NOT NULL " +
           "AND pm.expiresAt BETWEEN :now AND :thirtyDaysFromNow AND pm.isActive = true")
    List<PaymentMethod> findPaymentMethodsExpiringSoon(@Param("now") ZonedDateTime now, @Param("thirtyDaysFromNow") ZonedDateTime thirtyDaysFromNow);

    /**
     * Set all payment methods for a user as non-default
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.user = :user")
    void unsetAllDefaultPaymentMethods(@Param("user") User user);

    /**
     * Set all payment methods for a user as non-default by user ID
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.user.id = :userId")
    void unsetAllDefaultPaymentMethodsByUserId(@Param("userId") Long userId);

    /**
     * Deactivate payment method
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.id = :paymentMethodId")
    void deactivatePaymentMethod(@Param("paymentMethodId") Long paymentMethodId);

    /**
     * Deactivate payment methods by gateway payment method ID
     */
    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.gatewayPaymentMethodId = :gatewayPaymentMethodId")
    void deactivateByGatewayPaymentMethodId(@Param("gatewayPaymentMethodId") String gatewayPaymentMethodId);

    /**
     * Find payment methods by card details (for duplicate detection)
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user = :user AND pm.paymentType = 'CARD' " +
           "AND pm.cardLast4 = :cardLast4 AND pm.cardBrand = :cardBrand AND pm.isActive = true")
    List<PaymentMethod> findByUserAndCardDetails(@Param("user") User user, 
                                                @Param("cardLast4") String cardLast4, 
                                                @Param("cardBrand") String cardBrand);

    /**
     * Find payment methods by bank details (for duplicate detection)
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user = :user AND pm.paymentType = 'BANK_TRANSFER' " +
           "AND pm.bankLast4 = :bankLast4 AND pm.bankName = :bankName AND pm.isActive = true")
    List<PaymentMethod> findByUserAndBankDetails(@Param("user") User user, 
                                                @Param("bankLast4") String bankLast4, 
                                                @Param("bankName") String bankName);

    /**
     * Find all payment methods that need cleanup (inactive for more than specified days)
     */
    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.isActive = false AND pm.updatedAt < :cutoffDate")
    List<PaymentMethod> findInactivePaymentMethodsForCleanup(@Param("cutoffDate") ZonedDateTime cutoffDate);
}