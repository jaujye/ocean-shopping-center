package com.ocean.shopping.dto.product;

import com.ocean.shopping.model.entity.Product;
import com.ocean.shopping.model.entity.ProductImage;
import com.ocean.shopping.model.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product information response")
public class ProductResponse {

    @Schema(description = "Product unique identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Store information")
    private StoreInfo store;

    @Schema(description = "Category information")
    private CategoryInfo category;

    @Schema(description = "Product name", example = "Wireless Bluetooth Headphones")
    private String name;

    @Schema(description = "Product URL slug", example = "wireless-bluetooth-headphones")
    private String slug;

    @Schema(description = "Product description", example = "High-quality wireless Bluetooth headphones with noise cancellation")
    private String description;

    @Schema(description = "Short product description", example = "Premium wireless headphones with excellent sound quality")
    private String shortDescription;

    @Schema(description = "Stock Keeping Unit (SKU)", example = "WBH-001")
    private String sku;

    @Schema(description = "Product price", example = "99.99")
    private BigDecimal price;

    @Schema(description = "Compare at price (original price)", example = "149.99")
    private BigDecimal compareAtPrice;

    @Schema(description = "Cost price", example = "50.00")
    private BigDecimal cost;

    @Schema(description = "Track inventory for this product", example = "true")
    private Boolean trackInventory;

    @Schema(description = "Available inventory quantity", example = "100")
    private Integer inventoryQuantity;

    @Schema(description = "Low stock threshold", example = "10")
    private Integer lowStockThreshold;

    @Schema(description = "Product weight in kg", example = "0.5")
    private BigDecimal weight;

    @Schema(description = "Product length in cm", example = "20.0")
    private BigDecimal dimensionsLength;

    @Schema(description = "Product width in cm", example = "15.0")
    private BigDecimal dimensionsWidth;

    @Schema(description = "Product height in cm", example = "8.0")
    private BigDecimal dimensionsHeight;

    @Schema(description = "Product requires shipping", example = "true")
    private Boolean requiresShipping;

    @Schema(description = "Product is digital", example = "false")
    private Boolean isDigital;

    @Schema(description = "Product is featured", example = "false")
    private Boolean isFeatured;

    @Schema(description = "Product is active and visible to customers", example = "true")
    private Boolean isActive;

    @Schema(description = "SEO title", example = "Buy Wireless Bluetooth Headphones - Best Quality")
    private String seoTitle;

    @Schema(description = "SEO description", example = "Premium wireless Bluetooth headphones with exceptional sound quality")
    private String seoDescription;

    @Schema(description = "Product images")
    private List<ProductImageResponse> images;

    @Schema(description = "Product creation timestamp", example = "2023-11-01T09:00:00Z")
    private ZonedDateTime createdAt;

    @Schema(description = "Product last update timestamp", example = "2023-12-01T10:30:00Z")
    private ZonedDateTime updatedAt;

    // Computed fields
    @Schema(description = "Product is in stock", example = "true")
    private Boolean inStock;

    @Schema(description = "Product is low on stock", example = "false")
    private Boolean lowStock;

    @Schema(description = "Product has discount", example = "true")
    private Boolean hasDiscount;

    @Schema(description = "Discount amount", example = "50.00")
    private BigDecimal discountAmount;

    @Schema(description = "Discount percentage", example = "33.33")
    private BigDecimal discountPercentage;

    @Schema(description = "Average rating", example = "4.5")
    private BigDecimal averageRating;

    @Schema(description = "Total reviews count", example = "150")
    private Integer reviewCount;

    /**
     * Store information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Store information")
    public static class StoreInfo {
        @Schema(description = "Store ID", example = "123e4567-e89b-12d3-a456-426614174001")
        private UUID id;

        @Schema(description = "Store name", example = "Tech Store")
        private String name;

        @Schema(description = "Store slug", example = "tech-store")
        private String slug;
    }

    /**
     * Category information DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Category information")
    public static class CategoryInfo {
        @Schema(description = "Category ID", example = "123e4567-e89b-12d3-a456-426614174002")
        private UUID id;

        @Schema(description = "Category name", example = "Electronics")
        private String name;

        @Schema(description = "Category slug", example = "electronics")
        private String slug;
    }

    /**
     * Product image response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Product image response")
    public static class ProductImageResponse {
        @Schema(description = "Image ID", example = "123e4567-e89b-12d3-a456-426614174003")
        private UUID id;

        @Schema(description = "Image URL", example = "https://example.com/image.jpg")
        private String url;

        @Schema(description = "Alt text for accessibility", example = "Wireless Bluetooth headphones in black color")
        private String altText;

        @Schema(description = "Sort order for display", example = "1")
        private Integer sortOrder;

        @Schema(description = "Is primary image", example = "true")
        private Boolean isPrimary;
    }

    /**
     * Create ProductResponse from Product entity
     */
    public static ProductResponse fromEntity(Product product) {
        ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .shortDescription(product.getShortDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .cost(product.getCost())
                .trackInventory(product.getTrackInventory())
                .inventoryQuantity(product.getInventoryQuantity())
                .lowStockThreshold(product.getLowStockThreshold())
                .weight(product.getWeight())
                .dimensionsLength(product.getDimensionsLength())
                .dimensionsWidth(product.getDimensionsWidth())
                .dimensionsHeight(product.getDimensionsHeight())
                .requiresShipping(product.getRequiresShipping())
                .isDigital(product.getIsDigital())
                .isFeatured(product.getIsFeatured())
                .isActive(product.getIsActive())
                .seoTitle(product.getSeoTitle())
                .seoDescription(product.getSeoDescription())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .inStock(product.isInStock())
                .lowStock(product.isLowStock())
                .hasDiscount(product.hasDiscount())
                .discountAmount(product.getDiscountAmount())
                .discountPercentage(product.getDiscountPercentage());

        // Store information
        if (product.getStore() != null) {
            builder.store(StoreInfo.builder()
                    .id(product.getStore().getId())
                    .name(product.getStore().getName())
                    .slug(product.getStore().getSlug())
                    .build());
        }

        // Category information
        if (product.getCategory() != null) {
            builder.category(CategoryInfo.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .slug(product.getCategory().getSlug())
                    .build());
        }

        // Product images
        if (product.getImages() != null) {
            List<ProductImageResponse> imageResponses = product.getImages().stream()
                    .map(image -> ProductImageResponse.builder()
                            .id(image.getId())
                            .url(image.getUrl())
                            .altText(image.getAltText())
                            .sortOrder(image.getSortOrder())
                            .isPrimary(image.getIsPrimary())
                            .build())
                    .collect(Collectors.toList());
            builder.images(imageResponses);
        }

        // Calculate review statistics
        if (product.getReviews() != null && !product.getReviews().isEmpty()) {
            List<Review> publishedReviews = product.getReviews().stream()
                    .filter(Review::getIsPublished)
                    .collect(Collectors.toList());

            builder.reviewCount(publishedReviews.size());

            if (!publishedReviews.isEmpty()) {
                double avgRating = publishedReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);
                builder.averageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            builder.reviewCount(0);
            builder.averageRating(BigDecimal.ZERO);
        }

        return builder.build();
    }

    /**
     * Create summary ProductResponse for list views (without detailed information)
     */
    public static ProductResponse summaryFromEntity(Product product) {
        ProductResponseBuilder builder = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .shortDescription(product.getShortDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .trackInventory(product.getTrackInventory())
                .inventoryQuantity(product.getInventoryQuantity())
                .isFeatured(product.getIsFeatured())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .inStock(product.isInStock())
                .lowStock(product.isLowStock())
                .hasDiscount(product.hasDiscount())
                .discountAmount(product.getDiscountAmount())
                .discountPercentage(product.getDiscountPercentage());

        // Store information
        if (product.getStore() != null) {
            builder.store(StoreInfo.builder()
                    .id(product.getStore().getId())
                    .name(product.getStore().getName())
                    .slug(product.getStore().getSlug())
                    .build());
        }

        // Category information
        if (product.getCategory() != null) {
            builder.category(CategoryInfo.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .slug(product.getCategory().getSlug())
                    .build());
        }

        // Primary image only
        if (product.getImages() != null) {
            product.getImages().stream()
                    .filter(ProductImage::getIsPrimary)
                    .findFirst()
                    .ifPresent(image -> {
                        ProductImageResponse imageResponse = ProductImageResponse.builder()
                                .id(image.getId())
                                .url(image.getUrl())
                                .altText(image.getAltText())
                                .sortOrder(image.getSortOrder())
                                .isPrimary(image.getIsPrimary())
                                .build();
                        builder.images(List.of(imageResponse));
                    });
        }

        // Calculate review statistics
        if (product.getReviews() != null && !product.getReviews().isEmpty()) {
            List<Review> publishedReviews = product.getReviews().stream()
                    .filter(Review::getIsPublished)
                    .collect(Collectors.toList());

            builder.reviewCount(publishedReviews.size());

            if (!publishedReviews.isEmpty()) {
                double avgRating = publishedReviews.stream()
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);
                builder.averageRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            builder.reviewCount(0);
            builder.averageRating(BigDecimal.ZERO);
        }

        return builder.build();
    }
}