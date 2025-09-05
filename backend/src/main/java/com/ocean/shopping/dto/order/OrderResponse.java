package com.ocean.shopping.dto.order;

import com.ocean.shopping.model.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * DTO for order response with comprehensive order information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String orderNumber;
    private OrderStatus status;
    private String customerEmail;
    private String customerPhone;

    // Billing address
    private String billingFirstName;
    private String billingLastName;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPostalCode;
    private String billingCountry;

    // Shipping address
    private String shippingFirstName;
    private String shippingLastName;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPostalCode;
    private String shippingCountry;

    // Pricing
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String currency;

    // Order items
    private List<OrderItemResponse> orderItems;

    // Store information
    private Long storeId;
    private String storeName;

    // Timestamps
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ZonedDateTime confirmedAt;
    private ZonedDateTime shippedAt;
    private ZonedDateTime deliveredAt;
    private ZonedDateTime cancelledAt;

    // Additional fields
    private String notes;
    private String internalNotes;
    private String trackingNumber;

    // Helper methods
    public String getBillingFullName() {
        return billingFirstName + " " + billingLastName;
    }

    public String getShippingFullName() {
        return shippingFirstName + " " + shippingLastName;
    }

    public String getBillingFullAddress() {
        StringBuilder address = new StringBuilder();
        address.append(billingAddressLine1);
        if (billingAddressLine2 != null && !billingAddressLine2.trim().isEmpty()) {
            address.append(", ").append(billingAddressLine2);
        }
        address.append(", ").append(billingCity);
        if (billingState != null && !billingState.trim().isEmpty()) {
            address.append(", ").append(billingState);
        }
        address.append(" ").append(billingPostalCode);
        address.append(", ").append(billingCountry);
        return address.toString();
    }

    public String getShippingFullAddress() {
        StringBuilder address = new StringBuilder();
        address.append(shippingAddressLine1);
        if (shippingAddressLine2 != null && !shippingAddressLine2.trim().isEmpty()) {
            address.append(", ").append(shippingAddressLine2);
        }
        address.append(", ").append(shippingCity);
        if (shippingState != null && !shippingState.trim().isEmpty()) {
            address.append(", ").append(shippingState);
        }
        address.append(" ").append(shippingPostalCode);
        address.append(", ").append(shippingCountry);
        return address.toString();
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
    }

    public boolean isProcessing() {
        return status == OrderStatus.PROCESSING;
    }

    public boolean isShipped() {
        return status == OrderStatus.SHIPPED;
    }

    public boolean isDelivered() {
        return status == OrderStatus.DELIVERED;
    }

    public boolean isCancelled() {
        return status == OrderStatus.CANCELLED;
    }

    public boolean isReturned() {
        return status == OrderStatus.RETURNED;
    }
}