package com.ocean.shopping.model.entity.enums;

/**
 * Supported shipping carriers
 */
public enum CarrierType {
    DHL("DHL Express", "dhl"),
    FEDEX("FedEx", "fedex"),
    UPS("UPS", "ups"),
    USPS("United States Postal Service", "usps");
    
    private final String displayName;
    private final String code;
    
    CarrierType(String displayName, String code) {
        this.displayName = displayName;
        this.code = code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public static CarrierType fromCode(String code) {
        for (CarrierType carrier : values()) {
            if (carrier.code.equalsIgnoreCase(code)) {
                return carrier;
            }
        }
        throw new IllegalArgumentException("Unknown carrier code: " + code);
    }
}