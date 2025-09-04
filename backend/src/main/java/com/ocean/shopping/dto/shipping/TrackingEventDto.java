package com.ocean.shopping.dto.shipping;

import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO for individual tracking events
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrackingEventDto {
    
    private String eventId;
    
    private ShipmentStatus status;
    
    private String statusDescription;
    
    private String eventDescription;
    
    private LocalDateTime eventTime;
    
    private String location;
    
    private String city;
    
    private String state;
    
    private String country;
    
    private String postalCode;
    
    private String facilityName; // Sorting facility, delivery center, etc.
    
    private String eventType; // PICKUP, TRANSIT, DELIVERY, EXCEPTION
    
    private String eventCode; // Carrier-specific event code
    
    private String reasonCode; // Reason for exception or delay
    
    private String reasonDescription;
    
    private String nextAction; // What happens next
    
    private Boolean isDeliveryAttempt;
    
    private Boolean isException;
    
    private String signedBy; // Who signed for delivery
    
    /**
     * Get formatted location string
     */
    public String getFormattedLocation() {
        StringBuilder location = new StringBuilder();
        
        if (city != null && !city.trim().isEmpty()) {
            location.append(city);
        }
        
        if (state != null && !state.trim().isEmpty()) {
            if (location.length() > 0) location.append(", ");
            location.append(state);
        }
        
        if (country != null && !country.trim().isEmpty()) {
            if (location.length() > 0) location.append(", ");
            location.append(country);
        }
        
        return location.toString();
    }
    
    /**
     * Check if this is a terminal event (delivery completed or failed)
     */
    public boolean isTerminalEvent() {
        return status != null && status.isTerminal();
    }
}