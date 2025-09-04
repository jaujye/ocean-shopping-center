package com.ocean.shopping.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Product filter DTO for search and filtering operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Product filter criteria for search and listing")
public class ProductFilter {

    @Schema(description = "Search query for product name and description", example = "wireless headphones")
    private String query;

    @Schema(description = "Store ID to filter by", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID storeId;

    @Schema(description = "Category ID to filter by", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID categoryId;

    @Schema(description = "List of category IDs to filter by", example = "[\"123e4567-e89b-12d3-a456-426614174001\", \"123e4567-e89b-12d3-a456-426614174002\"]")
    private List<UUID> categoryIds;

    @Schema(description = "Minimum price filter", example = "10.00")
    @DecimalMin(value = "0.0", message = "Minimum price must be positive")
    private BigDecimal minPrice;

    @Schema(description = "Maximum price filter", example = "500.00")
    @DecimalMin(value = "0.0", message = "Maximum price must be positive")
    private BigDecimal maxPrice;

    @Schema(description = "Filter by in-stock products only", example = "true")
    private Boolean inStock;

    @Schema(description = "Filter by active products only", example = "true")
    @Builder.Default
    private Boolean active = true;

    @Schema(description = "Filter by featured products only", example = "false")
    private Boolean featured;

    @Schema(description = "Filter by digital products", example = "false")
    private Boolean digital;

    @Schema(description = "Filter by products with discount", example = "false")
    private Boolean hasDiscount;

    @Schema(description = "Minimum rating filter", example = "4.0")
    @DecimalMin(value = "1.0", message = "Minimum rating must be at least 1.0")
    private BigDecimal minRating;

    @Schema(description = "Tags to filter by", example = "[\"bestseller\", \"new-arrival\"]")
    private List<String> tags;

    // Sorting options
    @Schema(description = "Sort field", example = "name", allowableValues = {"name", "price", "createdAt", "updatedAt", "rating", "popularity"})
    @Builder.Default
    private String sortBy = "createdAt";

    @Schema(description = "Sort direction", example = "desc", allowableValues = {"asc", "desc"})
    @Builder.Default
    private String sortDirection = "desc";

    // Pagination
    @Schema(description = "Page number (0-based)", example = "0")
    @Min(value = 0, message = "Page number must be non-negative")
    @Builder.Default
    private Integer page = 0;

    @Schema(description = "Number of items per page", example = "20")
    @Min(value = 1, message = "Page size must be at least 1")
    @Builder.Default
    private Integer size = 20;

    /**
     * Validation and normalization methods
     */
    public void normalize() {
        // Ensure price range is valid
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal temp = minPrice;
            minPrice = maxPrice;
            maxPrice = temp;
        }

        // Normalize sort direction
        if (sortDirection != null) {
            sortDirection = sortDirection.toLowerCase();
            if (!"asc".equals(sortDirection) && !"desc".equals(sortDirection)) {
                sortDirection = "desc";
            }
        }

        // Validate sort field
        if (sortBy != null && !isValidSortField(sortBy)) {
            sortBy = "createdAt";
        }

        // Ensure page and size are valid
        if (page == null || page < 0) {
            page = 0;
        }
        if (size == null || size < 1) {
            size = 20;
        }
        if (size > 100) {
            size = 100; // Maximum page size
        }
    }

    private boolean isValidSortField(String field) {
        return List.of("name", "price", "createdAt", "updatedAt", "rating", "popularity").contains(field);
    }

    /**
     * Check if any filter is applied
     */
    public boolean hasFilters() {
        return query != null ||
                storeId != null ||
                categoryId != null ||
                (categoryIds != null && !categoryIds.isEmpty()) ||
                minPrice != null ||
                maxPrice != null ||
                inStock != null ||
                featured != null ||
                digital != null ||
                hasDiscount != null ||
                minRating != null ||
                (tags != null && !tags.isEmpty());
    }

    /**
     * Check if search query is present
     */
    public boolean hasSearchQuery() {
        return query != null && !query.trim().isEmpty();
    }

    /**
     * Check if price range filter is applied
     */
    public boolean hasPriceRange() {
        return minPrice != null || maxPrice != null;
    }

    /**
     * Check if category filter is applied
     */
    public boolean hasCategoryFilter() {
        return categoryId != null || (categoryIds != null && !categoryIds.isEmpty());
    }

    /**
     * Get effective category IDs list
     */
    public List<UUID> getEffectiveCategoryIds() {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            return categoryIds;
        }
        if (categoryId != null) {
            return List.of(categoryId);
        }
        return List.of();
    }

    /**
     * Create a filter for featured products
     */
    public static ProductFilter featuredProducts() {
        return ProductFilter.builder()
                .featured(true)
                .active(true)
                .sortBy("createdAt")
                .sortDirection("desc")
                .size(10)
                .build();
    }

    /**
     * Create a filter for best selling products (by rating)
     */
    public static ProductFilter bestSelling() {
        return ProductFilter.builder()
                .active(true)
                .sortBy("rating")
                .sortDirection("desc")
                .size(10)
                .build();
    }

    /**
     * Create a filter for new arrivals
     */
    public static ProductFilter newArrivals() {
        return ProductFilter.builder()
                .active(true)
                .sortBy("createdAt")
                .sortDirection("desc")
                .size(10)
                .build();
    }

    /**
     * Create a filter for products on sale
     */
    public static ProductFilter onSale() {
        return ProductFilter.builder()
                .hasDiscount(true)
                .active(true)
                .sortBy("createdAt")
                .sortDirection("desc")
                .size(20)
                .build();
    }
}