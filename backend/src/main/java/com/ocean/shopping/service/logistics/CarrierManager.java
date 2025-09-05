package com.ocean.shopping.service.logistics;

import com.ocean.shopping.dto.shipping.ShippingRateRequest;
import com.ocean.shopping.dto.shipping.ShippingRateResponse;
import com.ocean.shopping.dto.shipping.ShipmentRequest;
import com.ocean.shopping.dto.shipping.ShipmentResponse;
import com.ocean.shopping.dto.shipping.TrackingResponse;
import com.ocean.shopping.dto.shipping.AddressValidationRequest;
import com.ocean.shopping.dto.shipping.AddressValidationResponse;
import com.ocean.shopping.model.entity.enums.CarrierType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Manages multiple carrier services and provides rate shopping functionality
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarrierManager {

    private final Map<CarrierType, CarrierService> carrierServices;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * Get all available carriers
     */
    public List<CarrierType> getAvailableCarriers() {
        return carrierServices.entrySet().stream()
                .filter(entry -> entry.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Get carrier service by type
     */
    public CarrierService getCarrierService(CarrierType carrierType) throws CarrierException {
        CarrierService service = carrierServices.get(carrierType);
        if (service == null) {
            throw new CarrierException("Carrier service not found: " + carrierType);
        }
        if (!service.isAvailable()) {
            throw new CarrierException("Carrier service not available: " + carrierType);
        }
        return service;
    }

    /**
     * Rate shopping - get rates from all available carriers
     */
    public List<ShippingRateResponse> getRatesFromAllCarriers(ShippingRateRequest request) {
        List<CarrierType> preferredCarriers = request.getPreferredCarriers();
        
        // Use preferred carriers if specified, otherwise use all available carriers
        List<CarrierType> carriersToQuery = preferredCarriers != null && !preferredCarriers.isEmpty() 
            ? preferredCarriers 
            : getAvailableCarriers();

        List<CompletableFuture<List<ShippingRateResponse>>> futures = carriersToQuery.stream()
                .map(carrierType -> CompletableFuture.supplyAsync(() -> {
                    try {
                        CarrierService service = getCarrierService(carrierType);
                        return service.calculateRates(request);
                    } catch (CarrierException e) {
                        log.warn("Failed to get rates from carrier {}: {}", carrierType, e.getMessage());
                        return new ArrayList<ShippingRateResponse>();
                    }
                }, executorService))
                .collect(Collectors.toList());

        // Wait for all futures to complete and combine results
        List<ShippingRateResponse> allRates = futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        // Sort by total cost (cheapest first)
        allRates.sort(Comparator.comparing(ShippingRateResponse::getTotalCost));

        log.info("Rate shopping completed. Found {} rates from {} carriers", 
                allRates.size(), carriersToQuery.size());

        return allRates;
    }

    /**
     * Get best rate (cheapest) from all carriers
     */
    public ShippingRateResponse getBestRate(ShippingRateRequest request) {
        List<ShippingRateResponse> rates = getRatesFromAllCarriers(request);
        return rates.isEmpty() ? null : rates.get(0);
    }

    /**
     * Get fastest delivery option from all carriers
     */
    public ShippingRateResponse getFastestDelivery(ShippingRateRequest request) {
        List<ShippingRateResponse> rates = getRatesFromAllCarriers(request);
        
        return rates.stream()
                .filter(rate -> rate.getTransitDays() != null)
                .min(Comparator.comparing(ShippingRateResponse::getTransitDays))
                .orElse(rates.isEmpty() ? null : rates.get(0));
    }

    /**
     * Create shipment with specific carrier
     */
    public ShipmentResponse createShipment(ShipmentRequest request) throws CarrierException {
        CarrierService service = getCarrierService(request.getCarrier());
        
        log.info("Creating shipment with carrier {} for order {}", 
                request.getCarrier(), request.getOrderNumber());
        
        try {
            ShipmentResponse response = service.createShipment(request);
            log.info("Successfully created shipment {} with tracking number {}", 
                    response.getShipmentId(), response.getTrackingNumber());
            return response;
        } catch (CarrierException e) {
            log.error("Failed to create shipment with carrier {}: {}", 
                     request.getCarrier(), e.getMessage());
            throw e;
        }
    }

    /**
     * Track shipment - automatically detect carrier if not specified
     */
    public TrackingResponse trackShipment(String trackingNumber) throws CarrierException {
        return trackShipment(trackingNumber, null);
    }

    /**
     * Track shipment with specific carrier
     */
    public TrackingResponse trackShipment(String trackingNumber, CarrierType carrierType) throws CarrierException {
        if (carrierType != null) {
            // Use specified carrier
            CarrierService service = getCarrierService(carrierType);
            return service.trackShipment(trackingNumber);
        }

        // Try all carriers to find the tracking number
        List<CarrierType> availableCarriers = getAvailableCarriers();
        CarrierException lastException = null;

        for (CarrierType carrier : availableCarriers) {
            try {
                CarrierService service = getCarrierService(carrier);
                TrackingResponse response = service.trackShipment(trackingNumber);
                if (response != null) {
                    log.info("Found tracking information for {} with carrier {}", 
                            trackingNumber, carrier);
                    return response;
                }
            } catch (CarrierException e) {
                log.debug("Carrier {} could not track {}: {}", carrier, trackingNumber, e.getMessage());
                lastException = e;
            }
        }

        throw new CarrierException("Unable to track shipment " + trackingNumber + 
                                 " with any available carrier", lastException);
    }

    /**
     * Track multiple shipments in parallel
     */
    public List<TrackingResponse> trackMultipleShipments(List<String> trackingNumbers) {
        List<CompletableFuture<TrackingResponse>> futures = trackingNumbers.stream()
                .map(trackingNumber -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return trackShipment(trackingNumber);
                    } catch (CarrierException e) {
                        log.warn("Failed to track shipment {}: {}", trackingNumber, e.getMessage());
                        return null;
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    /**
     * Cancel shipment
     */
    public boolean cancelShipment(String trackingNumber, CarrierType carrierType) throws CarrierException {
        CarrierService service = getCarrierService(carrierType);
        
        log.info("Cancelling shipment {} with carrier {}", trackingNumber, carrierType);
        
        boolean cancelled = service.cancelShipment(trackingNumber);
        if (cancelled) {
            log.info("Successfully cancelled shipment {}", trackingNumber);
        } else {
            log.warn("Failed to cancel shipment {}", trackingNumber);
        }
        
        return cancelled;
    }

    /**
     * Generate shipping label
     */
    public String generateLabel(String trackingNumber, CarrierType carrierType) throws CarrierException {
        CarrierService service = getCarrierService(carrierType);
        return service.generateLabel(trackingNumber);
    }

    /**
     * Validate address with preferred carrier (or first available)
     */
    public AddressValidationResponse validateAddress(AddressValidationRequest request) throws CarrierException {
        return validateAddress(request, null);
    }

    /**
     * Validate address with specific carrier
     */
    public AddressValidationResponse validateAddress(AddressValidationRequest request, 
                                                   CarrierType preferredCarrier) throws CarrierException {
        List<CarrierType> carriersToTry = new ArrayList<>();
        
        if (preferredCarrier != null) {
            carriersToTry.add(preferredCarrier);
        }
        
        // Add other available carriers as fallback
        getAvailableCarriers().stream()
                .filter(carrier -> !carriersToTry.contains(carrier))
                .forEach(carriersToTry::add);

        CarrierException lastException = null;
        
        for (CarrierType carrier : carriersToTry) {
            try {
                CarrierService service = getCarrierService(carrier);
                AddressValidationResponse response = service.validateAddress(request);
                if (response != null && response.isValidationSuccessful()) {
                    return response;
                }
            } catch (CarrierException e) {
                log.debug("Address validation failed with carrier {}: {}", carrier, e.getMessage());
                lastException = e;
            }
        }

        throw new CarrierException("Address validation failed with all available carriers", lastException);
    }

    /**
     * Check carrier availability
     */
    public Map<CarrierType, Boolean> getCarrierStatus() {
        return carrierServices.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().isAvailable()
                ));
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}