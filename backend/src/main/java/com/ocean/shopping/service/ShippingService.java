package com.ocean.shopping.service;

import com.ocean.shopping.dto.shipping.*;
import com.ocean.shopping.model.entity.Order;
import com.ocean.shopping.model.entity.Shipment;
import com.ocean.shopping.model.entity.TrackingEvent;
import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import com.ocean.shopping.repository.ShipmentRepository;
import com.ocean.shopping.repository.TrackingEventRepository;
import com.ocean.shopping.service.logistics.CarrierException;
import com.ocean.shopping.service.logistics.CarrierManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Main shipping service for managing shipments and rates
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ShippingService {

    private final CarrierManager carrierManager;
    private final ShipmentRepository shipmentRepository;
    private final TrackingEventRepository trackingEventRepository;

    /**
     * Calculate shipping rates for an order
     */
    public List<ShippingRateResponse> calculateShippingRates(ShippingRateRequest request) throws CarrierException {
        log.info("Calculating shipping rates from {} to {}", 
                request.getOriginAddress().getCity(), request.getDestinationAddress().getCity());

        try {
            List<ShippingRateResponse> rates = carrierManager.getRatesFromAllCarriers(request);
            
            // Filter out unavailable rates
            List<ShippingRateResponse> availableRates = rates.stream()
                    .filter(rate -> Boolean.TRUE.equals(rate.getAvailable()))
                    .filter(rate -> rate.isQuoteValid())
                    .collect(Collectors.toList());

            log.info("Found {} available shipping rates", availableRates.size());
            return availableRates;
            
        } catch (Exception e) {
            log.error("Failed to calculate shipping rates: {}", e.getMessage(), e);
            throw new CarrierException("Failed to calculate shipping rates", e);
        }
    }

    /**
     * Get the cheapest shipping rate
     */
    public ShippingRateResponse getCheapestRate(ShippingRateRequest request) throws CarrierException {
        ShippingRateResponse bestRate = carrierManager.getBestRate(request);
        if (bestRate == null) {
            throw new CarrierException("No shipping rates available for the given criteria");
        }
        return bestRate;
    }

    /**
     * Get the fastest shipping option
     */
    public ShippingRateResponse getFastestRate(ShippingRateRequest request) throws CarrierException {
        ShippingRateResponse fastestRate = carrierManager.getFastestDelivery(request);
        if (fastestRate == null) {
            throw new CarrierException("No express shipping options available for the given criteria");
        }
        return fastestRate;
    }

    /**
     * Create a shipment for an order
     */
    public Shipment createShipment(Order order, ShipmentRequest shipmentRequest) throws CarrierException {
        log.info("Creating shipment for order {} with carrier {}", 
                order.getOrderNumber(), shipmentRequest.getCarrier());

        try {
            // Create shipment with carrier
            ShipmentResponse response = carrierManager.createShipment(shipmentRequest);

            // Save shipment entity
            Shipment shipment = buildShipmentFromResponse(order, shipmentRequest, response);
            shipment = shipmentRepository.save(shipment);

            // Create initial tracking event
            TrackingEvent initialEvent = TrackingEvent.builder()
                    .shipment(shipment)
                    .status(ShipmentStatus.PENDING)
                    .statusDescription("Shipment created")
                    .eventDescription("Shipment has been created and is awaiting pickup")
                    .eventTime(LocalDateTime.now())
                    .eventType("CREATED")
                    .processedAt(LocalDateTime.now())
                    .notificationSent(false)
                    .build();

            trackingEventRepository.save(initialEvent);

            log.info("Successfully created shipment {} with tracking number {}", 
                    shipment.getId(), shipment.getTrackingNumber());

            return shipment;

        } catch (CarrierException e) {
            log.error("Failed to create shipment for order {}: {}", 
                     order.getOrderNumber(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error creating shipment for order {}: {}", 
                     order.getOrderNumber(), e.getMessage(), e);
            throw new CarrierException("Failed to create shipment", e);
        }
    }

    /**
     * Get shipment by tracking number
     */
    @Transactional(readOnly = true)
    public Optional<Shipment> getShipmentByTrackingNumber(String trackingNumber) {
        return shipmentRepository.findByTrackingNumber(trackingNumber);
    }

    /**
     * Get all shipments for an order
     */
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsByOrder(Order order) {
        return shipmentRepository.findByOrder(order);
    }

    /**
     * Get shipments by customer email
     */
    @Transactional(readOnly = true)
    public Page<Shipment> getShipmentsByCustomerEmail(String email, Pageable pageable) {
        return shipmentRepository.findByCustomerEmail(email, pageable);
    }

    /**
     * Track a shipment and update database
     */
    public TrackingResponse trackShipment(String trackingNumber) throws CarrierException {
        log.info("Tracking shipment: {}", trackingNumber);

        try {
            // Get tracking information from carrier
            TrackingResponse trackingResponse = carrierManager.trackShipment(trackingNumber);

            // Update local shipment data
            Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
            if (shipmentOpt.isPresent()) {
                updateShipmentFromTracking(shipmentOpt.get(), trackingResponse);
            } else {
                log.warn("Shipment not found in database for tracking number: {}", trackingNumber);
            }

            return trackingResponse;

        } catch (CarrierException e) {
            log.error("Failed to track shipment {}: {}", trackingNumber, e.getMessage());
            throw e;
        }
    }

    /**
     * Track multiple shipments
     */
    public List<TrackingResponse> trackMultipleShipments(List<String> trackingNumbers) {
        log.info("Tracking {} shipments", trackingNumbers.size());
        
        List<TrackingResponse> responses = carrierManager.trackMultipleShipments(trackingNumbers);
        
        // Update database for each successful tracking response
        for (TrackingResponse response : responses) {
            try {
                Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(response.getTrackingNumber());
                if (shipmentOpt.isPresent()) {
                    updateShipmentFromTracking(shipmentOpt.get(), response);
                }
            } catch (Exception e) {
                log.warn("Failed to update shipment {} from tracking: {}", 
                        response.getTrackingNumber(), e.getMessage());
            }
        }
        
        return responses;
    }

    /**
     * Cancel a shipment
     */
    public boolean cancelShipment(String trackingNumber) throws CarrierException {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
        if (shipmentOpt.isEmpty()) {
            throw new CarrierException("Shipment not found: " + trackingNumber);
        }

        Shipment shipment = shipmentOpt.get();
        
        try {
            boolean cancelled = carrierManager.cancelShipment(trackingNumber, shipment.getCarrier());
            
            if (cancelled) {
                // Update shipment status
                shipment.setStatus(ShipmentStatus.CANCELLED);
                shipment.setStatusDescription("Shipment cancelled");
                shipmentRepository.save(shipment);

                // Add tracking event
                TrackingEvent cancelEvent = TrackingEvent.builder()
                        .shipment(shipment)
                        .status(ShipmentStatus.CANCELLED)
                        .statusDescription("Shipment cancelled")
                        .eventDescription("Shipment has been cancelled")
                        .eventTime(LocalDateTime.now())
                        .eventType("CANCELLED")
                        .processedAt(LocalDateTime.now())
                        .notificationSent(false)
                        .build();

                trackingEventRepository.save(cancelEvent);
                
                log.info("Successfully cancelled shipment: {}", trackingNumber);
            }

            return cancelled;

        } catch (CarrierException e) {
            log.error("Failed to cancel shipment {}: {}", trackingNumber, e.getMessage());
            throw e;
        }
    }

    /**
     * Generate shipping label
     */
    public String generateShippingLabel(String trackingNumber) throws CarrierException {
        Optional<Shipment> shipmentOpt = shipmentRepository.findByTrackingNumber(trackingNumber);
        if (shipmentOpt.isEmpty()) {
            throw new CarrierException("Shipment not found: " + trackingNumber);
        }

        Shipment shipment = shipmentOpt.get();
        return carrierManager.generateLabel(trackingNumber, shipment.getCarrier());
    }

    /**
     * Validate shipping address
     */
    public AddressValidationResponse validateAddress(AddressValidationRequest request) throws CarrierException {
        return validateAddress(request, null);
    }

    /**
     * Validate shipping address with preferred carrier
     */
    public AddressValidationResponse validateAddress(AddressValidationRequest request, 
                                                   CarrierType preferredCarrier) throws CarrierException {
        try {
            return carrierManager.validateAddress(request, preferredCarrier);
        } catch (CarrierException e) {
            log.error("Address validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Get shipments that need tracking updates
     */
    @Transactional(readOnly = true)
    public List<Shipment> getShipmentsNeedingUpdate() {
        List<ShipmentStatus> activeStatuses = List.of(
            ShipmentStatus.PENDING,
            ShipmentStatus.PICKED_UP, 
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.OUT_FOR_DELIVERY,
            ShipmentStatus.DELAYED,
            ShipmentStatus.EXCEPTION
        );
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2); // Update every 2 hours
        return shipmentRepository.findShipmentsNeedingUpdate(activeStatuses, cutoffTime);
    }

    /**
     * Get overdue shipments
     */
    @Transactional(readOnly = true)
    public List<Shipment> getOverdueShipments() {
        List<ShipmentStatus> terminalStatuses = List.of(
            ShipmentStatus.DELIVERED,
            ShipmentStatus.RETURNED,
            ShipmentStatus.CANCELLED
        );
        
        return shipmentRepository.findOverdueShipments(java.time.LocalDate.now(), terminalStatuses);
    }

    /**
     * Get carrier status
     */
    public java.util.Map<CarrierType, Boolean> getCarrierStatus() {
        return carrierManager.getCarrierStatus();
    }

    /**
     * Build shipment entity from carrier response
     */
    private Shipment buildShipmentFromResponse(Order order, ShipmentRequest request, ShipmentResponse response) {
        return Shipment.builder()
                .trackingNumber(response.getTrackingNumber())
                .order(order)
                .carrier(response.getCarrier())
                .serviceType(response.getServiceType())
                .serviceName(request.getServiceType().getDisplayName())
                .status(response.getStatus())
                .statusDescription(response.getStatus().getDescription())
                
                // Shipper address
                .shipperName(request.getShipperAddress().getFullName())
                .shipperCompany(request.getShipperAddress().getCompany())
                .shipperAddressLine1(request.getShipperAddress().getAddressLine1())
                .shipperAddressLine2(request.getShipperAddress().getAddressLine2())
                .shipperCity(request.getShipperAddress().getCity())
                .shipperState(request.getShipperAddress().getState())
                .shipperPostalCode(request.getShipperAddress().getPostalCode())
                .shipperCountry(request.getShipperAddress().getCountry())
                .shipperPhone(request.getShipperAddress().getPhone())
                .shipperEmail(request.getShipperAddress().getEmail())
                
                // Recipient address
                .recipientName(request.getRecipientAddress().getFullName())
                .recipientCompany(request.getRecipientAddress().getCompany())
                .recipientAddressLine1(request.getRecipientAddress().getAddressLine1())
                .recipientAddressLine2(request.getRecipientAddress().getAddressLine2())
                .recipientCity(request.getRecipientAddress().getCity())
                .recipientState(request.getRecipientAddress().getState())
                .recipientPostalCode(request.getRecipientAddress().getPostalCode())
                .recipientCountry(request.getRecipientAddress().getCountry())
                .recipientPhone(request.getRecipientAddress().getPhone())
                .recipientEmail(request.getRecipientAddress().getEmail())
                
                // Package information
                .weight(getTotalWeight(request.getPackages()))
                .packageCount(request.getPackages().size())
                .packageDescription(getPackageDescription(request.getPackages()))
                
                // Pricing and service options
                .shippingCost(response.getActualCost())
                .currency(response.getCurrency())
                .declaredValue(request.getDeclaredValue())
                .insuredValue(response.getInsuredValue())
                .shipDate(response.getShipDate())
                .estimatedDeliveryDate(response.getEstimatedDeliveryDate())
                .signatureRequired(request.isSignatureRequired())
                .adultSignatureRequired(request.isAdultSignatureRequired())
                .saturdayDelivery(request.isSaturdayDelivery())
                .insuranceIncluded(response.getInsuranceIncluded())
                
                // References and labels
                .reference(request.getReference())
                .customerReference(request.getOrderNumber())
                .specialInstructions(request.getSpecialInstructions())
                .deliveryInstructions(request.getDeliveryInstructions())
                .labelUrl(response.getLabelUrl())
                .labelFormat(response.getLabelFormat())
                .trackingUrl(response.getTrackingUrl())
                .additionalTrackingNumbers(response.getAdditionalTrackingNumbers())
                
                .lastTrackingUpdate(LocalDateTime.now())
                .build();
    }

    /**
     * Update shipment from tracking response
     */
    private void updateShipmentFromTracking(Shipment shipment, TrackingResponse trackingResponse) {
        // Update shipment status
        if (trackingResponse.getCurrentStatus() != null) {
            shipment.setStatus(trackingResponse.getCurrentStatus());
            shipment.setStatusDescription(trackingResponse.getStatusDescription());
        }
        
        shipment.setCurrentLocation(trackingResponse.getCurrentLocation());
        shipment.setLastTrackingUpdate(LocalDateTime.now());
        
        if (trackingResponse.getActualDeliveryDate() != null) {
            shipment.setActualDeliveryDate(trackingResponse.getActualDeliveryDate());
        }
        
        if (trackingResponse.getDeliveryTime() != null) {
            shipment.setDeliveryTime(trackingResponse.getDeliveryTime());
        }
        
        if (trackingResponse.getSignedBy() != null) {
            shipment.setSignedBy(trackingResponse.getSignedBy());
        }

        shipmentRepository.save(shipment);

        // Update tracking events
        List<TrackingEventDto> newEvents = trackingResponse.getTrackingHistory();
        if (newEvents != null) {
            for (TrackingEventDto eventDto : newEvents) {
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
                }
            }
        }
    }

    /**
     * Calculate total weight from packages
     */
    private BigDecimal getTotalWeight(List<PackageDto> packages) {
        return packages.stream()
                .map(PackageDto::getWeight)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Generate package description
     */
    private String getPackageDescription(List<PackageDto> packages) {
        return packages.stream()
                .map(pkg -> pkg.getDescription() != null ? pkg.getDescription() : "Package")
                .collect(Collectors.joining(", "));
    }
}