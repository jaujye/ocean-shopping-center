package com.ocean.shopping.model.entity.enums;

/**
 * Order status enumeration matching database order_status_type
 */
public enum OrderStatus {
    PENDING("pending"),
    CONFIRMED("confirmed"),
    PROCESSING("processing"),
    SHIPPED("shipped"),
    DELIVERED("delivered"),
    CANCELLED("cancelled"),
    RETURNED("returned");

    private final String value;

    OrderStatus(String value) {
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