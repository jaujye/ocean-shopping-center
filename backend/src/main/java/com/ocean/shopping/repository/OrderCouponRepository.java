package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Coupon;
import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.OrderCoupon;
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
public interface OrderCouponRepository extends JpaRepository<OrderCoupon, Long> {

    /**
     * Find all applied coupons for an order
     */
    List<OrderCoupon> findByOrderOrderByCreatedAtDesc(Order order);

    /**
     * Find applied coupons by order ID
     */
    List<OrderCoupon> findByOrder_IdOrderByCreatedAtDesc(Long orderId);

    /**
     * Find all orders where a specific coupon was used
     */
    Page<OrderCoupon> findByCoupon(Coupon coupon, Pageable pageable);

    /**
     * Find specific order-coupon combination
     */
    Optional<OrderCoupon> findByOrderAndCoupon(Order order, Coupon coupon);

    /**
     * Check if coupon was used in a specific order
     */
    boolean existsByOrderAndCoupon(Order order, Coupon coupon);

    /**
     * Count total uses of a coupon
     */
    long countByCoupon(Coupon coupon);

    /**
     * Count uses of a coupon by a specific customer email
     */
    long countByCouponAndOrder_CustomerEmail(Coupon coupon, String customerEmail);

    /**
     * Find order coupons by coupon code (for reporting)
     */
    Page<OrderCoupon> findByCouponCode(String couponCode, Pageable pageable);

    /**
     * Get total discount amount for an order
     */
    @Query("SELECT COALESCE(SUM(oc.discountAmount), 0) FROM OrderCoupon oc WHERE oc.order = :order")
    java.math.BigDecimal getTotalDiscountByOrder(@Param("order") Order order);

    /**
     * Get total discount amount across all orders
     */
    @Query("SELECT COALESCE(SUM(oc.discountAmount), 0) FROM OrderCoupon oc")
    java.math.BigDecimal getTotalDiscountAmount();

    /**
     * Find order coupons within date range
     */
    @Query("SELECT oc FROM OrderCoupon oc WHERE oc.createdAt >= :startDate AND oc.createdAt <= :endDate ORDER BY oc.createdAt DESC")
    Page<OrderCoupon> findOrderCouponsInDateRange(@Param("startDate") ZonedDateTime startDate, 
                                                 @Param("endDate") ZonedDateTime endDate, 
                                                 Pageable pageable);

    /**
     * Find most used coupons
     */
    @Query("SELECT oc.coupon, COUNT(oc) as usageCount FROM OrderCoupon oc GROUP BY oc.coupon ORDER BY usageCount DESC")
    Page<Object[]> findMostUsedCoupons(Pageable pageable);

    /**
     * Find coupons with highest total discount amount
     */
    @Query("SELECT oc.coupon, SUM(oc.discountAmount) as totalDiscount FROM OrderCoupon oc GROUP BY oc.coupon ORDER BY totalDiscount DESC")
    Page<Object[]> findCouponsWithHighestDiscount(Pageable pageable);

    /**
     * Get coupon usage statistics for a date range
     */
    @Query("SELECT oc.couponCode, COUNT(oc) as usageCount, SUM(oc.discountAmount) as totalDiscount, AVG(oc.discountAmount) as avgDiscount FROM OrderCoupon oc WHERE oc.createdAt >= :startDate AND oc.createdAt <= :endDate GROUP BY oc.couponCode ORDER BY usageCount DESC")
    List<Object[]> getCouponUsageStats(@Param("startDate") ZonedDateTime startDate, 
                                      @Param("endDate") ZonedDateTime endDate);

    /**
     * Find order coupons for a specific customer
     */
    @Query("SELECT oc FROM OrderCoupon oc WHERE oc.order.customerEmail = :customerEmail ORDER BY oc.createdAt DESC")
    Page<OrderCoupon> findByCustomerEmail(@Param("customerEmail") String customerEmail, Pageable pageable);

    /**
     * Check if customer has used a coupon before (for first-time customer validation)
     */
    @Query("SELECT COUNT(oc) > 0 FROM OrderCoupon oc WHERE oc.order.customerEmail = :customerEmail")
    boolean hasCustomerUsedAnyCoupon(@Param("customerEmail") String customerEmail);

    /**
     * Find recent coupon usage
     */
    @Query("SELECT oc FROM OrderCoupon oc WHERE oc.createdAt >= :since ORDER BY oc.createdAt DESC")
    List<OrderCoupon> findRecentUsage(@Param("since") ZonedDateTime since);

    /**
     * Get coupon performance metrics
     */
    @Query("SELECT " +
           "COUNT(oc) as totalUsage, " +
           "COUNT(DISTINCT oc.order.customerEmail) as uniqueCustomers, " +
           "SUM(oc.discountAmount) as totalDiscount, " +
           "AVG(oc.discountAmount) as avgDiscount, " +
           "MAX(oc.discountAmount) as maxDiscount " +
           "FROM OrderCoupon oc WHERE oc.coupon = :coupon")
    Object[] getCouponMetrics(@Param("coupon") Coupon coupon);

    /**
     * Find orders with multiple coupons applied (edge case handling)
     */
    @Query("SELECT oc.order FROM OrderCoupon oc GROUP BY oc.order HAVING COUNT(oc) > 1")
    List<Order> findOrdersWithMultipleCoupons();

    /**
     * Get monthly coupon usage statistics
     */
    @Query("SELECT " +
           "EXTRACT(YEAR FROM oc.createdAt) as year, " +
           "EXTRACT(MONTH FROM oc.createdAt) as month, " +
           "COUNT(oc) as usageCount, " +
           "SUM(oc.discountAmount) as totalDiscount " +
           "FROM OrderCoupon oc " +
           "WHERE oc.createdAt >= :startDate AND oc.createdAt <= :endDate " +
           "GROUP BY EXTRACT(YEAR FROM oc.createdAt), EXTRACT(MONTH FROM oc.createdAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> getMonthlyCouponStats(@Param("startDate") ZonedDateTime startDate, 
                                        @Param("endDate") ZonedDateTime endDate);
}