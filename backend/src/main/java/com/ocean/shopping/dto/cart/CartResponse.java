package com.ocean.shopping.dto.cart;

import com.ocean.shopping.model.entity.Cart;
import com.ocean.shopping.model.entity.CartItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cart response DTO for API responses
 */
@Data
@Builder
public class CartResponse {

    private UUID id;
    private UUID userId;
    private String sessionId;
    private List<CartItemResponse> items;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private String currency;
    private String appliedCouponCode;
    private BigDecimal couponDiscount;
    private Integer totalItems;
    private Integer uniqueItemsCount;
    private Boolean isEmpty;
    private ZonedDateTime expiresAt;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;

    /**
     * Convert Cart entity to response DTO
     */
    public static CartResponse fromEntity(Cart cart) {
        if (cart == null) {
            return null;
        }

        // Filter active items (not saved for later)
        List<CartItemResponse> activeItems = cart.getItems().stream()
                .filter(item -> !item.getSavedForLater())
                .map(CartItemResponse::fromEntity)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .items(activeItems)
                .status(cart.getStatus().name())
                .subtotal(cart.getSubtotal())
                .taxAmount(cart.getTaxAmount())
                .shippingFee(cart.getShippingFee())
                .discountAmount(cart.getDiscountAmount())
                .total(cart.getTotal())
                .currency(cart.getCurrency())
                .appliedCouponCode(cart.getAppliedCouponCode())
                .couponDiscount(cart.getCouponDiscount())
                .totalItems(cart.getTotalItems())
                .uniqueItemsCount(cart.getUniqueItemsCount())
                .isEmpty(cart.isEmpty())
                .expiresAt(cart.getExpiresAt())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    /**
     * Convert Cart entity to summary response (minimal data)
     */
    public static CartResponse summaryFromEntity(Cart cart) {
        if (cart == null) {
            return null;
        }

        return CartResponse.builder()
                .id(cart.getId())
                .totalItems(cart.getTotalItems())
                .uniqueItemsCount(cart.getUniqueItemsCount())
                .subtotal(cart.getSubtotal())
                .total(cart.getTotal())
                .currency(cart.getCurrency())
                .isEmpty(cart.isEmpty())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}