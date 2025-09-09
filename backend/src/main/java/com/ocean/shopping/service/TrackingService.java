package com.ocean.shopping.service;

import com.ocean.shopping.dto.shipping.TrackingEventDto;
import com.ocean.shopping.dto.shipping.TrackingResponse;
import com.ocean.shopping.model.entity.Shipment;
import com.ocean.shopping.model.entity.TrackingEvent;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import com.ocean.shopping.repository.ShipmentRepository;
import com.ocean.shopping.repository.TrackingEventRepository;
import com.ocean.shopping.service.logistics.CarrierException;
import com.ocean.shopping.service.logistics.CarrierManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Service for tracking shipments and managing tracking events
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TrackingService {

    private final CarrierManager carrierManager;
    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;

    /**
     * Get tracking information for a shipment
     */
    @Transactional(readOnly = true)
    public TrackingResponse getTrackingInfo(String trackingNumber) throws CarrierException {
        log.info("Getting tracking info for: {}", trackingNumber);

        // First, try to get from database
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
        if (shipmentOpt.isPresent()) {
            Shipment shipment = shipmentOpt.get();
            return buildTrackingResponseFromDatabase(shipment);
        }

        // If not in database, get from carrier
        return carrierManager.trackShipment(trackingNumber);
    }

    /**
     * Get tracking history for a shipment
     */
    @Transactional(readOnly = true)
    public List<TrackingEventDto> getTrackingHistory(String trackingNumber) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
        if (shipmentOpt.isEmpty()) {
            return List.of();
        }

        List<TrackingEvent> events = trackingEventRepository
                .findByShipmentOrderByEventTimeDesc(shipmentOpt.get());

        return events.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get latest tracking event for a shipment
     */
    @Transactional(readOnly = true)
    public Optional<TrackingEventDto> getLatestTrackingEvent(String trackingNumber) {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
        if (shipmentOpt.isEmpty()) {
            return Optional.empty();
        }

        Optional<TrackingEvent> latestEvent = trackingEventRepository
                .findFirstByShipmentOrderByEventTimeDesc(shipmentOpt.get());

        return latestEvent.map(this::convertToDto);
    }

    /**
     * Update tracking information for a single shipment
     */
    public CompletableFuture<Boolean> updateTrackingInfo(String trackingNumber) {
        return updateTrackingInfoAsync(trackingNumber);
    }

    /**
     * Update tracking information asynchronously
     */
    @Async
    @Transactional
    public CompletableFuture<Boolean> updateTrackingInfoAsync(String trackingNumber) {
        try {
            log.debug("Updating tracking info for: {}", trackingNumber);

            Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipmentOpt.isEmpty()) {
                log.warn("Shipment not found for tracking number: {}", trackingNumber);
                return CompletableFuture.completedFuture(false);
            }

            Shipment shipment = shipmentOpt.get();

            // Skip if already in terminal status and recently updated
            if (shipment.getStatus().isTerminal() && 
                shipment.getLastTrackingUpdate() != null && 
                shipment.getLastTrackingUpdate().isAfter(LocalDateTime.now().minusDays(1))) {
                log.debug("Skipping update for terminal shipment: {}", trackingNumber);
                return CompletableFuture.completedFuture(true);
            }

            // Get latest tracking from carrier
            TrackingResponse trackingResponse = carrierManager.trackShipment(trackingNumber, shipment.getCarrier());
            
            // Update shipment and events
            updateShipmentFromTracking(shipment, trackingResponse);
            
            log.debug("Successfully updated tracking info for: {}", trackingNumber);
            return CompletableFuture.completedFuture(true);

        } catch (CarrierException e) {
            log.error("Failed to update tracking for {}: {}", trackingNumber, e.getMessage());
            return CompletableFuture.completedFuture(false);
        } catch (Exception e) {
            log.error("Unexpected error updating tracking for {}: {}", trackingNumber, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Scheduled task to update all active shipments
     */
    @Scheduled(fixedDelayString = "${tracking.update.interval:3600000}") // Default: 1 hour
    @Transactional
    public void updateActiveShipments() {
        log.info("Starting scheduled tracking update for active shipments");

        List<ShipmentStatus> activeStatuses = List.of(
            ShipmentStatus.PENDING,
            ShipmentStatus.PICKED_UP,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELAYED,
            ShipmentStatus.EXCEPTION
        );

        // Find shipments that haven't been updated in the last 2 hours
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);
        List<Shipment> shipmentsToUpdate = shipmentRepository
                .findShipmentsNeedingUpdate(activeStatuses, cutoffTime);

        log.info("Found {} shipments needing tracking updates", shipmentsToUpdate.size());

        if (shipmentsToUpdate.isEmpty()) {
            return;
        }

        // Update in batches to avoid overwhelming carriers
        List<String> trackingNumbers = shipmentsToUpdate.stream()
                .map(Shipment::getTrackingNumber)
                .collect(Collectors.toList());

        // Process in batches of 10
        int batchSize = 10;
        for (int i = 0; i < trackingNumbers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, trackingNumbers.size());
            List<String> batch = trackingNumbers.subList(i, endIndex);
            
            log.debug("Processing tracking update batch: {} to {}", i, endIndex - 1);
            
            // Update batch asynchronously
            batch.forEach(this::updateTrackingInfoAsync);
            
            // Small delay between batches to be respectful to carrier APIs
            try {
                Thread.sleep(1000); // 1 second delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Tracking update interrupted");
                break;
            }
        }

        log.info("Completed scheduled tracking update");
    }

    /**
     * Get tracking events that need notifications
     */
    @Transactional(readOnly = true)
    public List<TrackingEvent> getEventsNeedingNotification() {
        List<ShipmentStatus> notifiableStatuses = List.of(
            ShipmentStatus.PICKED_UP,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELIVERED,
            ShipmentStatus.DELAYED,
            ShipmentStatus.EXCEPTION,
            ShipmentStatus.RETURNED
        );

        return trackingEventRepository.findUnprocessedNotifications(notifiableStatuses);
    }

    /**
     * Mark tracking event as notification sent
     */
    @Transactional
    public void markNotificationSent(Long trackingEventId) {
        Optional<TrackingEvent> eventOpt = trackingEventRepository.findById(trackingEventId);
        if (eventOpt.isPresent()) {
            TrackingEvent event = eventOpt.get();
            event.setNotificationSent(true);
            event.setNotificationSentAt(LocalDateTime.now());
            trackingEventRepository.save(event);
        }
    }

    /**
     * Get tracking statistics
     */
    @Transactional(readOnly = true)
    public TrackingStatistics getTrackingStatistics() {
        List<Object[]> statusCounts = trackingEventRepository.countByStatus();
        List<Object[]> eventTypeCounts = trackingEventRepository.countByEventType();
        
        return TrackingStatistics.builder()
                .statusCounts(statusCounts.stream()
                        .collect(Collectors.toMap(
                            row -> (ShipmentStatus) row[0],
                            row -> ((Number) row[1]).longValue()
                        )))
                .eventTypeCounts(eventTypeCounts.stream()
                        .collect(Collectors.toMap(
                            row -> (String) row[0],
                            row -> ((Number) row[1]).longValue()
                        )))
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    /**
     * Clean up old tracking events
     */
    @Scheduled(cron = "${tracking.cleanup.cron:0 0 2 * * *}") // Default: 2 AM daily
    @Transactional
    public void cleanupOldTrackingEvents() {
        // Keep tracking events for 2 years
        LocalDateTime cutoffDate = LocalDateTime.now().minusYears(2);
        
        log.info("Cleaning up tracking events older than: {}", cutoffDate);
        trackingEventRepository.deleteOldEvents(cutoffDate);
        log.info("Completed tracking events cleanup");
    }

    /**
     * Build tracking response from database
     */
    private TrackingResponse buildTrackingResponseFromDatabase(Shipment shipment) {
        List<TrackingEvent> events = trackingEventRepository
                .findByShipmentOrderByEventTimeDesc(shipment);

        List<TrackingEventDto> eventDtos = events.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return TrackingResponse.builder()
                .trackingNumber(shipment.getTrackingNumber())
                .carrier(shipment.getCarrier())
                .serviceType(shipment.getServiceType())
                .currentStatus(shipment.getStatus())
                .statusDescription(shipment.getStatusDescription())
                .lastUpdated(shipment.getLastTrackingUpdate())
                .shipDate(shipment.getShipDate())
                .estimatedDeliveryDate(shipment.getEstimatedDeliveryDate())
                .actualDeliveryDate(shipment.getActualDeliveryDate())
                .deliveryTime(shipment.getDeliveryTime())
                .signedBy(shipment.getSignedBy())
                .currentLocation(shipment.getCurrentLocation())
                .trackingHistory(eventDtos)
                .specialInstructions(shipment.getSpecialInstructions())
                .deliveryInstructions(shipment.getDeliveryInstructions())
                .isDelivered(shipment.isDelivered())
                .hasException(shipment.hasException())
                .totalEvents(events.size())
                .trackingUrl(shipment.getTrackingUrl())
                .build();
    }

    /**
     * Update shipment from tracking response
     */
    private void updateShipmentFromTracking(Shipment shipment, TrackingResponse trackingResponse) {
        boolean updated = false;

        // Update status if changed
        if (trackingResponse.getCurrentStatus() != null && 
            !trackingResponse.getCurrentStatus().equals(shipment.getStatus())) {
            shipment.setStatus(trackingResponse.getCurrentStatus());
            shipment.setStatusDescription(trackingResponse.getStatusDescription());
            updated = true;
        }

        // Update location if changed
        if (trackingResponse.getCurrentLocation() != null && 
            !trackingResponse.getCurrentLocation().equals(shipment.getCurrentLocation())) {
            shipment.setCurrentLocation(trackingResponse.getCurrentLocation());
            updated = true;
        }

        // Update delivery information
        if (trackingResponse.getActualDeliveryDate() != null && 
            !trackingResponse.getActualDeliveryDate().equals(shipment.getActualDeliveryDate())) {
            shipment.setActualDeliveryDate(trackingResponse.getActualDeliveryDate());
            updated = true;
        }

        if (trackingResponse.getDeliveryTime() != null && 
            !trackingResponse.getDeliveryTime().equals(shipment.getDeliveryTime())) {
            shipment.setDeliveryTime(trackingResponse.getDeliveryTime());
            updated = true;
        }

        if (trackingResponse.getSignedBy() != null && 
            !trackingResponse.getSignedBy().equals(shipment.getSignedBy())) {
            shipment.setSignedBy(trackingResponse.getSignedBy());
            updated = true;
        }

        // Always update last tracking update time
        shipment.setLastTrackingUpdate(LocalDateTime.now());
        
        if (updated) {
            shipmentRepository.save(shipment);
            log.debug("Updated shipment {} from tracking", shipment.getTrackingNumber());
        }

        // Process new tracking events
        if (trackingResponse.getTrackingHistory() != null) {
            for (TrackingEventDto eventDto : trackingResponse.getTrackingHistory()) {
                // Check if event already exists
                Optional<TrackingEvent> existingEvent = trackingEventRepository
                        .findByShipmentAndEventId(shipment, eventDto.getEventId());

                if (existingEvent.isEmpty()) {
                    // Create new tracking event
                    TrackingEvent trackingEvent = TrackingEvent.builder()
                            .shipment(shipment)
                            .eventId(eventDto.getEventId())
                            .status(eventDto.getStatus())
                            .statusDescription(eventDto.getStatusDescription())
                            .eventDescription(eventDto.getEventDescription())
                            .eventTime(eventDto.getEventTime())
                            .eventType(eventDto.getEventType())
                            .eventCode(eventDto.getEventCode())
                            .location(eventDto.getLocation())
                            .city(eventDto.getCity())
                            .state(eventDto.getState())
                            .country(eventDto.getCountry())
                            .postalCode(eventDto.getPostalCode())
                            .facilityName(eventDto.getFacilityName())
                            .reasonCode(eventDto.getReasonCode())
                            .reasonDescription(eventDto.getReasonDescription())
                            .nextAction(eventDto.getNextAction())
                            .signedBy(eventDto.getSignedBy())
                            .isDeliveryAttempt(eventDto.getIsDeliveryAttempt())
                            .isException(eventDto.getIsException())
                            .isFinalDelivery(eventDto.isTerminalEvent())
                            .processedAt(LocalDateTime.now())
                            .notificationSent(false)
                            .build();

                    trackingEventRepository.save(trackingEvent);
                    log.debug("Created new tracking event for shipment {}: {}", 
                             shipment.getTrackingNumber(), eventDto.getStatus());
                }
            }
        }
    }

    /**
     * Convert tracking event entity to DTO
     */
    private TrackingEventDto convertToDto(TrackingEvent event) {
        return TrackingEventDto.builder()
                .eventId(event.getEventId())
                .status(event.getStatus())
                .statusDescription(event.getStatusDescription())
                .eventDescription(event.getEventDescription())
                .eventTime(event.getEventTime())
                .location(event.getLocation())
                .city(event.getCity())
                .state(event.getState())
                .country(event.getCountry())
                .postalCode(event.getPostalCode())
                .facilityName(event.getFacilityName())
                .eventType(event.getEventType())
                .eventCode(event.getEventCode())
                .reasonCode(event.getReasonCode())
                .reasonDescription(event.getReasonDescription())
                .nextAction(event.getNextAction())
                .isDeliveryAttempt(event.getIsDeliveryAttempt())
                .isException(event.getIsException())
                .signedBy(event.getSignedBy())
                .build();
    }

    /**
     * Update order tracking with a tracking number
     */
    @Transactional
    public void updateOrderTracking(UUID orderId, String trackingNumber) {
        log.info("Updating order {} with tracking number: {}", orderId, trackingNumber);
        
        // Find existing shipment for this order or create new one
        Optional<Shipment> existingShipment = shipmentRepository.findByOrder_Id(orderId);
        
        if (existingShipment.isPresent()) {
            Shipment shipment = existingShipment.get();
            shipment.setTrackingNumber(trackingNumber);
            shipmentRepository.save(shipment);
        } else {
            log.warn("No shipment found for order {}. Tracking number {} noted but shipment may need to be created.", 
                    orderId, trackingNumber);
        }
    }

    /**
     * Initialize order tracking when order is shipped
     */
    @Transactional
    public void initializeOrderTracking(UUID orderId) {
        log.info("Initializing tracking for order: {}", orderId);
        
        // Find shipment for this order
        Optional<Shipment> shipmentOpt = shipmentRepository.findByOrder_Id(orderId);
        
        if (shipmentOpt.isPresent()) {
            Shipment shipment = shipmentOpt.get();
            if (shipment.getTrackingNumber() != null && !shipment.getTrackingNumber().isEmpty()) {
                // Start tracking updates for this shipment
                updateTrackingInfoAsync(shipment.getTrackingNumber());
                log.info("Started tracking updates for order {} with tracking number: {}", 
                        orderId, shipment.getTrackingNumber());
            } else {
                log.warn("Cannot initialize tracking for order {} - no tracking number available", orderId);
            }
        } else {
            log.warn("Cannot initialize tracking for order {} - no shipment found", orderId);
        }
    }

    /**
     * Statistics data class
     */
    @lombok.Data
    @lombok.Builder
    public static class TrackingStatistics {
        private java.util.Map<ShipmentStatus, Long> statusCounts;
        private java.util.Map<String, Long> eventTypeCounts;
        private LocalDateTime lastUpdated;
    }
}