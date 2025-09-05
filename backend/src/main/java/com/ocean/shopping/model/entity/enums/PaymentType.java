package com.ocean.shopping.model.entity.enums;

/**
 * Payment type enumeration
 */
public enum PaymentType {
    CARD("card"),
    BANK_TRANSFER("bank_transfer"),
    DIGITAL_WALLET("digital_wallet"),
    CRYPTO("crypto"),
    CASH("cash"),
    CHECK("check");

    private final String value;

    PaymentType(String value) {
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