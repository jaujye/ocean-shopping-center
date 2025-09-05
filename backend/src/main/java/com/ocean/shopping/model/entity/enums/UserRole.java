package com.ocean.shopping.model.entity.enums;

/**
 * User role enumeration matching database user_role_type
 */
public enum UserRole {
    CUSTOMER("customer"),
    STORE_OWNER("store_owner"),
    ADMINISTRATOR("administrator");

    private final String value;

    UserRole(String value) {
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