package com.ocean.shopping.model.entity.enums;

/**
 * Shipment status tracking
 */
public enum ShipmentStatus {
    PENDING("Pending", "Shipment created but not yet picked up"),
    PICKED_UP("Picked Up", "Package has been picked up by carrier"),
    IN_TRANSIT("In Transit", "Package is in transit to destination"),
    OUT_FOR_DELIVERY("Out for Delivery", "Package is out for delivery"),
    DELIVERED("Delivered", "Package has been delivered"),
    DELAYED("Delayed", "Shipment is delayed"),
    EXCEPTION("Exception", "Delivery exception occurred"),
    RETURNED("Returned", "Package has been returned to sender"),
    CANCELLED("Cancelled", "Shipment has been cancelled");
    
    private final String displayName;
    private final String description;
    
    ShipmentStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }
    
    public boolean isActive() {
        return this == PICKED_UP || this == IN_TRANSIT || this == OUT_FOR_DELIVERY;
    }
}