package com.ocean.shopping.model.entity.enums;

/**
 * Invoice status enumeration
 */
public enum InvoiceStatus {
    DRAFT("draft"),
    GENERATED("generated"),
    SENT("sent"),
    PAID("paid"),
    VOID("void");

    private final String value;

    InvoiceStatus(String value) {
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