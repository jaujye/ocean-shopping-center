package com.ocean.shopping.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Cart entity with Redis-based session management for shopping cart functionality.
 * Supports both persistent (authenticated users) and session-based (guest users) carts.
 */
@Entity
@Table(name = "carts", indexes = {
    @Index(name = "idx_carts_user_id", columnList = "user_id"),
    @Index(name = "idx_carts_session_id", columnList = "session_id"),
    @Index(name = "idx_carts_status", columnList = "status"),
    @Index(name = "idx_carts_updated_at", columnList = "updated_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "session_id", length = 255)
    private String sessionId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CartStatus status = CartStatus.ACTIVE;

    @Column(name = "subtotal", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "tax_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(name = "shipping_fee", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "total", precision = 10, scale = 2, nullable = false)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "currency", length = 3, nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(name = "applied_coupon_code")
    private String appliedCouponCode;

    @Column(name = "coupon_discount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscount = BigDecimal.ZERO;

    @Column(name = "expires_at")
    private ZonedDateTime expiresAt;

    @Column(name = "merged_from_session")
    private String mergedFromSession;


    /**
     * Cart status enum for tracking cart lifecycle
     */
    public enum CartStatus {
        ACTIVE,         // Active cart with items
        ABANDONED,      // Cart left inactive for extended time
        CONVERTED,      // Cart converted to order
        MERGED,         // Guest cart merged with user cart
        EXPIRED         // Cart expired and cleared
    }

    /**
     * Add an item to the cart
     */
    public void addItem(CartItem item) {
        if (items == null) {
            items = new ArrayList<>();
        }
        items.add(item);
        item.setCart(this);
        recalculateTotals();
    }

    /**
     * Remove an item from the cart
     */
    public void removeItem(CartItem item) {
        if (items != null) {
            items.remove(item);
            item.setCart(null);
            recalculateTotals();
        }
    }

    /**
     * Clear all items from the cart
     */
    public void clearItems() {
        if (items != null) {
            items.forEach(item -> item.setCart(null));
            items.clear();
            recalculateTotals();
        }
    }

    /**
     * Get total number of items in cart
     */
    public int getTotalItems() {
        return items != null ? items.stream().mapToInt(CartItem::getQuantity).sum() : 0;
    }

    /**
     * Get total number of unique products in cart
     */
    public int getUniqueItemsCount() {
        return items != null ? items.size() : 0;
    }

    /**
     * Check if cart is empty
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Check if cart belongs to a user (authenticated) or session (guest)
     */
    public boolean isUserCart() {
        return user != null;
    }

    /**
     * Check if cart is session-based (guest user)
     */
    public boolean isSessionCart() {
        return user == null && sessionId != null;
    }

    /**
     * Check if cart has expired
     */
    public boolean isExpired() {
        return expiresAt != null && ZonedDateTime.now().isAfter(expiresAt);
    }

    /**
     * Extend cart expiration time
     */
    public void extendExpiration(int days) {
        this.expiresAt = ZonedDateTime.now().plusDays(days);
    }

    /**
     * Recalculate cart totals based on items
     */
    public void recalculateTotals() {
        if (items == null || items.isEmpty()) {
            subtotal = BigDecimal.ZERO;
            total = BigDecimal.ZERO;
            return;
        }

        subtotal = items.stream()
            .map(CartItem::getItemTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate total: subtotal + tax + shipping - discount - coupon discount
        total = subtotal
            .add(taxAmount)
            .add(shippingFee)
            .subtract(discountAmount)
            .subtract(couponDiscount);

        // Ensure total is not negative
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }
    }

    /**
     * Apply coupon discount
     */
    public void applyCoupon(String couponCode, BigDecimal discount) {
        this.appliedCouponCode = couponCode;
        this.couponDiscount = discount;
        recalculateTotals();
    }

    /**
     * Remove applied coupon
     */
    public void removeCoupon() {
        this.appliedCouponCode = null;
        this.couponDiscount = BigDecimal.ZERO;
        recalculateTotals();
    }

    /**
     * Mark cart as abandoned
     */
    public void markAsAbandoned() {
        this.status = CartStatus.ABANDONED;
    }

    /**
     * Mark cart as converted to order
     */
    public void markAsConverted() {
        this.status = CartStatus.CONVERTED;
    }

    /**
     * Mark cart as merged from another cart
     */
    public void markAsMerged(String fromSessionId) {
        this.status = CartStatus.MERGED;
        this.mergedFromSession = fromSessionId;
    }

    @PrePersist
    @PreUpdate
    private void updateTotals() {
        recalculateTotals();
    }
}