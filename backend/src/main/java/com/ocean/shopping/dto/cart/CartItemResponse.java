package com.ocean.shopping.dto.cart;

import com.ocean.shopping.dto.product.ProductResponse;
import com.ocean.shopping.model.entity.CartItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Cart item response DTO for API responses
 */
@Data
@Builder
public class CartItemResponse {

    private UUID id;
    private UUID cartId;
    private ProductResponse product;
    private UUID productVariantId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal itemTotal;
    private BigDecimal originalUnitPrice;
    private BigDecimal discountAmount;
    private BigDecimal discountPercentage;
    private Map<String, String> selectedOptions;
    private String notes;
    private Boolean isGift;
    private String giftMessage;
    private Boolean savedForLater;
    private Boolean hasDiscount;
    private BigDecimal savingsAmount;
    private Integer maxAvailableQuantity;
    private Boolean isValid;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    /**
     * Convert CartItem entity to response DTO
     */
    public static CartItemResponse fromEntity(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .cartId(cartItem.getCart().getId())
                .product(ProductResponse.summaryFromEntity(cartItem.getProduct()))
                .productVariantId(cartItem.getProductVariant() != null ? 
                                cartItem.getProductVariant().getId() : null)
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .itemTotal(cartItem.getItemTotal())
                .originalUnitPrice(cartItem.getOriginalUnitPrice())
                .discountAmount(cartItem.getDiscountAmount())
                .discountPercentage(cartItem.getDiscountPercentage())
                .selectedOptions(cartItem.getSelectedOptions())
                .notes(cartItem.getNotes())
                .isGift(cartItem.getIsGift())
                .giftMessage(cartItem.getGiftMessage())
                .savedForLater(cartItem.getSavedForLater())
                .hasDiscount(cartItem.hasDiscount())
                .savingsAmount(cartItem.getSavingsAmount())
                .maxAvailableQuantity(cartItem.getMaxAvailableQuantity())
                .isValid(cartItem.isValid())
                .createdAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }

    /**
     * Convert CartItem entity to summary response (minimal data)
     */
    public static CartItemResponse summaryFromEntity(CartItem cartItem) {
        if (cartItem == null) {
            return null;
        }

        return CartItemResponse.builder()
                .id(cartItem.getId())
                .product(ProductResponse.summaryFromEntity(cartItem.getProduct()))
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .itemTotal(cartItem.getItemTotal())
                .hasDiscount(cartItem.hasDiscount())
                .isValid(cartItem.isValid())
                .build();
    }
}