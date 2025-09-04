package com.ocean.shopping.dto.order;

import com.ocean.shopping.model.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating order status
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateRequest {

    @NotNull(message = "Order status is required")
    private OrderStatus status;

    @Size(max = 1000)
    private String internalNotes;

    private String trackingNumber;

    // Flag to indicate if customer should be notified
    private boolean notifyCustomer = true;
}