package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Product entity with advanced search capabilities
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    // Basic finder methods
    Optional<Product> findBySlugAndStoreId(String slug, UUID storeId);

    Optional<Product> findBySkuAndStoreId(String sku, UUID storeId);

    List<Product> findByStoreIdAndIsActiveTrue(UUID storeId);

    List<Product> findByStoreIdAndIsActiveTrueOrderByCreatedAtDesc(UUID storeId, Pageable pageable);

    Page<Product> findByStoreIdAndIsActiveTrue(UUID storeId, Pageable pageable);

    List<Product> findByCategoryIdAndIsActiveTrue(UUID categoryId);

    Page<Product> findByCategoryIdAndIsActiveTrue(UUID categoryId, Pageable pageable);

    List<Product> findByIsFeaturedTrueAndIsActiveTrue();

    Page<Product> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);

    List<Product> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    // Count methods
    long countByStoreId(UUID storeId);

    long countByStoreIdAndIsActiveTrue(UUID storeId);

    long countByCategoryId(UUID categoryId);

    long countByCategoryIdAndIsActiveTrue(UUID categoryId);

    // Inventory management
    @Query("SELECT p FROM Product p WHERE p.trackInventory = true AND p.inventoryQuantity <= p.lowStockThreshold AND p.isActive = true")
    List<Product> findLowStockProducts();

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.trackInventory = true AND p.inventoryQuantity <= p.lowStockThreshold AND p.isActive = true")
    List<Product> findLowStockProductsByStore(@Param("storeId") UUID storeId);

    @Query("SELECT p FROM Product p WHERE p.trackInventory = true AND p.inventoryQuantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProducts();

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.trackInventory = true AND p.inventoryQuantity = 0 AND p.isActive = true")
    List<Product> findOutOfStockProductsByStore(@Param("storeId") UUID storeId);

    // Search by text
    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "WHERE p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN p.category c " +
           "WHERE p.store.id = :storeId AND p.isActive = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProductsByStore(@Param("query") String query, @Param("storeId") UUID storeId, Pageable pageable);

    // Price range filtering
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByPriceRange(@Param("minPrice") BigDecimal minPrice, 
                                   @Param("maxPrice") BigDecimal maxPrice, 
                                   Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.isActive = true AND p.price BETWEEN :minPrice AND :maxPrice")
    Page<Product> findByStoreAndPriceRange(@Param("storeId") UUID storeId,
                                           @Param("minPrice") BigDecimal minPrice, 
                                           @Param("maxPrice") BigDecimal maxPrice, 
                                           Pageable pageable);

    // Products with discount
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.compareAtPrice IS NOT NULL AND p.compareAtPrice > p.price")
    Page<Product> findDiscountedProducts(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.isActive = true AND p.compareAtPrice IS NOT NULL AND p.compareAtPrice > p.price")
    Page<Product> findDiscountedProductsByStore(@Param("storeId") UUID storeId, Pageable pageable);

    // Products by rating
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.reviews r " +
           "WHERE p.isActive = true AND r.isPublished = true " +
           "GROUP BY p.id " +
           "HAVING AVG(CAST(r.rating AS double)) >= :minRating " +
           "ORDER BY AVG(CAST(r.rating AS double)) DESC")
    Page<Product> findByMinRating(@Param("minRating") BigDecimal minRating, Pageable pageable);

    // Popular products (by review count)
    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.reviews r " +
           "WHERE p.isActive = true AND r.isPublished = true " +
           "GROUP BY p.id " +
           "ORDER BY COUNT(r.id) DESC")
    Page<Product> findPopularProducts(Pageable pageable);

    // Recently added products
    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Product> findRecentProducts(@Param("since") ZonedDateTime since, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.isActive = true AND p.createdAt >= :since ORDER BY p.createdAt DESC")
    List<Product> findRecentProductsByStore(@Param("storeId") UUID storeId, @Param("since") ZonedDateTime since, Pageable pageable);

    // Related products (same category, excluding current product)
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.id != :excludeId AND p.isActive = true ORDER BY p.createdAt DESC")
    List<Product> findRelatedProducts(@Param("categoryId") UUID categoryId, 
                                      @Param("excludeId") UUID excludeId, 
                                      Pageable pageable);

    // Products by multiple categories
    @Query("SELECT DISTINCT p FROM Product p WHERE p.category.id IN :categoryIds AND p.isActive = true")
    Page<Product> findByCategoryIdIn(@Param("categoryIds") List<UUID> categoryIds, Pageable pageable);

    // Digital products
    Page<Product> findByIsDigitalTrueAndIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.isDigital = true AND p.isActive = true")
    Page<Product> findDigitalProductsByStore(@Param("storeId") UUID storeId, Pageable pageable);

    // Analytics and reporting
    @Query("SELECT COUNT(p) FROM Product p WHERE p.store.id = :storeId AND p.isActive = true AND p.createdAt >= :since")
    long countNewProductsByStore(@Param("storeId") UUID storeId, @Param("since") ZonedDateTime since);

    @Query("SELECT AVG(p.price) FROM Product p WHERE p.store.id = :storeId AND p.isActive = true")
    BigDecimal getAveragePriceByStore(@Param("storeId") UUID storeId);

    @Query("SELECT MIN(p.price) FROM Product p WHERE p.store.id = :storeId AND p.isActive = true")
    BigDecimal getMinPriceByStore(@Param("storeId") UUID storeId);

    @Query("SELECT MAX(p.price) FROM Product p WHERE p.store.id = :storeId AND p.isActive = true")
    BigDecimal getMaxPriceByStore(@Param("storeId") UUID storeId);

    @Query("SELECT p FROM Product p " +
           "LEFT JOIN p.reviews r " +
           "WHERE p.isActive = true AND r.isPublished = true " +
           "GROUP BY p.id " +
           "ORDER BY AVG(CAST(r.rating AS double)) DESC")
    Page<Product> findTopRatedProducts(Pageable pageable);

    // Store management queries
    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId ORDER BY p.createdAt DESC")
    Page<Product> findAllByStoreId(@Param("storeId") UUID storeId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.isActive = :active")
    Page<Product> findByStoreIdAndIsActive(@Param("storeId") UUID storeId, 
                                           @Param("active") boolean active, 
                                           Pageable pageable);

    // Existence checks
    boolean existsBySlugAndStoreId(String slug, UUID storeId);

    boolean existsBySkuAndStoreId(String sku, UUID storeId);

    // Bulk operations support
    @Query("SELECT p.id FROM Product p WHERE p.store.id = :storeId AND p.isActive = true")
    List<UUID> findActiveProductIdsByStore(@Param("storeId") UUID storeId);

    @Query("SELECT p.id FROM Product p WHERE p.category.id = :categoryId AND p.isActive = true")
    List<UUID> findActiveProductIdsByCategory(@Param("categoryId") UUID categoryId);
}