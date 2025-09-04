package com.ocean.shopping.controller;

import com.ocean.shopping.dto.product.ProductFilter;
import com.ocean.shopping.dto.product.ProductRequest;
import com.ocean.shopping.dto.product.ProductResponse;
import com.ocean.shopping.exception.ErrorResponse;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Product controller handling product CRUD operations, search, and recommendations
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Products", description = "Product management and catalog endpoints")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Get all products", 
               description = "Get all products with optional filtering, search, and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Products retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid filter parameters",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getAllProducts(
            @Parameter(description = "Search query for product name/description")
            @RequestParam(required = false) String query,
            @Parameter(description = "Store ID to filter by")
            @RequestParam(required = false) UUID storeId,
            @Parameter(description = "Category ID to filter by")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by in-stock products only")
            @RequestParam(required = false) Boolean inStock,
            @Parameter(description = "Filter by featured products only")
            @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Filter by digital products")
            @RequestParam(required = false) Boolean digital,
            @Parameter(description = "Filter by products with discount")
            @RequestParam(required = false) Boolean hasDiscount,
            @Parameter(description = "Minimum rating filter")
            @RequestParam(required = false) BigDecimal minRating,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Getting all products with filters - query: {}, storeId: {}, categoryId: {}", 
                  query, storeId, categoryId);

        ProductFilter filter = ProductFilter.builder()
                .query(query)
                .storeId(storeId)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStock(inStock)
                .featured(featured)
                .digital(digital)
                .hasDiscount(hasDiscount)
                .minRating(minRating)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<ProductResponse> products = productService.getAllProducts(filter);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get product by ID", 
               description = "Get detailed product information by product ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
                     content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(
            @Parameter(description = "Product ID") 
            @PathVariable UUID id) {
        
        log.debug("Getting product by ID: {}", id);
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Get product by slug", 
               description = "Get detailed product information by store ID and product slug")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product found",
                     content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/store/{storeId}/slug/{slug}")
    public ResponseEntity<ProductResponse> getProductBySlug(
            @Parameter(description = "Store ID") 
            @PathVariable UUID storeId,
            @Parameter(description = "Product slug") 
            @PathVariable String slug) {
        
        log.debug("Getting product by slug: {} for store: {}", slug, storeId);
        ProductResponse product = productService.getProductBySlug(slug, storeId);
        return ResponseEntity.ok(product);
    }

    @Operation(summary = "Search products", 
               description = "Search products with advanced filtering and full-text search")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @Parameter(description = "Search query", required = true)
            @RequestParam String q,
            @Parameter(description = "Store ID to filter by")
            @RequestParam(required = false) UUID storeId,
            @Parameter(description = "Category ID to filter by")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Filter by in-stock products only")
            @RequestParam(required = false) Boolean inStock,
            @Parameter(description = "Minimum rating filter")
            @RequestParam(required = false) BigDecimal minRating,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Searching products with query: {}", q);

        ProductFilter filter = ProductFilter.builder()
                .query(q)
                .storeId(storeId)
                .categoryId(categoryId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .inStock(inStock)
                .minRating(minRating)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<ProductResponse> products = productService.searchProducts(filter);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get featured products", 
               description = "Get featured products for homepage display")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Featured products retrieved successfully")
    })
    @GetMapping("/featured")
    public ResponseEntity<List<ProductResponse>> getFeaturedProducts(
            @Parameter(description = "Maximum number of products to return")
            @RequestParam(defaultValue = "10") int limit) {
        
        log.debug("Getting featured products with limit: {}", limit);
        List<ProductResponse> featuredProducts = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(featuredProducts);
    }

    @Operation(summary = "Get product recommendations", 
               description = "Get personalized product recommendations for authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/recommendations")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProductResponse>> getRecommendations(
            @Parameter(description = "Maximum number of recommendations to return")
            @RequestParam(defaultValue = "10") int limit) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            log.warn("Unauthenticated request for recommendations");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        User user = (User) authentication.getPrincipal();
        log.debug("Getting recommendations for user: {} with limit: {}", user.getId(), limit);
        
        List<ProductResponse> recommendations = productService.getRecommendations(user.getId(), limit);
        return ResponseEntity.ok(recommendations);
    }

    @Operation(summary = "Get related products", 
               description = "Get products related to a specific product (same category)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Related products retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/related")
    public ResponseEntity<List<ProductResponse>> getRelatedProducts(
            @Parameter(description = "Product ID") 
            @PathVariable UUID id,
            @Parameter(description = "Maximum number of related products to return")
            @RequestParam(defaultValue = "10") int limit) {
        
        log.debug("Getting related products for product: {} with limit: {}", id, limit);
        List<ProductResponse> relatedProducts = productService.getRelatedProducts(id, limit);
        return ResponseEntity.ok(relatedProducts);
    }

    @Operation(summary = "Get products by store", 
               description = "Get all products for a specific store with filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Store products retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Store not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/store/{storeId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByStore(
            @Parameter(description = "Store ID") 
            @PathVariable UUID storeId,
            @Parameter(description = "Category ID to filter by")
            @RequestParam(required = false) UUID categoryId,
            @Parameter(description = "Filter by featured products only")
            @RequestParam(required = false) Boolean featured,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Getting products for store: {}", storeId);

        ProductFilter filter = ProductFilter.builder()
                .storeId(storeId)
                .categoryId(categoryId)
                .featured(featured)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<ProductResponse> products = productService.getProductsByStore(storeId, filter);
        return ResponseEntity.ok(products);
    }

    @Operation(summary = "Get products by category", 
               description = "Get all products in a specific category with filtering and pagination")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Category products retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Category not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getProductsByCategory(
            @Parameter(description = "Category ID") 
            @PathVariable UUID categoryId,
            @Parameter(description = "Store ID to filter by")
            @RequestParam(required = false) UUID storeId,
            @Parameter(description = "Minimum price filter")
            @RequestParam(required = false) BigDecimal minPrice,
            @Parameter(description = "Maximum price filter")
            @RequestParam(required = false) BigDecimal maxPrice,
            @Parameter(description = "Sort field")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction")
            @RequestParam(defaultValue = "desc") String sortDirection,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size) {
        
        log.debug("Getting products for category: {}", categoryId);

        ProductFilter filter = ProductFilter.builder()
                .categoryId(categoryId)
                .storeId(storeId)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<ProductResponse> products = productService.getProductsByCategory(categoryId, filter);
        return ResponseEntity.ok(products);
    }

    // Store owner/admin endpoints

    @Operation(summary = "Create new product", 
               description = "Create a new product (requires store owner or admin privileges)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Product created successfully",
                     content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid product data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User not authorized to create products",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Product with same slug/SKU already exists",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    @PreAuthorize("hasRole('STORE_OWNER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> createProduct(
            @Parameter(description = "Product creation data")
            @Valid @RequestBody ProductRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        log.info("Creating product: {} for user: {}", request.getName(), user.getEmail());
        
        ProductResponse createdProduct = productService.createProduct(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdProduct);
    }

    @Operation(summary = "Update existing product", 
               description = "Update an existing product (requires store owner or admin privileges)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Product updated successfully",
                     content = @Content(schema = @Schema(implementation = ProductResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid product data",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User not authorized to update this product",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Product with same slug/SKU already exists",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STORE_OWNER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ProductResponse> updateProduct(
            @Parameter(description = "Product ID") 
            @PathVariable UUID id,
            @Parameter(description = "Product update data")
            @Valid @RequestBody ProductRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        log.info("Updating product: {} for user: {}", id, user.getEmail());
        
        ProductResponse updatedProduct = productService.updateProduct(id, request, user.getId());
        return ResponseEntity.ok(updatedProduct);
    }

    @Operation(summary = "Delete product", 
               description = "Delete a product (soft delete - requires store owner or admin privileges)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Product deleted successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "User not authorized to delete this product",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STORE_OWNER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Product ID") 
            @PathVariable UUID id) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        log.info("Deleting product: {} for user: {}", id, user.getEmail());
        
        productService.deleteProduct(id, user.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Check product availability", 
               description = "Check if a product is available for purchase with specified quantity")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Availability check completed"),
        @ApiResponse(responseCode = "404", description = "Product not found",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}/availability")
    public ResponseEntity<Boolean> checkAvailability(
            @Parameter(description = "Product ID") 
            @PathVariable UUID id,
            @Parameter(description = "Quantity to check")
            @RequestParam(defaultValue = "1") int quantity) {
        
        log.debug("Checking availability for product: {} with quantity: {}", id, quantity);
        boolean available = productService.isProductAvailable(id, quantity);
        return ResponseEntity.ok(available);
    }

    // Inventory management endpoints for store owners

    @Operation(summary = "Get low stock products", 
               description = "Get products that are low on stock (requires authentication)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Low stock products retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/low-stock")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<ProductResponse>> getLowStockProducts(
            @Parameter(description = "Store ID to filter by (optional for admins)")
            @RequestParam(required = false) UUID storeId) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        
        log.debug("Getting low stock products for user: {}, storeId: {}", user.getEmail(), storeId);
        
        List<ProductResponse> lowStockProducts = productService.getLowStockProducts(
                storeId != null ? java.util.Optional.of(storeId) : java.util.Optional.empty());
        
        return ResponseEntity.ok(lowStockProducts);
    }
}