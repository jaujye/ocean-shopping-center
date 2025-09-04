package com.ocean.shopping.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for checkout response containing order confirmation details
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutResponse {

    private Long orderId;
    private String orderNumber;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentIntentId;
    private String customerEmail;
    
    // Payment confirmation details
    private String paymentStatus;
    private String paymentMethodLast4;
    private String paymentMethodBrand;
    
    // Checkout success indicators
    private boolean success;
    private String message;
    
    // Next steps for customer
    private String redirectUrl;
    private boolean requiresPaymentAction;
    private String paymentActionUrl;

    public static CheckoutResponse success(Long orderId, String orderNumber, 
                                         BigDecimal totalAmount, String currency,
                                         String paymentIntentId, String customerEmail) {
        return CheckoutResponse.builder()
                .orderId(orderId)
                .orderNumber(orderNumber)
                .totalAmount(totalAmount)
                .currency(currency)
                .paymentIntentId(paymentIntentId)
                .customerEmail(customerEmail)
                .success(true)
                .message("Order placed successfully")
                .build();
    }

    public static CheckoutResponse error(String message) {
        return CheckoutResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}