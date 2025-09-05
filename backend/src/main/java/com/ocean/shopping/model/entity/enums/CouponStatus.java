package com.ocean.shopping.model.entity.enums;

/**
 * Coupon status enumeration
 */
public enum CouponStatus {
    ACTIVE("active"),
    INACTIVE("inactive"),
    EXPIRED("expired"),
    USED_UP("used_up");

    private final String value;

    CouponStatus(String value) {
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