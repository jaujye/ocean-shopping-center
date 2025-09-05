package com.ocean.shopping.service.logistics;

import com.ocean.shopping.dto.shipping.ShippingRateRequest;
import com.ocean.shopping.dto.shipping.ShippingRateResponse;
import com.ocean.shopping.dto.shipping.ShipmentRequest;
import com.ocean.shopping.dto.shipping.ShipmentResponse;
import com.ocean.shopping.dto.shipping.TrackingResponse;
import com.ocean.shopping.model.entity.enums.CarrierType;

import java.util.List;

/**
 * Interface for carrier-specific logistics operations
 * Provides abstraction layer for different shipping carriers (DHL, FedEx, UPS, etc.)
 */
public interface CarrierService {
    
    /**
     * Get supported carrier type
     * 
     * @return The carrier type this service handles
     */
    CarrierType getCarrierType();
    
    /**
     * Check if carrier service is available
     * 
     * @return true if service is available, false otherwise
     */
    boolean isAvailable();
    
    /**
     * Calculate shipping rates for given request
     * 
     * @param request The shipping rate calculation request
     * @return List of available shipping rates
     * @throws CarrierException if rate calculation fails
     */
    List<ShippingRateResponse> calculateRates(ShippingRateRequest request) throws CarrierException;
    
    /**
     * Create a shipment with the carrier
     * 
     * @param request The shipment creation request
     * @return Shipment creation response with tracking information
     * @throws CarrierException if shipment creation fails
     */
    ShipmentResponse createShipment(ShipmentRequest request) throws CarrierException;
    
    /**
     * Track a shipment by tracking number
     * 
     * @param trackingNumber The tracking number to track
     * @return Current tracking status and history
     * @throws CarrierException if tracking fails
     */
    TrackingResponse trackShipment(String trackingNumber) throws CarrierException;
    
    /**
     * Cancel a shipment
     * 
     * @param trackingNumber The tracking number of shipment to cancel
     * @return true if cancellation successful, false otherwise
     * @throws CarrierException if cancellation fails
     */
    boolean cancelShipment(String trackingNumber) throws CarrierException;
    
    /**
     * Generate shipping label for shipment
     * 
     * @param trackingNumber The tracking number
     * @return Base64 encoded label data
     * @throws CarrierException if label generation fails
     */
    String generateLabel(String trackingNumber) throws CarrierException;
    
    /**
     * Validate address with carrier
     * 
     * @param address The address to validate
     * @return validated/corrected address or null if invalid
     * @throws CarrierException if validation fails
     */
    com.ocean.shopping.dto.shipping.AddressValidationResponse validateAddress(
            com.ocean.shopping.dto.shipping.AddressValidationRequest address) throws CarrierException;
}