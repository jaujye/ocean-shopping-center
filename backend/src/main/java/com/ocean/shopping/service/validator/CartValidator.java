package com.ocean.shopping.service.validator;

import com.ocean.shopping.model.entity.Product;
import com.ocean.shopping.model.entity.ProductVariant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized validation logic for cart operations.
 * Extracted from CartItem.isValid() and CartService.validateCart() to follow Single Responsibility Principle.
 */
@Component
@Slf4j
public class CartValidator {

    /**
     * Validation result containing issues and recommended quantity
     */
    public static class ValidationResult {
        private final List<String> issues;
        private final Integer recommendedQuantity;

        public ValidationResult(List<String> issues, Integer recommendedQuantity) {
            this.issues = issues;
            this.recommendedQuantity = recommendedQuantity;
        }

        public List<String> getIssues() {
            return issues;
        }

        public Integer getRecommendedQuantity() {
            return recommendedQuantity;
        }

        public boolean hasIssues() {
            return !issues.isEmpty();
        }

        public boolean isValid() {
            return issues.isEmpty();
        }
    }

    /**
     * Validate if a product is available for purchase
     */
    public boolean isProductAvailable(Product product) {
        if (product == null) {
            log.warn("Product validation failed: product is null");
            return false;
        }

        if (!product.isActive()) {
            log.debug("Product {} is not active", product.getId());
            return false;
        }

        return true;
    }

    /**
     * Validate cart item with product and quantity
     */
    public ValidationResult validateCartItem(Product product, int quantity) {
        return validateCartItem(product, null, quantity);
    }

    /**
     * Validate cart item with product, variant, and quantity
     */
    public ValidationResult validateCartItem(Product product, ProductVariant variant, int quantity) {
        List<String> issues = new ArrayList<>();
        Integer recommendedQuantity = null;

        // Check if product exists
        if (product == null) {
            issues.add("Product not found");
            return new ValidationResult(issues, null);
        }

        // Check if product is active
        if (!product.isActive()) {
            issues.add("Product '" + product.getName() + "' is no longer available");
            return new ValidationResult(issues, null);
        }

        // Check inventory if tracking is enabled
        if (product.getTrackInventory()) {
            int availableStock = getAvailableStock(product, variant);

            if (availableStock == 0) {
                issues.add("Product '" + product.getName() + "' is out of stock");
                recommendedQuantity = 0;
            } else if (quantity > availableStock) {
                issues.add("Insufficient inventory for '" + product.getName() +
                          "'. Available: " + availableStock + ", Requested: " + quantity);
                recommendedQuantity = availableStock;
            }
        }

        return new ValidationResult(issues, recommendedQuantity);
    }

    /**
     * Check if requested quantity is available
     */
    public boolean hasAvailableStock(Product product, ProductVariant variant, int requestedQuantity) {
        if (!product.getTrackInventory()) {
            return true;
        }

        int availableStock = getAvailableStock(product, variant);
        return requestedQuantity <= availableStock;
    }

    /**
     * Get available stock quantity
     */
    public int getAvailableStock(Product product, ProductVariant variant) {
        if (variant != null && variant.getInventoryQuantity() != null) {
            return variant.getInventoryQuantity();
        }

        if (product.getInventoryQuantity() != null) {
            return product.getInventoryQuantity();
        }

        return 0;
    }

    /**
     * Get maximum purchasable quantity
     */
    public int getMaxPurchasableQuantity(Product product, ProductVariant variant) {
        if (!product.getTrackInventory()) {
            return Integer.MAX_VALUE;
        }

        return getAvailableStock(product, variant);
    }

    /**
     * Validate product availability and stock for a specific quantity
     * @return List of validation issues (empty if valid)
     */
    public List<String> validateProductAvailability(Product product, int quantity) {
        List<String> issues = new ArrayList<>();

        if (product == null) {
            issues.add("Product not found");
            return issues;
        }

        if (!product.isActive()) {
            issues.add("Product is not available");
            return issues;
        }

        if (product.getTrackInventory() && quantity > product.getInventoryQuantity()) {
            issues.add("Insufficient inventory. Available: " + product.getInventoryQuantity());
        }

        return issues;
    }
}
