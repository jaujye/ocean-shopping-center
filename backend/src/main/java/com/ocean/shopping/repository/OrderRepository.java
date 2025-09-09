package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.Store;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Order entity with comprehensive query support
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Basic finders
    Optional<Order> findByOrderNumber(String orderNumber);
    
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    
    Page<Order> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    
    List<Order> findByStoreOrderByCreatedAtDesc(Store store);
    
    Page<Order> findByStoreOrderByCreatedAtDesc(Store store, Pageable pageable);

    // Status-based queries
    List<Order> findByStatus(OrderStatus status);
    
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    
    List<Order> findByStatusIn(List<OrderStatus> statuses);
    
    Page<Order> findByStatusInOrderByCreatedAtDesc(List<OrderStatus> statuses, Pageable pageable);

    // User-specific status queries
    List<Order> findByUserAndStatus(User user, OrderStatus status);
    
    Page<Order> findByUserAndStatusOrderByCreatedAtDesc(User user, OrderStatus status, Pageable pageable);
    
    List<Order> findByUserAndStatusIn(User user, List<OrderStatus> statuses);

    // Store-specific status queries
    List<Order> findByStoreAndStatus(Store store, OrderStatus status);
    
    Page<Order> findByStoreAndStatusOrderByCreatedAtDesc(Store store, OrderStatus status, Pageable pageable);
    
    List<Order> findByStoreAndStatusIn(Store store, List<OrderStatus> statuses);

    // Email-based queries
    List<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    Page<Order> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail, Pageable pageable);

    // Date range queries
    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    List<Order> findByDateRange(@Param("startDate") ZonedDateTime startDate, 
                               @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByDateRange(@Param("startDate") ZonedDateTime startDate, 
                               @Param("endDate") ZonedDateTime endDate, 
                               Pageable pageable);

    // Store date range queries
    @Query("SELECT o FROM Order o WHERE o.store = :store AND o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    List<Order> findByStoreAndDateRange(@Param("store") Store store,
                                       @Param("startDate") ZonedDateTime startDate, 
                                       @Param("endDate") ZonedDateTime endDate);

    @Query("SELECT o FROM Order o WHERE o.store = :store AND o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByStoreAndDateRange(@Param("store") Store store,
                                       @Param("startDate") ZonedDateTime startDate, 
                                       @Param("endDate") ZonedDateTime endDate,
                                       Pageable pageable);

    // Count queries
    long countByUser(User user);
    
    long countByUserAndStatus(User user, OrderStatus status);
    
    long countByStore(Store store);
    
    long countByStoreAndStatus(Store store, OrderStatus status);
    
    long countByStatus(OrderStatus status);

    // Amount queries
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user = :user AND o.status IN :statuses")
    BigDecimal sumTotalAmountByUserAndStatuses(@Param("user") User user, 
                                              @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.store = :store AND o.status IN :statuses")
    BigDecimal sumTotalAmountByStoreAndStatuses(@Param("store") Store store, 
                                               @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.store = :store AND o.createdAt >= :startDate AND o.createdAt <= :endDate AND o.status IN :statuses")
    BigDecimal sumTotalAmountByStoreAndDateRangeAndStatuses(@Param("store") Store store,
                                                           @Param("startDate") ZonedDateTime startDate,
                                                           @Param("endDate") ZonedDateTime endDate,
                                                           @Param("statuses") List<OrderStatus> statuses);

    // Analytics queries
    @Query("SELECT COUNT(o) FROM Order o WHERE o.store = :store AND o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countByStoreAndDateRange(@Param("store") Store store,
                                 @Param("startDate") ZonedDateTime startDate,
                                 @Param("endDate") ZonedDateTime endDate);

    // Recent orders for dashboard
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE o.store = :store ORDER BY o.createdAt DESC")
    List<Order> findRecentOrdersByStore(@Param("store") Store store, Pageable pageable);

    // Orders requiring attention (pending, processing)
    @Query("SELECT o FROM Order o WHERE o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING') ORDER BY o.createdAt ASC")
    List<Order> findOrdersRequiringAttention();
    
    @Query("SELECT o FROM Order o WHERE o.store = :store AND o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING') ORDER BY o.createdAt ASC")
    List<Order> findOrdersRequiringAttentionByStore(@Param("store") Store store);

    // Search functionality
    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.billingFirstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.billingLastName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrders(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT o FROM Order o WHERE " +
           "o.store = :store AND (" +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.customerEmail) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.billingFirstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(o.billingLastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> searchOrdersByStore(@Param("store") Store store, @Param("searchTerm") String searchTerm, Pageable pageable);

    // Order status transition queries
    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'PENDING' AND o.createdAt < :cutoffTime")
    long countStaleOrders(@Param("cutoffTime") ZonedDateTime cutoffTime);
    
    @Query("SELECT o FROM Order o WHERE o.status = 'CONFIRMED' AND o.confirmedAt < :cutoffTime")
    List<Order> findOrdersReadyForProcessing(@Param("cutoffTime") ZonedDateTime cutoffTime);

    // Customer insights
    @Query("SELECT COUNT(DISTINCT o.customerEmail) FROM Order o WHERE o.store = :store")
    long countUniqueCustomersByStore(@Param("store") Store store);
    
    @Query("SELECT o.customerEmail, COUNT(o) as orderCount FROM Order o WHERE o.store = :store GROUP BY o.customerEmail ORDER BY orderCount DESC")
    List<Object[]> findTopCustomersByOrderCount(@Param("store") Store store, Pageable pageable);

    // Revenue analytics
    @Query("SELECT DATE(o.createdAt), SUM(o.totalAmount) FROM Order o WHERE " +
           "o.store = :store AND o.status IN :statuses AND " +
           "o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt)")
    List<Object[]> getDailyRevenue(@Param("store") Store store,
                                  @Param("statuses") List<OrderStatus> statuses,
                                  @Param("startDate") ZonedDateTime startDate,
                                  @Param("endDate") ZonedDateTime endDate);

    // Missing methods required by OrderManagementService
    
    // Status and date range combined queries
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByStatusAndDateRange(@Param("status") OrderStatus status,
                                        @Param("startDate") ZonedDateTime startDate,
                                        @Param("endDate") ZonedDateTime endDate,
                                        Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.store = :store AND o.status = :status AND o.createdAt >= :startDate AND o.createdAt <= :endDate ORDER BY o.createdAt DESC")
    Page<Order> findByStoreAndStatusAndDateRange(@Param("store") Store store,
                                                @Param("status") OrderStatus status,
                                                @Param("startDate") ZonedDateTime startDate,
                                                @Param("endDate") ZonedDateTime endDate,
                                                Pageable pageable);

    // Global analytics methods
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate AND o.status IN :statuses")
    BigDecimal sumTotalAmountByDateRangeAndStatuses(@Param("startDate") ZonedDateTime startDate,
                                                   @Param("endDate") ZonedDateTime endDate,
                                                   @Param("statuses") List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate AND o.status IN :statuses")
    long countByDateRangeAndStatuses(@Param("startDate") ZonedDateTime startDate,
                                    @Param("endDate") ZonedDateTime endDate,
                                    @Param("statuses") List<OrderStatus> statuses);

    // Top stores by revenue for admin analytics
    @Query("SELECT o.store.id, o.store.name, SUM(o.totalAmount) as revenue, COUNT(o) as orderCount FROM Order o " +
           "WHERE o.status IN :statuses AND o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "GROUP BY o.store.id, o.store.name ORDER BY revenue DESC")
    List<Object[]> getTopStoresByRevenue(@Param("statuses") List<OrderStatus> statuses,
                                        @Param("startDate") ZonedDateTime startDate,
                                        @Param("endDate") ZonedDateTime endDate);
}