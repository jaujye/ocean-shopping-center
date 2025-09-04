package com.ocean.shopping.model.entity.enums;

/**
 * Store status enumeration matching database store_status_type
 */
public enum StoreStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    SUSPENDED("suspended"),
    PENDING_APPROVAL("pending_approval");

    private final String value;

    StoreStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}