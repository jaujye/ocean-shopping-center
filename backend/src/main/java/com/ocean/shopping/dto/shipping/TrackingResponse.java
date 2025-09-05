package com.ocean.shopping.dto.shipping;

import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ServiceType;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for shipment tracking
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingResponse {
    
    private String trackingNumber;
    
    private CarrierType carrier;
    
    private ServiceType serviceType;
    
    private ShipmentStatus currentStatus;
    
    private String statusDescription;
    
    private LocalDateTime lastUpdated;
    
    private LocalDate shipDate;
    
    private LocalDate estimatedDeliveryDate;
    
    private LocalDate actualDeliveryDate;
    
    private LocalDateTime deliveryTime;
    
    private String signedBy; // Who signed for delivery
    
    private AddressDto shipperAddress;
    
    private AddressDto recipientAddress;
    
    private String currentLocation;
    
    private List<TrackingEventDto> trackingHistory;
    
    private String specialInstructions;
    
    private String deliveryInstructions;
    
    private Boolean isDelivered;
    
    private Boolean hasException;
    
    private String exceptionReason;
    
    private String nextAction; // What customer should do next
    
    private Integer totalEvents;
    
    private String trackingUrl; // Direct link to carrier tracking page
    
    private LocalDateTime nextUpdateExpected;
    
    /**
     * Get the most recent tracking event
     */
    public TrackingEventDto getLatestEvent() {
        if (trackingHistory == null || trackingHistory.isEmpty()) {
            return null;
        }
        return trackingHistory.get(0); // Assuming sorted by most recent first
    }
    
    /**
     * Check if shipment is in transit
     */
    public boolean isInTransit() {
        return currentStatus != null && currentStatus.isActive();
    }
    
    /**
     * Get delivery progress percentage (0-100)
     */
    public Integer getDeliveryProgress() {
        if (currentStatus == null) return 0;
        
        switch (currentStatus) {
            case PENDING:
                return 10;
            case PICKED_UP:
                return 25;
            case IN_TRANSIT:
                return 50;
            case OUT_FOR_DELIVERY:
                return 90;
            case DELIVERED:
                return 100;
            case RETURNED:
            case CANCELLED:
                return 0;
            default:
                return 25;
        }
    }
    
    /**
     * Get expected delivery status text
     */
    public String getDeliveryStatusText() {
        if (Boolean.TRUE.equals(isDelivered)) {
            return "Delivered" + (actualDeliveryDate != null ? " on " + actualDeliveryDate : "");
        }
        
        if (Boolean.TRUE.equals(hasException)) {
            return "Delivery Exception: " + (exceptionReason != null ? exceptionReason : "Unknown issue");
        }
        
        if (estimatedDeliveryDate != null) {
            return "Expected delivery: " + estimatedDeliveryDate;
        }
        
        return "In transit";
    }
}