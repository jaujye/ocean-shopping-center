package com.ocean.shopping.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Product creation/update request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product creation or update request")
public class ProductRequest {

    @Schema(description = "Store ID that owns the product", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Store ID is required")
    private UUID storeId;

    @Schema(description = "Product category ID", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID categoryId;

    @Schema(description = "Product name", example = "Wireless Bluetooth Headphones", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product name is required")
    @Size(max = 255, message = "Product name cannot exceed 255 characters")
    private String name;

    @Schema(description = "Product URL slug", example = "wireless-bluetooth-headphones", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Product slug is required")
    @Size(max = 255, message = "Product slug cannot exceed 255 characters")
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain only lowercase letters, numbers, and hyphens")
    private String slug;

    @Schema(description = "Detailed product description", example = "High-quality wireless Bluetooth headphones with noise cancellation")
    private String description;

    @Schema(description = "Short product description", example = "Premium wireless headphones with excellent sound quality")
    private String shortDescription;

    @Schema(description = "Stock Keeping Unit (SKU)", example = "WBH-001")
    @Size(max = 100, message = "SKU cannot exceed 100 characters")
    private String sku;

    @Schema(description = "Product price", example = "99.99", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have maximum 8 integer digits and 2 decimal places")
    private BigDecimal price;

    @Schema(description = "Compare at price (original price)", example = "149.99")
    @DecimalMin(value = "0.0", inclusive = false, message = "Compare at price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Compare at price must have maximum 8 integer digits and 2 decimal places")
    private BigDecimal compareAtPrice;

    @Schema(description = "Cost price", example = "50.00")
    @DecimalMin(value = "0.0", inclusive = false, message = "Cost must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Cost must have maximum 8 integer digits and 2 decimal places")
    private BigDecimal cost;

    @Schema(description = "Track inventory for this product", example = "true")
    @Builder.Default
    private Boolean trackInventory = true;

    @Schema(description = "Available inventory quantity", example = "100")
    @PositiveOrZero(message = "Inventory quantity must be positive or zero")
    private Integer inventoryQuantity = 0;

    @Schema(description = "Low stock threshold", example = "10")
    @PositiveOrZero(message = "Low stock threshold must be positive or zero")
    private Integer lowStockThreshold = 10;

    @Schema(description = "Product weight in kg", example = "0.5")
    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be greater than 0")
    @Digits(integer = 6, fraction = 2, message = "Weight must have maximum 6 integer digits and 2 decimal places")
    private BigDecimal weight;

    @Schema(description = "Product length in cm", example = "20.0")
    @DecimalMin(value = "0.0", inclusive = false, message = "Length must be greater than 0")
    @Digits(integer = 6, fraction = 2, message = "Length must have maximum 6 integer digits and 2 decimal places")
    private BigDecimal dimensionsLength;

    @Schema(description = "Product width in cm", example = "15.0")
    @DecimalMin(value = "0.0", inclusive = false, message = "Width must be greater than 0")
    @Digits(integer = 6, fraction = 2, message = "Width must have maximum 6 integer digits and 2 decimal places")
    private BigDecimal dimensionsWidth;

    @Schema(description = "Product height in cm", example = "8.0")
    @DecimalMin(value = "0.0", inclusive = false, message = "Height must be greater than 0")
    @Digits(integer = 6, fraction = 2, message = "Height must have maximum 6 integer digits and 2 decimal places")
    private BigDecimal dimensionsHeight;

    @Schema(description = "Product requires shipping", example = "true")
    @Builder.Default
    private Boolean requiresShipping = true;

    @Schema(description = "Product is digital", example = "false")
    @Builder.Default
    private Boolean isDigital = false;

    @Schema(description = "Product is featured", example = "false")
    @Builder.Default
    private Boolean isFeatured = false;

    @Schema(description = "Product is active and visible to customers", example = "true")
    @Builder.Default
    private Boolean isActive = true;

    @Schema(description = "SEO title", example = "Buy Wireless Bluetooth Headphones - Best Quality")
    @Size(max = 255, message = "SEO title cannot exceed 255 characters")
    private String seoTitle;

    @Schema(description = "SEO description", example = "Premium wireless Bluetooth headphones with exceptional sound quality and noise cancellation technology")
    private String seoDescription;

    @Schema(description = "Product image URLs")
    private List<ProductImageRequest> images;

    /**
     * Product image request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Product image request")
    public static class ProductImageRequest {

        @Schema(description = "Image URL", example = "https://example.com/image.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Image URL is required")
        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        private String url;

        @Schema(description = "Alt text for accessibility", example = "Wireless Bluetooth headphones in black color")
        @Size(max = 255, message = "Alt text cannot exceed 255 characters")
        private String altText;

        @Schema(description = "Sort order for display", example = "1")
        @Builder.Default
        private Integer sortOrder = 0;

        @Schema(description = "Is primary image", example = "true")
        @Builder.Default
        private Boolean isPrimary = false;
    }
}