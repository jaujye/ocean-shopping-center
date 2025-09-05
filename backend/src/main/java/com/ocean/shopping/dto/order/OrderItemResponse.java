package com.ocean.shopping.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for order item response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String currency;

    // Product variant information
    private String variantName;
    private String variantSku;

    // Store information for marketplace scenarios
    private Long storeId;
    private String storeName;
}