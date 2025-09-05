package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TrackingEvent entity for shipment tracking history
 */
@Entity
@Table(name = "tracking_events", indexes = {
    @Index(name = "idx_tracking_events_shipment_id", columnList = "shipment_id"),
    @Index(name = "idx_tracking_events_event_time", columnList = "event_time"),
    @Index(name = "idx_tracking_events_status", columnList = "status"),
    @Index(name = "idx_tracking_events_event_type", columnList = "event_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    @NotNull(message = "Shipment is required")
    private Shipment shipment;

    @Column(name = "event_id")
    @Size(max = 100)
    private String eventId; // Carrier-specific event ID

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Status is required")
    private ShipmentStatus status;

    @Column(name = "status_description")
    @Size(max = 255)
    private String statusDescription;

    @Column(name = "event_description", columnDefinition = "TEXT")
    private String eventDescription;

    @Column(name = "event_time", nullable = false)
    @NotNull(message = "Event time is required")
    private LocalDateTime eventTime;

    @Column(name = "event_type")
    @Size(max = 50)
    private String eventType; // PICKUP, TRANSIT, DELIVERY, EXCEPTION

    @Column(name = "event_code")
    @Size(max = 50)
    private String eventCode; // Carrier-specific event code

    @Column(name = "location")
    @Size(max = 255)
    private String location;

    @Column(name = "city")
    @Size(max = 100)
    private String city;

    @Column(name = "state")
    @Size(max = 100)
    private String state;

    @Column(name = "country")
    @Size(max = 2)
    private String country;

    @Column(name = "postal_code")
    @Size(max = 20)
    private String postalCode;

    @Column(name = "facility_name")
    @Size(max = 255)
    private String facilityName; // Sorting facility, delivery center, etc.

    @Column(name = "reason_code")
    @Size(max = 50)
    private String reasonCode; // Reason for exception or delay

    @Column(name = "reason_description")
    @Size(max = 500)
    private String reasonDescription;

    @Column(name = "next_action")
    @Size(max = 500)
    private String nextAction; // What happens next

    @Column(name = "signed_by")
    @Size(max = 100)
    private String signedBy; // Who signed for delivery

    @Column(name = "is_delivery_attempt")
    @Builder.Default
    private Boolean isDeliveryAttempt = false;

    @Column(name = "is_exception")
    @Builder.Default
    private Boolean isException = false;

    @Column(name = "is_final_delivery")
    @Builder.Default
    private Boolean isFinalDelivery = false;

    @Column(name = "delivery_type")
    @Size(max = 50)
    private String deliveryType; // FRONT_DOOR, BACK_DOOR, NEIGHBOR, etc.

    @Column(name = "attempted_delivery_time")
    private LocalDateTime attemptedDeliveryTime;

    // Carrier-specific raw data
    @Column(name = "raw_event_data", columnDefinition = "TEXT")
    private String rawEventData; // JSON string of original carrier data

    @Column(name = "carrier_timestamp")
    private LocalDateTime carrierTimestamp; // Original timestamp from carrier

    @Column(name = "timezone")
    @Size(max = 50)
    private String timezone;

    // Processing information
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    // Helper methods
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

    public boolean isTerminalEvent() {
        return status != null && status.isTerminal();
    }

    public boolean isDeliveryEvent() {
        return status == ShipmentStatus.DELIVERED || Boolean.TRUE.equals(isFinalDelivery);
    }

    public boolean isExceptionEvent() {
        return Boolean.TRUE.equals(isException) || 
               status == ShipmentStatus.EXCEPTION || 
               status == ShipmentStatus.DELAYED;
    }

    public String getDisplayText() {
        StringBuilder text = new StringBuilder();
        
        if (statusDescription != null && !statusDescription.trim().isEmpty()) {
            text.append(statusDescription);
        } else if (eventDescription != null && !eventDescription.trim().isEmpty()) {
            text.append(eventDescription);
        } else {
            text.append(status.getDisplayName());
        }
        
        String loc = getFormattedLocation();
        if (!loc.isEmpty()) {
            text.append(" - ").append(loc);
        }
        
        return text.toString();
    }
}