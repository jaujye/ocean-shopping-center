package com.ocean.shopping.model.entity.enums;

/**
 * Coupon type enumeration
 */
public enum CouponType {
    PERCENTAGE("percentage"),
    FIXED_AMOUNT("fixed_amount"),
    FREE_SHIPPING("free_shipping"),
    BUY_ONE_GET_ONE("buy_one_get_one");

    private final String value;

    CouponType(String value) {
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