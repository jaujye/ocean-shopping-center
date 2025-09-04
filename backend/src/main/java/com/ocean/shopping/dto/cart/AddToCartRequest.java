package com.ocean.shopping.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for adding items to cart
 */
@Data
public class AddToCartRequest {

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    private UUID productVariantId;

    private Map<String, String> selectedOptions;

    private String notes;

    private Boolean isGift = false;

    private String giftMessage;
}