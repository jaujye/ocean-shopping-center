package com.ocean.shopping.model.entity.enums;

/**
 * Payment provider enumeration
 */
public enum PaymentProvider {
    STRIPE("stripe"),
    PAYPAL("paypal"),
    CASH("cash"),
    BANK_TRANSFER("bank_transfer");

    private final String value;

    PaymentProvider(String value) {
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