package com.ocean.shopping.dto.order;

import com.ocean.shopping.model.entity.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

/**
 * DTO for order summary response (for order lists)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderSummaryResponse {

    private String id;
    private String orderNumber;
    private OrderStatus status;
    private String customerEmail;
    private String customerName; // Full name for display
    
    // Pricing summary
    private BigDecimal totalAmount;
    private String currency;
    private int itemCount;
    
    // Store information
    private String storeId;
    private String storeName;
    
    // Timestamps
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    
    // Quick status indicators
    private boolean requiresAttention;
    private String statusDisplayText;
    
    // Shipping info
    private String shippingCity;
    private String shippingCountry;
    private String trackingNumber;

    // Helper methods
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