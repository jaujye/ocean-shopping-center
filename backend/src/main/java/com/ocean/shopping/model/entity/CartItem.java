package com.ocean.shopping.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;

import java.math.BigDecimal;
import java.util.Map;

/**
 * CartItem entity representing individual items in a shopping cart.
 * Supports product variants, custom options, and pricing calculations.
 */
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_items_cart_id", columnList = "cart_id"),
    @Index(name = "idx_cart_items_product_id", columnList = "product_id"),
    @Index(name = "idx_cart_items_product_variant_id", columnList = "product_variant_id")
})
@RedisHash("cart_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    @JsonIgnoreProperties({"items"})
    @NotNull(message = "Cart is required")
    private Cart cart;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id")
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "unit_price", precision = 10, scale = 2, nullable = false)
    @NotNull(message = "Unit price is required")
    @PositiveOrZero(message = "Unit price must be positive or zero")
    private BigDecimal unitPrice;

    @Column(name = "item_total", precision = 10, scale = 2, nullable = false)
    @PositiveOrZero(message = "Item total must be positive or zero")
    @Builder.Default
    private BigDecimal itemTotal = BigDecimal.ZERO;

    @Column(name = "original_unit_price", precision = 10, scale = 2)
    @PositiveOrZero(message = "Original unit price must be positive or zero")
    private BigDecimal originalUnitPrice;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(name = "cart_item_options", 
                    joinColumns = @JoinColumn(name = "cart_item_id"),
                    indexes = @Index(name = "idx_cart_item_options_cart_item_id", columnList = "cart_item_id"))
    @MapKeyColumn(name = "option_name")
    @Column(name = "option_value")
    private Map<String, String> selectedOptions;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_gift", nullable = false)
    @Builder.Default
    private Boolean isGift = false;

    @Column(name = "gift_message", columnDefinition = "TEXT")
    private String giftMessage;

    @Column(name = "saved_for_later", nullable = false)
    @Builder.Default
    private Boolean savedForLater = false;

    /**
     * Calculate the total price for this item (quantity × unit price - discount)
     */
    public BigDecimal calculateItemTotal() {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return subtotal.subtract(discountAmount != null ? discountAmount : BigDecimal.ZERO);
    }

    /**
     * Get the effective price per unit (considering discounts)
     */
    public BigDecimal getEffectiveUnitPrice() {
        if (quantity == 0) return BigDecimal.ZERO;
        return itemTotal.divide(BigDecimal.valueOf(quantity), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Calculate savings amount if there's an original price
     */
    public BigDecimal getSavingsAmount() {
        if (originalUnitPrice == null || originalUnitPrice.equals(unitPrice)) {
            return BigDecimal.ZERO;
        }
        BigDecimal originalTotal = originalUnitPrice.multiply(BigDecimal.valueOf(quantity));
        return originalTotal.subtract(itemTotal);
    }

    /**
     * Check if this item has any discounts applied
     */
    public boolean hasDiscount() {
        return (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0) ||
               (discountPercentage != null && discountPercentage.compareTo(BigDecimal.ZERO) > 0) ||
               (originalUnitPrice != null && originalUnitPrice.compareTo(unitPrice) > 0);
    }

    /**
     * Check if item has selected options/variants
     */
    public boolean hasOptions() {
        return selectedOptions != null && !selectedOptions.isEmpty();
    }

    /**
     * Get option value by name
     */
    public String getOption(String optionName) {
        return selectedOptions != null ? selectedOptions.get(optionName) : null;
    }

    /**
     * Check if the item is valid (has valid product and stock)
     */
    public boolean isValid() {
        if (product == null) return false;
        
        // Check if product is active
        if (!product.getIsActive()) return false;
        
        // Check inventory if tracking is enabled
        if (product.getTrackInventory()) {
            int availableStock = product.getInventoryQuantity();
            if (productVariant != null) {
                availableStock = productVariant.getInventoryQuantity();
            }
            return quantity <= availableStock;
        }
        
        return true;
    }

    /**
     * Get maximum available quantity for this item
     */
    public int getMaxAvailableQuantity() {
        if (product == null || !product.getTrackInventory()) {
            return Integer.MAX_VALUE;
        }
        
        if (productVariant != null) {
            return productVariant.getInventoryQuantity();
        }
        
        return product.getInventoryQuantity();
    }

    /**
     * Update quantity and recalculate totals
     */
    public void updateQuantity(int newQuantity) {
        if (newQuantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }
        
        int maxQuantity = getMaxAvailableQuantity();
        if (newQuantity > maxQuantity) {
            throw new IllegalArgumentException("Quantity exceeds available stock: " + maxQuantity);
        }
        
        this.quantity = newQuantity;
        recalculateItemTotal();
    }

    /**
     * Apply discount amount
     */
    public void applyDiscountAmount(BigDecimal discount) {
        this.discountAmount = discount;
        recalculateItemTotal();
    }

    /**
     * Apply discount percentage
     */
    public void applyDiscountPercentage(BigDecimal percentage) {
        this.discountPercentage = percentage;
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.discountAmount = subtotal.multiply(percentage).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        recalculateItemTotal();
    }

    /**
     * Remove all discounts
     */
    public void removeDiscount() {
        this.discountAmount = BigDecimal.ZERO;
        this.discountPercentage = BigDecimal.ZERO;
        recalculateItemTotal();
    }

    /**
     * Mark item as saved for later
     */
    public void saveForLater() {
        this.savedForLater = true;
    }

    /**
     * Move item back to active cart
     */
    public void moveToCart() {
        this.savedForLater = false;
    }

    /**
     * Set as gift with message
     */
    public void setAsGift(String message) {
        this.isGift = true;
        this.giftMessage = message;
    }

    /**
     * Remove gift status
     */
    public void removeGift() {
        this.isGift = false;
        this.giftMessage = null;
    }

    /**
     * Private method to recalculate item total
     */
    private void recalculateItemTotal() {
        this.itemTotal = calculateItemTotal();
        
        // Update cart totals if cart is available
        if (cart != null) {
            cart.recalculateTotals();
        }
    }

    @PrePersist
    @PreUpdate
    private void updateItemTotal() {
        if (unitPrice != null && quantity != null) {
            this.itemTotal = calculateItemTotal();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CartItem cartItem = (CartItem) obj;
        
        // Consider items equal if they have the same product, variant, and options
        if (product == null || !product.equals(cartItem.product)) return false;
        
        if (productVariant != null ? !productVariant.equals(cartItem.productVariant) : cartItem.productVariant != null) {
            return false;
        }
        
        return selectedOptions != null ? selectedOptions.equals(cartItem.selectedOptions) : cartItem.selectedOptions == null;
    }

    @Override
    public int hashCode() {
        int result = product != null ? product.hashCode() : 0;
        result = 31 * result + (productVariant != null ? productVariant.hashCode() : 0);
        result = 31 * result + (selectedOptions != null ? selectedOptions.hashCode() : 0);
        return result;
    }
}