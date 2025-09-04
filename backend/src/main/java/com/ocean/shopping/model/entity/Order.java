package com.ocean.shopping.model.entity;

import com.ocean.shopping.model.entity.enums.OrderStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Order entity with comprehensive tracking
 */
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_order_number", columnList = "order_number"),
    @Index(name = "idx_orders_user_id", columnList = "user_id"),
    @Index(name = "idx_orders_store_id", columnList = "store_id"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_customer_email", columnList = "customer_email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(name = "order_number", unique = true, nullable = false)
    @NotBlank(message = "Order number is required")
    @Size(max = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    @NotNull(message = "Store is required")
    private Store store;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "customer_email", nullable = false)
    @NotBlank(message = "Customer email is required")
    @Size(max = 255)
    private String customerEmail;

    @Column(name = "customer_phone")
    @Size(max = 20)
    private String customerPhone;

    // Billing address
    @Column(name = "billing_first_name", nullable = false)
    @NotBlank(message = "Billing first name is required")
    @Size(max = 100)
    private String billingFirstName;

    @Column(name = "billing_last_name", nullable = false)
    @NotBlank(message = "Billing last name is required")
    @Size(max = 100)
    private String billingLastName;

    @Column(name = "billing_address_line_1", nullable = false)
    @NotBlank(message = "Billing address is required")
    @Size(max = 255)
    private String billingAddressLine1;

    @Column(name = "billing_city", nullable = false)
    @NotBlank(message = "Billing city is required")
    @Size(max = 100)
    private String billingCity;

    @Column(name = "billing_postal_code", nullable = false)
    @NotBlank(message = "Billing postal code is required")
    @Size(max = 20)
    private String billingPostalCode;

    @Column(name = "billing_country", nullable = false)
    @NotBlank(message = "Billing country is required")
    @Size(min = 2, max = 2)
    private String billingCountry;

    // Shipping address
    @Column(name = "shipping_first_name", nullable = false)
    @NotBlank(message = "Shipping first name is required")
    @Size(max = 100)
    private String shippingFirstName;

    @Column(name = "shipping_last_name", nullable = false)
    @NotBlank(message = "Shipping last name is required")
    @Size(max = 100)
    private String shippingLastName;

    @Column(name = "shipping_address_line_1", nullable = false)
    @NotBlank(message = "Shipping address is required")
    @Size(max = 255)
    private String shippingAddressLine1;

    @Column(name = "shipping_city", nullable = false)
    @NotBlank(message = "Shipping city is required")
    @Size(max = 100)
    private String shippingCity;

    @Column(name = "shipping_postal_code", nullable = false)
    @NotBlank(message = "Shipping postal code is required")
    @Size(max = 20)
    private String shippingPostalCode;

    @Column(name = "shipping_country", nullable = false)
    @NotBlank(message = "Shipping country is required")
    @Size(min = 2, max = 2)
    private String shippingCountry;

    // Pricing
    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Subtotal is required")
    @PositiveOrZero(message = "Subtotal must be positive or zero")
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    @PositiveOrZero(message = "Tax amount must be positive or zero")
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_amount", nullable = false, precision = 10, scale = 2)
    @PositiveOrZero(message = "Shipping amount must be positive or zero")
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    @PositiveOrZero(message = "Discount amount must be positive or zero")
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    @NotNull(message = "Total amount is required")
    @PositiveOrZero(message = "Total amount must be positive or zero")
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false)
    @Size(min = 3, max = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    // Timestamps
    @Column(name = "confirmed_at")
    private ZonedDateTime confirmedAt;

    @Column(name = "shipped_at")
    private ZonedDateTime shippedAt;

    @Column(name = "delivered_at")
    private ZonedDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private ZonedDateTime cancelledAt;

    // Relationships
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderCoupon> appliedCoupons;

    // Helper methods
    public String getBillingFullName() {
        return billingFirstName + " " + billingLastName;
    }

    public String getShippingFullName() {
        return shippingFirstName + " " + shippingLastName;
    }

    public boolean isPending() {
        return status == OrderStatus.PENDING;
    }

    public boolean isConfirmed() {
        return status == OrderStatus.CONFIRMED;
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
}