package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Coupon;
import com.ocean.shopping.model.entity.Store;
import com.ocean.shopping.model.entity.enums.CouponStatus;
import com.ocean.shopping.model.entity.enums.CouponType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    /**
     * Find coupon by code (case-insensitive)
     */
    Optional<Coupon> findByCodeIgnoreCase(String code);

    /**
     * Find coupon by exact code
     */
    Optional<Coupon> findByCode(String code);

    /**
     * Check if coupon code exists
     */
    boolean existsByCodeIgnoreCase(String code);

    /**
     * Find coupons by status
     */
    Page<Coupon> findByStatus(CouponStatus status, Pageable pageable);

    /**
     * Find coupons by type
     */
    Page<Coupon> findByType(CouponType type, Pageable pageable);

    /**
     * Find coupons by store
     */
    Page<Coupon> findByStore(Store store, Pageable pageable);

    /**
     * Find global coupons (store is null)
     */
    Page<Coupon> findByStoreIsNull(Pageable pageable);

    /**
     * Find active coupons that can be used now
     */
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validUntil >= :now AND (c.usageLimit IS NULL OR c.timesUsed < c.usageLimit)")
    List<Coupon> findActiveCoupons(@Param("now") ZonedDateTime now);

    /**
     * Find active coupons for a specific store or global coupons
     */
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validUntil >= :now AND (c.usageLimit IS NULL OR c.timesUsed < c.usageLimit) AND (c.store = :store OR c.store IS NULL)")
    List<Coupon> findActiveCouponsForStore(@Param("store") Store store, @Param("now") ZonedDateTime now);

    /**
     * Find coupon by code and validate it's usable
     */
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code) AND c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validUntil >= :now AND (c.usageLimit IS NULL OR c.timesUsed < c.usageLimit)")
    Optional<Coupon> findUsableCouponByCode(@Param("code") String code, @Param("now") ZonedDateTime now);

    /**
     * Find usable coupon by code for specific store or global
     */
    @Query("SELECT c FROM Coupon c WHERE UPPER(c.code) = UPPER(:code) AND c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validUntil >= :now AND (c.usageLimit IS NULL OR c.timesUsed < c.usageLimit) AND (c.store = :store OR c.store IS NULL)")
    Optional<Coupon> findUsableCouponByCodeForStore(@Param("code") String code, @Param("store") Store store, @Param("now") ZonedDateTime now);

    /**
     * Find expired coupons that need status update
     */
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' AND c.validUntil < :now")
    List<Coupon> findExpiredActiveCoupons(@Param("now") ZonedDateTime now);

    /**
     * Find coupons that reached usage limit
     */
    @Query("SELECT c FROM Coupon c WHERE c.status = 'ACTIVE' AND c.usageLimit IS NOT NULL AND c.timesUsed >= c.usageLimit")
    List<Coupon> findUsedUpActiveCoupons();

    /**
     * Find coupons within date range
     */
    @Query("SELECT c FROM Coupon c WHERE c.validFrom >= :startDate AND c.validUntil <= :endDate ORDER BY c.validFrom DESC")
    Page<Coupon> findCouponsInDateRange(@Param("startDate") ZonedDateTime startDate, 
                                       @Param("endDate") ZonedDateTime endDate, 
                                       Pageable pageable);

    /**
     * Count coupons by status
     */
    long countByStatus(CouponStatus status);

    /**
     * Count coupons by store
     */
    long countByStore(Store store);

    /**
     * Count global coupons
     */
    long countByStoreIsNull();

    /**
     * Find coupons by name containing (case-insensitive search)
     */
    Page<Coupon> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Find coupons by store and status
     */
    Page<Coupon> findByStoreAndStatus(Store store, CouponStatus status, Pageable pageable);

    /**
     * Find global coupons by status
     */
    Page<Coupon> findByStoreIsNullAndStatus(CouponStatus status, Pageable pageable);

    /**
     * Count usage of a specific coupon by user email (for per-customer limit validation)
     */
    @Query("SELECT COUNT(oc) FROM OrderCoupon oc WHERE oc.coupon = :coupon AND oc.order.customerEmail = :customerEmail")
    long countUsageByCustomer(@Param("coupon") Coupon coupon, @Param("customerEmail") String customerEmail);

    /**
     * Find coupons that apply to first-time customers only
     */
    @Query("SELECT c FROM Coupon c WHERE c.firstTimeCustomerOnly = true AND c.status = 'ACTIVE' AND c.validFrom <= :now AND c.validUntil >= :now AND (c.usageLimit IS NULL OR c.timesUsed < c.usageLimit)")
    List<Coupon> findFirstTimeCustomerCoupons(@Param("now") ZonedDateTime now);


    /**
     * Get total discount provided by all coupons
     */
    @Query("SELECT COALESCE(SUM(oc.discountAmount), 0) FROM OrderCoupon oc WHERE oc.coupon IN :coupons")
    java.math.BigDecimal getTotalDiscountByCoupons(@Param("coupons") List<Coupon> coupons);
}