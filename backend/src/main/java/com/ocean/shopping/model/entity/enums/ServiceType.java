package com.ocean.shopping.model.entity.enums;

/**
 * Shipping service types
 */
public enum ServiceType {
    STANDARD("Standard", "Standard ground delivery", false),
    EXPRESS("Express", "Express delivery (1-2 business days)", true),
    OVERNIGHT("Overnight", "Next business day delivery", true),
    TWO_DAY("Two Day", "Two business day delivery", true),
    GROUND("Ground", "Ground delivery (3-5 business days)", false),
    INTERNATIONAL("International", "International shipping", false),
    SAME_DAY("Same Day", "Same day delivery", true);
    
    private final String displayName;
    private final String description;
    private final boolean expedited;
    
    ServiceType(String displayName, String description, boolean expedited) {
        this.displayName = displayName;
        this.description = description;
        this.expedited = expedited;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isExpedited() {
        return expedited;
    }
}