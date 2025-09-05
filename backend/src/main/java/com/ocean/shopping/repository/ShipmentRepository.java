package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.Shipment;
import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Shipment entity
 */
@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    /**
     * Find shipment by tracking number
     */
    Optional<Shipment> findByTrackingNumber(String trackingNumber);

    /**
     * Find all shipments for an order
     */
    List<Shipment> findByOrder(Order order);

    /**
     * Find shipments by order ID
     */
    List<Shipment> findByOrderId(Long orderId);

    /**
     * Find shipments by carrier
     */
    Page<Shipment> findByCarrier(CarrierType carrier, Pageable pageable);

    /**
     * Find shipments by status
     */
    Page<Shipment> findByStatus(ShipmentStatus status, Pageable pageable);

    /**
     * Find shipments by carrier and status
     */
    Page<Shipment> findByCarrierAndStatus(CarrierType carrier, ShipmentStatus status, Pageable pageable);

    /**
     * Find shipments shipped on a specific date
     */
    List<Shipment> findByShipDate(LocalDate shipDate);

    /**
     * Find shipments shipped between dates
     */
    @Query("SELECT s FROM Shipment s WHERE s.shipDate BETWEEN :startDate AND :endDate")
    List<Shipment> findByShipDateBetween(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate);

    /**
     * Find shipments that need tracking updates
     */
    @Query("SELECT s FROM Shipment s WHERE s.status IN :activeStatuses " +
           "AND (s.lastTrackingUpdate IS NULL OR s.lastTrackingUpdate < :cutoffTime)")
    List<Shipment> findShipmentsNeedingUpdate(@Param("activeStatuses") List<ShipmentStatus> activeStatuses,
                                             @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find shipments by customer email
     */
    @Query("SELECT s FROM Shipment s WHERE s.recipientEmail = :email OR s.order.customerEmail = :email")
    Page<Shipment> findByCustomerEmail(@Param("email") String email, Pageable pageable);

    /**
     * Find overdue shipments (past estimated delivery date)
     */
    @Query("SELECT s FROM Shipment s WHERE s.estimatedDeliveryDate < :currentDate " +
           "AND s.status NOT IN :terminalStatuses")
    List<Shipment> findOverdueShipments(@Param("currentDate") LocalDate currentDate,
                                       @Param("terminalStatuses") List<ShipmentStatus> terminalStatuses);

    /**
     * Find shipments with exceptions
     */
    @Query("SELECT s FROM Shipment s WHERE s.status IN :exceptionStatuses")
    Page<Shipment> findShipmentsWithExceptions(@Param("exceptionStatuses") List<ShipmentStatus> exceptionStatuses, 
                                               Pageable pageable);

    /**
     * Count shipments by status
     */
    @Query("SELECT s.status, COUNT(s) FROM Shipment s GROUP BY s.status")
    List<Object[]> countByStatus();

    /**
     * Count shipments by carrier
     */
    @Query("SELECT s.carrier, COUNT(s) FROM Shipment s GROUP BY s.carrier")
    List<Object[]> countByCarrier();

    /**
     * Find shipments that require signature
     */
    @Query("SELECT s FROM Shipment s WHERE s.signatureRequired = true OR s.adultSignatureRequired = true")
    List<Shipment> findShipmentsRequiringSignature();

    /**
     * Check if tracking number exists
     */
    boolean existsByTrackingNumber(String trackingNumber);

    /**
     * Find shipments by multiple tracking numbers (for bulk operations)
     */
    @Query("SELECT s FROM Shipment s WHERE s.trackingNumber IN :trackingNumbers")
    List<Shipment> findByTrackingNumbers(@Param("trackingNumbers") List<String> trackingNumbers);

    /**
     * Find recent shipments for a customer
     */
    @Query("SELECT s FROM Shipment s WHERE (s.recipientEmail = :email OR s.order.customerEmail = :email) " +
           "AND s.createdAt >= :sinceDate ORDER BY s.createdAt DESC")
    List<Shipment> findRecentShipmentsByCustomer(@Param("email") String email, 
                                                @Param("sinceDate") LocalDateTime sinceDate);

    /**
     * Find shipments by reference number
     */
    List<Shipment> findByReferenceOrCustomerReference(String reference, String customerReference);
}