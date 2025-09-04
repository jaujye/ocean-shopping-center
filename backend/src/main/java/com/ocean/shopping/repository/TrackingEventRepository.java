package com.ocean.shopping.repository;

import com.ocean.shopping.model.entity.Shipment;
import com.ocean.shopping.model.entity.TrackingEvent;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TrackingEvent entity
 */
@Repository
public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {

    /**
     * Find all tracking events for a shipment, ordered by event time (most recent first)
     */
    List<TrackingEvent> findByShipmentOrderByEventTimeDesc(Shipment shipment);

    /**
     * Find tracking events by shipment ID, ordered by event time
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.shipment.id = :shipmentId ORDER BY te.eventTime DESC")
    List<TrackingEvent> findByShipmentIdOrderByEventTimeDesc(@Param("shipmentId") Long shipmentId);

    /**
     * Find the most recent tracking event for a shipment
     */
    Optional<TrackingEvent> findFirstByShipmentOrderByEventTimeDesc(Shipment shipment);

    /**
     * Find tracking events by tracking number
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.shipment.trackingNumber = :trackingNumber ORDER BY te.eventTime DESC")
    List<TrackingEvent> findByTrackingNumber(@Param("trackingNumber") String trackingNumber);

    /**
     * Find tracking events by status
     */
    List<TrackingEvent> findByStatus(ShipmentStatus status);

    /**
     * Find delivery events
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.status = 'DELIVERED' OR te.isFinalDelivery = true")
    List<TrackingEvent> findDeliveryEvents();

    /**
     * Find exception events
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.isException = true OR te.status IN :exceptionStatuses")
    List<TrackingEvent> findExceptionEvents(@Param("exceptionStatuses") List<ShipmentStatus> exceptionStatuses);

    /**
     * Find events that occurred between dates
     */
    List<TrackingEvent> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find events by event type
     */
    List<TrackingEvent> findByEventType(String eventType);

    /**
     * Find unprocessed events that need notifications
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.notificationSent = false " +
           "AND te.status IN :notifiableStatuses ORDER BY te.eventTime ASC")
    List<TrackingEvent> findUnprocessedNotifications(@Param("notifiableStatuses") List<ShipmentStatus> notifiableStatuses);

    /**
     * Find events by city and country
     */
    List<TrackingEvent> findByCityAndCountry(String city, String country);

    /**
     * Check if event already exists (to avoid duplicates)
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.shipment = :shipment AND te.eventId = :eventId")
    Optional<TrackingEvent> findByShipmentAndEventId(@Param("shipment") Shipment shipment, 
                                                     @Param("eventId") String eventId);

    /**
     * Find events that occurred today
     */
    @Query("SELECT te FROM TrackingEvent te WHERE DATE(te.eventTime) = CURRENT_DATE ORDER BY te.eventTime DESC")
    List<TrackingEvent> findTodaysEvents();

    /**
     * Count events by status
     */
    @Query("SELECT te.status, COUNT(te) FROM TrackingEvent te GROUP BY te.status")
    List<Object[]> countByStatus();

    /**
     * Count events by event type
     */
    @Query("SELECT te.eventType, COUNT(te) FROM TrackingEvent te WHERE te.eventType IS NOT NULL GROUP BY te.eventType")
    List<Object[]> countByEventType();

    /**
     * Find recent events for a customer's shipments
     */
    @Query("SELECT te FROM TrackingEvent te WHERE " +
           "(te.shipment.recipientEmail = :customerEmail OR te.shipment.order.customerEmail = :customerEmail) " +
           "AND te.eventTime >= :sinceTime ORDER BY te.eventTime DESC")
    List<TrackingEvent> findRecentEventsByCustomer(@Param("customerEmail") String customerEmail,
                                                  @Param("sinceTime") LocalDateTime sinceTime);

    /**
     * Find events that were processed recently
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.processedAt >= :sinceTime ORDER BY te.processedAt DESC")
    List<TrackingEvent> findRecentlyProcessedEvents(@Param("sinceTime") LocalDateTime sinceTime);

    /**
     * Get event statistics for a date range
     */
    @Query("SELECT DATE(te.eventTime) as eventDate, te.status, COUNT(te) as eventCount " +
           "FROM TrackingEvent te WHERE te.eventTime BETWEEN :startDate AND :endDate " +
           "GROUP BY DATE(te.eventTime), te.status ORDER BY eventDate DESC")
    List<Object[]> getEventStatistics(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    /**
     * Find events for multiple shipments
     */
    @Query("SELECT te FROM TrackingEvent te WHERE te.shipment IN :shipments ORDER BY te.eventTime DESC")
    List<TrackingEvent> findByShipmentsOrderByEventTimeDesc(@Param("shipments") List<Shipment> shipments);

    /**
     * Delete old tracking events (for cleanup)
     */
    @Query("DELETE FROM TrackingEvent te WHERE te.eventTime < :cutoffDate")
    void deleteOldEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
}