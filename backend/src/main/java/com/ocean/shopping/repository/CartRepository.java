package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Cart;
import com.ocean.shopping.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Cart entity with Redis session support and comprehensive cart management.
 * Handles both authenticated user carts and guest session-based carts.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, UUID>, JpaSpecificationExecutor<Cart> {

    // User-based cart queries
    Optional<Cart> findByUserAndStatus(User user, Cart.CartStatus status);
    
    Optional<Cart> findByUserIdAndStatus(UUID userId, Cart.CartStatus status);
    
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId AND c.status = :status")
    Optional<Cart> findActiveCartByUserId(@Param("userId") UUID userId, @Param("status") Cart.CartStatus status);
    
    List<Cart> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    
    List<Cart> findByUserAndStatusOrderByUpdatedAtDesc(User user, Cart.CartStatus status);

    // Session-based cart queries
    Optional<Cart> findBySessionIdAndStatus(String sessionId, Cart.CartStatus status);
    
    @Query("SELECT c FROM Cart c WHERE c.sessionId = :sessionId AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveSessionCart(@Param("sessionId") String sessionId);
    
    List<Cart> findBySessionIdOrderByUpdatedAtDesc(String sessionId);

    // Cart status management
    List<Cart> findByStatus(Cart.CartStatus status);
    
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.updatedAt < :threshold")
    List<Cart> findActiveCartsOlderThan(@Param("threshold") ZonedDateTime threshold);
    
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    List<Cart> findExpiredCarts(@Param("now") ZonedDateTime now);

    // Empty cart management
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND SIZE(c.items) = 0")
    List<Cart> findEmptyActiveCarts();
    
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND SIZE(c.items) = 0 AND c.updatedAt < :threshold")
    List<Cart> findEmptyCartsOlderThan(@Param("threshold") ZonedDateTime threshold);

    // Cart item count queries
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = false")
    long countActiveItemsByCartId(@Param("cartId") UUID cartId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.savedForLater = false")
    long countActiveItemsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = false")
    Long getTotalQuantityByCartId(@Param("cartId") UUID cartId);

    // Abandoned cart analysis
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.updatedAt BETWEEN :startDate AND :endDate AND SIZE(c.items) > 0")
    List<Cart> findAbandonedCarts(@Param("startDate") ZonedDateTime startDate, @Param("endDate") ZonedDateTime endDate);
    
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId AND c.status = 'ABANDONED'")
    List<Cart> findAbandonedCartsByUser(@Param("userId") UUID userId);

    // Cart merging support
    @Query("SELECT c FROM Cart c WHERE c.mergedFromSession = :sessionId")
    List<Cart> findCartsMergedFromSession(@Param("sessionId") String sessionId);

    // Bulk operations
    @Modifying
    @Query("UPDATE Cart c SET c.status = 'ABANDONED' WHERE c.status = 'ACTIVE' AND c.updatedAt < :threshold")
    int markOldCartsAsAbandoned(@Param("threshold") ZonedDateTime threshold);
    
    @Modifying
    @Query("UPDATE Cart c SET c.status = 'EXPIRED' WHERE c.expiresAt IS NOT NULL AND c.expiresAt < :now")
    int markExpiredCarts(@Param("now") ZonedDateTime now);
    
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.status IN ('EXPIRED', 'MERGED') AND c.updatedAt < :threshold")
    int deleteOldProcessedCarts(@Param("threshold") ZonedDateTime threshold);
    
    @Modifying
    @Query("DELETE FROM Cart c WHERE c.status = 'ACTIVE' AND SIZE(c.items) = 0 AND c.updatedAt < :threshold")
    int deleteEmptyOldCarts(@Param("threshold") ZonedDateTime threshold);

    // Analytics and reporting
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.status = 'ACTIVE' AND c.user IS NOT NULL")
    long countActiveUserCarts();
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.status = 'ACTIVE' AND c.sessionId IS NOT NULL")
    long countActiveSessionCarts();
    
    @Query("SELECT AVG(SIZE(c.items)) FROM Cart c WHERE c.status = 'ACTIVE' AND SIZE(c.items) > 0")
    Double getAverageCartSize();
    
    @Query("SELECT AVG(c.total) FROM Cart c WHERE c.status = 'ACTIVE' AND c.total > 0")
    Double getAverageCartValue();

    // Cart conversion tracking
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.status = 'CONVERTED' AND c.updatedAt >= :since")
    long countConvertedCartsSince(@Param("since") ZonedDateTime since);
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.status = 'ABANDONED' AND c.updatedAt >= :since")
    long countAbandonedCartsSince(@Param("since") ZonedDateTime since);

    // Advanced search and filtering
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.total BETWEEN :minValue AND :maxValue")
    List<Cart> findCartsByValueRange(@Param("minValue") java.math.BigDecimal minValue, 
                                    @Param("maxValue") java.math.BigDecimal maxValue);
    
    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND SIZE(c.items) BETWEEN :minItems AND :maxItems")
    List<Cart> findCartsByItemCountRange(@Param("minItems") int minItems, @Param("maxItems") int maxItems);

    // User cart history
    @Query("SELECT c FROM Cart c WHERE c.user.id = :userId ORDER BY c.updatedAt DESC")
    List<Cart> findCartHistoryByUser(@Param("userId") UUID userId);

    // Coupon usage tracking
    @Query("SELECT c FROM Cart c WHERE c.appliedCouponCode = :couponCode AND c.status = 'ACTIVE'")
    List<Cart> findActiveCartsWithCoupon(@Param("couponCode") String couponCode);
    
    @Query("SELECT COUNT(c) FROM Cart c WHERE c.appliedCouponCode = :couponCode")
    long countCartsUsingCoupon(@Param("couponCode") String couponCode);

    // Cart existence checks
    boolean existsByUserAndStatus(User user, Cart.CartStatus status);
    
    boolean existsByUserIdAndStatus(UUID userId, Cart.CartStatus status);
    
    boolean existsBySessionIdAndStatus(String sessionId, Cart.CartStatus status);

    // Performance optimization queries
    @Query("SELECT c.id FROM Cart c WHERE c.status = 'ACTIVE'")
    List<UUID> findActiveCartIds();
    
    @Query("SELECT c.id, c.user.id FROM Cart c WHERE c.status = 'ACTIVE' AND c.user IS NOT NULL")
    List<Object[]> findActiveUserCartInfo();
}