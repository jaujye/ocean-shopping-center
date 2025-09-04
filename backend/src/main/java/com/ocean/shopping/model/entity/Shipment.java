package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.CarrierType;
import com.ocean.shopping.model.entity.enums.ServiceType;
import com.ocean.shopping.model.entity.enums.ShipmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Shipment entity for tracking shipments
 */
@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipments_tracking_number", columnList = "tracking_number"),
    @Index(name = "idx_shipments_order_id", columnList = "order_id"),
    @Index(name = "idx_shipments_carrier", columnList = "carrier"),
    @Index(name = "idx_shipments_status", columnList = "status"),
    @Index(name = "idx_shipments_ship_date", columnList = "ship_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment extends BaseEntity {

    @Column(name = "tracking_number", unique = true, nullable = false)
    @NotBlank(message = "Tracking number is required")
    @Size(max = 100)
    private String trackingNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @NotNull(message = "Order is required")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "carrier", nullable = false)
    @NotNull(message = "Carrier is required")
    private CarrierType carrier;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false)
    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @Column(name = "service_name")
    @Size(max = 100)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "status_description")
    @Size(max = 255)
    private String statusDescription;

    // Shipper address
    @Column(name = "shipper_name", nullable = false)
    @NotBlank(message = "Shipper name is required")
    @Size(max = 200)
    private String shipperName;

    @Column(name = "shipper_company")
    @Size(max = 100)
    private String shipperCompany;

    @Column(name = "shipper_address_line_1", nullable = false)
    @NotBlank(message = "Shipper address is required")
    @Size(max = 255)
    private String shipperAddressLine1;

    @Column(name = "shipper_address_line_2")
    @Size(max = 255)
    private String shipperAddressLine2;

    @Column(name = "shipper_city", nullable = false)
    @NotBlank(message = "Shipper city is required")
    @Size(max = 100)
    private String shipperCity;

    @Column(name = "shipper_state")
    @Size(max = 100)
    private String shipperState;

    @Column(name = "shipper_postal_code", nullable = false)
    @NotBlank(message = "Shipper postal code is required")
    @Size(max = 20)
    private String shipperPostalCode;

    @Column(name = "shipper_country", nullable = false)
    @NotBlank(message = "Shipper country is required")
    @Size(min = 2, max = 2)
    private String shipperCountry;

    @Column(name = "shipper_phone")
    @Size(max = 20)
    private String shipperPhone;

    @Column(name = "shipper_email")
    @Size(max = 255)
    private String shipperEmail;

    // Recipient address
    @Column(name = "recipient_name", nullable = false)
    @NotBlank(message = "Recipient name is required")
    @Size(max = 200)
    private String recipientName;

    @Column(name = "recipient_company")
    @Size(max = 100)
    private String recipientCompany;

    @Column(name = "recipient_address_line_1", nullable = false)
    @NotBlank(message = "Recipient address is required")
    @Size(max = 255)
    private String recipientAddressLine1;

    @Column(name = "recipient_address_line_2")
    @Size(max = 255)
    private String recipientAddressLine2;

    @Column(name = "recipient_city", nullable = false)
    @NotBlank(message = "Recipient city is required")
    @Size(max = 100)
    private String recipientCity;

    @Column(name = "recipient_state")
    @Size(max = 100)
    private String recipientState;

    @Column(name = "recipient_postal_code", nullable = false)
    @NotBlank(message = "Recipient postal code is required")
    @Size(max = 20)
    private String recipientPostalCode;

    @Column(name = "recipient_country", nullable = false)
    @NotBlank(message = "Recipient country is required")
    @Size(min = 2, max = 2)
    private String recipientCountry;

    @Column(name = "recipient_phone")
    @Size(max = 20)
    private String recipientPhone;

    @Column(name = "recipient_email")
    @Size(max = 255)
    private String recipientEmail;

    // Package information
    @Column(name = "weight", precision = 8, scale = 3)
    @PositiveOrZero(message = "Weight must be positive or zero")
    private BigDecimal weight;

    @Column(name = "weight_unit")
    @Size(max = 3)
    @Builder.Default
    private String weightUnit = "kg";

    @Column(name = "dimensions")
    @Size(max = 100)
    private String dimensions; // "L x W x H"

    @Column(name = "dimension_unit")
    @Size(max = 3)
    @Builder.Default
    private String dimensionUnit = "cm";

    @Column(name = "package_count")
    @Builder.Default
    private Integer packageCount = 1;

    @Column(name = "package_description", columnDefinition = "TEXT")
    private String packageDescription;

    // Pricing
    @Column(name = "shipping_cost", precision = 10, scale = 2)
    @PositiveOrZero(message = "Shipping cost must be positive or zero")
    private BigDecimal shippingCost;

    @Column(name = "currency")
    @Size(min = 3, max = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "declared_value", precision = 10, scale = 2)
    @PositiveOrZero(message = "Declared value must be positive or zero")
    private BigDecimal declaredValue;

    @Column(name = "insured_value", precision = 10, scale = 2)
    @PositiveOrZero(message = "Insured value must be positive or zero")
    private BigDecimal insuredValue;

    // Shipping details
    @Column(name = "ship_date")
    private LocalDate shipDate;

    @Column(name = "estimated_delivery_date")
    private LocalDate estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;

    @Column(name = "delivery_time")
    private LocalDateTime deliveryTime;

    @Column(name = "signed_by")
    @Size(max = 100)
    private String signedBy;

    // Service options
    @Column(name = "signature_required")
    @Builder.Default
    private Boolean signatureRequired = false;

    @Column(name = "adult_signature_required")
    @Builder.Default
    private Boolean adultSignatureRequired = false;

    @Column(name = "saturday_delivery")
    @Builder.Default
    private Boolean saturdayDelivery = false;

    @Column(name = "insurance_included")
    @Builder.Default
    private Boolean insuranceIncluded = false;

    // References and notes
    @Column(name = "reference")
    @Size(max = 255)
    private String reference;

    @Column(name = "customer_reference")
    @Size(max = 255)
    private String customerReference;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "delivery_instructions", columnDefinition = "TEXT")
    private String deliveryInstructions;

    // Label information
    @Column(name = "label_url")
    @Size(max = 500)
    private String labelUrl;

    @Column(name = "label_format")
    @Size(max = 10)
    private String labelFormat;

    @Column(name = "tracking_url")
    @Size(max = 500)
    private String trackingUrl;

    // Additional tracking numbers for multi-package shipments
    @ElementCollection
    @CollectionTable(name = "shipment_additional_tracking", joinColumns = @JoinColumn(name = "shipment_id"))
    @Column(name = "tracking_number")
    private List<String> additionalTrackingNumbers;

    // Current location
    @Column(name = "current_location")
    @Size(max = 255)
    private String currentLocation;

    @Column(name = "last_tracking_update")
    private LocalDateTime lastTrackingUpdate;

    // Relationships
    @OneToMany(mappedBy = "shipment", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TrackingEvent> trackingEvents;

    // Helper methods
    public String getShipperFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(shipperAddressLine1);
        if (shipperAddressLine2 != null && !shipperAddressLine2.trim().isEmpty()) {
            sb.append(", ").append(shipperAddressLine2);
        }
        sb.append(", ").append(shipperCity);
        if (shipperState != null && !shipperState.trim().isEmpty()) {
            sb.append(", ").append(shipperState);
        }
        sb.append(" ").append(shipperPostalCode);
        sb.append(", ").append(shipperCountry);
        return sb.toString();
    }

    public String getRecipientFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(recipientAddressLine1);
        if (recipientAddressLine2 != null && !recipientAddressLine2.trim().isEmpty()) {
            sb.append(", ").append(recipientAddressLine2);
        }
        sb.append(", ").append(recipientCity);
        if (recipientState != null && !recipientState.trim().isEmpty()) {
            sb.append(", ").append(recipientState);
        }
        sb.append(" ").append(recipientPostalCode);
        sb.append(", ").append(recipientCountry);
        return sb.toString();
    }

    public boolean isDelivered() {
        return status == ShipmentStatus.DELIVERED;
    }

    public boolean isInTransit() {
        return status != null && status.isActive();
    }

    public boolean hasException() {
        return status == ShipmentStatus.EXCEPTION || status == ShipmentStatus.DELAYED;
    }

    public boolean isMultiPackage() {
        return packageCount != null && packageCount > 1;
    }
}