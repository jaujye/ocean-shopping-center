package com.ocean.shopping.dto.shipping;

import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ServiceType;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Response DTO for shipment creation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    
    private String shipmentId; // Internal shipment ID
    
    private String trackingNumber; // Carrier tracking number
    
    private List<String> additionalTrackingNumbers; // For multi-package shipments
    
    private CarrierType carrier;
    
    private ServiceType serviceType;
    
    private ShipmentStatus status;
    
    private BigDecimal actualCost;
    
    private String currency;
    
    private LocalDate shipDate;
    
    private LocalDate estimatedDeliveryDate;
    
    private LocalDateTime createdAt;
    
    private String labelUrl; // URL to download shipping label
    
    private String labelData; // Base64 encoded label data
    
    private String labelFormat; // PDF, PNG, etc.
    
    private List<String> documents; // Additional documents (commercial invoice, etc.)
    
    private Map<String, String> carrierMetadata; // Carrier-specific information
    
    private String reference; // Customer reference
    
    private String orderNumber; // Customer order number
    
    private Boolean insuranceIncluded;
    
    private BigDecimal insuredValue;
    
    private String trackingUrl; // Direct URL to carrier tracking page
    
    private List<TrackingEventDto> initialTrackingEvents;
    
    /**
     * Check if shipment has multiple packages
     */
    public boolean isMultiPackage() {
        return additionalTrackingNumbers != null && !additionalTrackingNumbers.isEmpty();
    }
    
    /**
     * Get all tracking numbers including primary
     */
    public List<String> getAllTrackingNumbers() {
        List<String> allTracking = new java.util.ArrayList<>();
        allTracking.add(trackingNumber);
        if (additionalTrackingNumbers != null) {
            allTracking.addAll(additionalTrackingNumbers);
        }
        return allTracking;
    }
}