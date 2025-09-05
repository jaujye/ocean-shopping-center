package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.CartItem;
import com.ocean.shopping.model.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for CartItem entity with efficient queries for cart management.
 * Provides optimized queries for cart operations, inventory management, and analytics.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID>, JpaSpecificationExecutor<CartItem> {

    // Basic cart item queries
    List<CartItem> findByCartIdAndSavedForLaterFalseOrderByCreatedAt(UUID cartId);
    
    List<CartItem> findByCartIdOrderByCreatedAt(UUID cartId);
    
    List<CartItem> findByCartIdAndSavedForLaterTrueOrderByCreatedAt(UUID cartId);

    // Product-based queries
    List<CartItem> findByProductId(UUID productId);
    
    List<CartItem> findByProductIdAndCartStatus(UUID productId, com.ocean.shopping.model.entity.Cart.CartStatus cartStatus);
    
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId AND ci.productVariant IS NULL")
    Optional<CartItem> findByCartAndProductWithoutVariant(@Param("cartId") UUID cartId, @Param("productId") UUID productId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId AND ci.productVariant.id = :variantId")
    Optional<CartItem> findByCartProductAndVariant(@Param("cartId") UUID cartId, 
                                                  @Param("productId") UUID productId, 
                                                  @Param("variantId") UUID variantId);

    // User-based queries
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.cart.status = 'ACTIVE' AND ci.savedForLater = false")
    List<CartItem> findActiveItemsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.savedForLater = true")
    List<CartItem> findSavedItemsByUserId(@Param("userId") UUID userId);

    // Session-based queries
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.sessionId = :sessionId AND ci.cart.status = 'ACTIVE' AND ci.savedForLater = false")
    List<CartItem> findActiveItemsBySessionId(@Param("sessionId") String sessionId);

    // Quantity and inventory queries
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.product.id = :productId AND ci.cart.status = 'ACTIVE'")
    Long getTotalQuantityInCartsForProduct(@Param("productId") UUID productId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.productVariant.id = :variantId AND ci.cart.status = 'ACTIVE'")
    Long getTotalQuantityInCartsForVariant(@Param("variantId") UUID variantId);
    
    @Query("SELECT ci.product.id, SUM(ci.quantity) FROM CartItem ci " +
           "WHERE ci.cart.status = 'ACTIVE' " +
           "GROUP BY ci.product.id")
    List<Object[]> getProductQuantitiesInActiveCarts();

    // Cart totals and statistics
    @Query("SELECT SUM(ci.itemTotal) FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = false")
    BigDecimal getTotalAmountByCartId(@Param("cartId") UUID cartId);
    
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = false")
    Long getTotalQuantityByCartId(@Param("cartId") UUID cartId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = false")
    long countActiveItemsByCartId(@Param("cartId") UUID cartId);

    // Gift items
    List<CartItem> findByCartIdAndIsGiftTrue(UUID cartId);
    
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.isGift = true AND ci.cart.status = 'ACTIVE'")
    long countGiftItemsByUserId(@Param("userId") UUID userId);

    // Discount and pricing queries
    @Query("SELECT ci FROM CartItem ci WHERE ci.discountAmount > 0 AND ci.cart.status = 'ACTIVE'")
    List<CartItem> findDiscountedItems();
    
    @Query("SELECT SUM(ci.discountAmount) FROM CartItem ci WHERE ci.cart.id = :cartId")
    BigDecimal getTotalDiscountByCartId(@Param("cartId") UUID cartId);

    // Wishlist/Save for later operations
    @Modifying
    @Query("UPDATE CartItem ci SET ci.savedForLater = true WHERE ci.id = :itemId")
    int saveItemForLater(@Param("itemId") UUID itemId);
    
    @Modifying
    @Query("UPDATE CartItem ci SET ci.savedForLater = false WHERE ci.id = :itemId")
    int moveItemToCart(@Param("itemId") UUID itemId);

    // Bulk operations
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") UUID cartId);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = :savedForLater")
    int deleteByCartIdAndSavedForLater(@Param("cartId") UUID cartId, @Param("savedForLater") boolean savedForLater);
    
    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.product.id = :productId")
    int deleteAllByProductId(@Param("productId") UUID productId);

    // Invalid items (out of stock, inactive products)
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.status = 'ACTIVE' AND " +
           "(ci.product.isActive = false OR " +
           "(ci.product.trackInventory = true AND ci.product.inventoryQuantity < ci.quantity))")
    List<CartItem> findInvalidItems();
    
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.id = :cartId AND " +
           "(ci.product.isActive = false OR " +
           "(ci.product.trackInventory = true AND ci.product.inventoryQuantity < ci.quantity))")
    List<CartItem> findInvalidItemsByCartId(@Param("cartId") UUID cartId);

    // Recently added items
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.createdAt >= :since ORDER BY ci.createdAt DESC")
    List<CartItem> findRecentItemsByUserId(@Param("userId") UUID userId, @Param("since") ZonedDateTime since);

    // Popular products in carts
    @Query("SELECT ci.product, COUNT(ci) as itemCount FROM CartItem ci " +
           "WHERE ci.cart.status = 'ACTIVE' AND ci.cart.updatedAt >= :since " +
           "GROUP BY ci.product " +
           "ORDER BY itemCount DESC")
    List<Object[]> findPopularProductsInCarts(@Param("since") ZonedDateTime since);

    // Abandoned cart analysis
    @Query("SELECT ci FROM CartItem ci " +
           "WHERE ci.cart.status = 'ACTIVE' AND ci.cart.updatedAt < :threshold")
    List<CartItem> findItemsInAbandonedCarts(@Param("threshold") ZonedDateTime threshold);

    // Store-based queries
    @Query("SELECT ci FROM CartItem ci WHERE ci.product.store.id = :storeId AND ci.cart.status = 'ACTIVE'")
    List<CartItem> findActiveItemsByStoreId(@Param("storeId") UUID storeId);
    
    @Query("SELECT SUM(ci.itemTotal) FROM CartItem ci WHERE ci.product.store.id = :storeId AND ci.cart.status = 'ACTIVE'")
    BigDecimal getTotalValueByStoreId(@Param("storeId") UUID storeId);

    // Category-based queries
    @Query("SELECT ci FROM CartItem ci WHERE ci.product.category.id = :categoryId AND ci.cart.status = 'ACTIVE'")
    List<CartItem> findActiveItemsByCategoryId(@Param("categoryId") UUID categoryId);

    // Pricing validation
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.status = 'ACTIVE' AND ci.unitPrice != ci.product.price")
    List<CartItem> findItemsWithOutdatedPrices();

    // Cart item age analysis
    @Query("SELECT AVG(FUNCTION('EXTRACT', EPOCH FROM (CURRENT_TIMESTAMP - ci.createdAt))) / 3600 " +
           "FROM CartItem ci WHERE ci.cart.status = 'ACTIVE'")
    Double getAverageItemAgeInHours();

    // Item count by user
    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.cart.status = 'ACTIVE' AND ci.savedForLater = false")
    long countActiveItemsByUserId(@Param("userId") UUID userId);

    // Duplicate item detection
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.product.id = :productId " +
           "AND (:variantId IS NULL OR ci.productVariant.id = :variantId)")
    List<CartItem> findDuplicateItems(@Param("cartId") UUID cartId, 
                                     @Param("productId") UUID productId, 
                                     @Param("variantId") UUID variantId);

    // Performance optimization queries
    @Query("SELECT ci.id FROM CartItem ci WHERE ci.cart.id = :cartId")
    List<UUID> findItemIdsByCartId(@Param("cartId") UUID cartId);
    
    @Query("SELECT ci.product.id, ci.quantity FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.savedForLater = false")
    List<Object[]> findProductQuantitiesByCartId(@Param("cartId") UUID cartId);

    // Custom option queries
    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND SIZE(ci.selectedOptions) > 0")
    List<CartItem> findItemsWithOptions(@Param("cartId") UUID cartId);
    
    boolean existsByCartIdAndProductId(UUID cartId, UUID productId);
    
    boolean existsByCartIdAndProductIdAndProductVariantId(UUID cartId, UUID productId, UUID variantId);
}