package com.ocean.shopping.service;

import com.ocean.shopping.dto.product.ProductFilter;
import com.ocean.shopping.dto.product.ProductRequest;
import com.ocean.shopping.dto.product.ProductResponse;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ConflictException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.repository.ProductRepository;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Product service for managing products with advanced search and filtering
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final UserService userService;

    /**
     * Get all products with filtering and pagination
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(ProductFilter filter) {
        log.debug("Getting all products with filter: {}", filter);

        filter.normalize();
        
        Specification<Product> specification = createSpecification(filter);
        Pageable pageable = createPageable(filter);
        
        Page<Product> productPage = productRepository.findAll(specification, pageable);
        
        return productPage.map(ProductResponse::summaryFromEntity);
    }

    /**
     * Get product by ID
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        log.debug("Getting product by ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Product", id));

        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product not found or not active");
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Get product by slug and store ID
     */
    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug, UUID storeId) {
        log.debug("Getting product by slug: {} and store ID: {}", slug, storeId);

        Product product = productRepository.findBySlugAndStoreId(slug, storeId)
                .orElseThrow(() -> ResourceNotFoundException.forField("Product", "slug", slug));

        if (!product.getIsActive()) {
            throw new ResourceNotFoundException("Product not found or not active");
        }

        return ProductResponse.fromEntity(product);
    }

    /**
     * Create new product
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request, UUID userId) {
        log.info("Creating product with name: {} for user ID: {}", request.getName(), userId);

        // Validate user has permission to create products for this store
        validateStoreAccess(request.getStoreId(), userId);

        // Validate unique constraints
        validateUniqueConstraints(request, null);

        // Build product entity
        Product product = buildProductFromRequest(request);

        // Save product
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {}", savedProduct.getId());
        return ProductResponse.fromEntity(savedProduct);
    }

    /**
     * Update existing product
     */
    @Transactional
    public ProductResponse updateProduct(UUID id, ProductRequest request, UUID userId) {
        log.info("Updating product with ID: {} for user ID: {}", id, userId);

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Product", id));

        // Validate user has permission to update this product
        validateStoreAccess(existingProduct.getStore().getId(), userId);

        // Validate unique constraints (excluding current product)
        validateUniqueConstraints(request, id);

        // Update product fields
        updateProductFromRequest(existingProduct, request);

        // Save updated product
        Product updatedProduct = productRepository.save(existingProduct);

        log.info("Product updated successfully with ID: {}", updatedProduct.getId());
        return ProductResponse.fromEntity(updatedProduct);
    }

    /**
     * Delete product
     */
    @Transactional
    public void deleteProduct(UUID id, UUID userId) {
        log.info("Deleting product with ID: {} for user ID: {}", id, userId);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.forEntity("Product", id));

        // Validate user has permission to delete this product
        validateStoreAccess(product.getStore().getId(), userId);

        // Soft delete by setting active to false
        product.setIsActive(false);
        productRepository.save(product);

        log.info("Product deleted successfully with ID: {}", id);
    }

    /**
     * Search products with advanced filtering
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductFilter filter) {
        log.debug("Searching products with filter: {}", filter);

        filter.normalize();
        
        if (!filter.hasSearchQuery()) {
            return getAllProducts(filter);
        }

        Specification<Product> specification = createSearchSpecification(filter);
        Pageable pageable = createPageable(filter);
        
        Page<Product> productPage = productRepository.findAll(specification, pageable);
        
        return productPage.map(ProductResponse::summaryFromEntity);
    }

    /**
     * Get featured products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        log.debug("Getting featured products with limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        Page<Product> featuredProducts = productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable);

        return featuredProducts.getContent().stream()
                .map(ProductResponse::summaryFromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get products by store
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByStore(UUID storeId, ProductFilter filter) {
        log.debug("Getting products for store ID: {} with filter: {}", storeId, filter);

        filter.normalize();
        filter.setStoreId(storeId);

        return getAllProducts(filter);
    }

    /**
     * Get products by category
     */
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(UUID categoryId, ProductFilter filter) {
        log.debug("Getting products for category ID: {} with filter: {}", categoryId, filter);

        filter.normalize();
        filter.setCategoryId(categoryId);

        return getAllProducts(filter);
    }

    /**
     * Get related products (same category, excluding current product)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(UUID productId, int limit) {
        log.debug("Getting related products for product ID: {} with limit: {}", productId, limit);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));

        if (product.getCategory() == null) {
            return List.of();
        }

        Pageable pageable = PageRequest.of(0, limit);
        List<Product> relatedProducts = productRepository.findRelatedProducts(
                product.getCategory().getId(), productId, pageable);

        return relatedProducts.stream()
                .map(ProductResponse::summaryFromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Get product recommendations (simplified algorithm)
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getRecommendations(UUID userId, int limit) {
        log.debug("Getting product recommendations for user ID: {} with limit: {}", userId, limit);

        // Simple recommendation algorithm: popular products + featured products
        Pageable pageable = PageRequest.of(0, limit);
        
        // Get popular products (by review count)
        Page<Product> popularProducts = productRepository.findPopularProducts(pageable);
        
        List<ProductResponse> recommendations = popularProducts.getContent().stream()
                .map(ProductResponse::summaryFromEntity)
                .collect(Collectors.toList());
        
        // If not enough popular products, fill with featured products
        if (recommendations.size() < limit) {
            int remaining = limit - recommendations.size();
            List<ProductResponse> featuredProducts = getFeaturedProducts(remaining);
            
            // Add featured products that are not already in recommendations
            List<UUID> existingIds = recommendations.stream()
                    .map(ProductResponse::getId)
                    .collect(Collectors.toList());
                    
            featuredProducts.stream()
                    .filter(p -> !existingIds.contains(p.getId()))
                    .limit(remaining)
                    .forEach(recommendations::add);
        }

        return recommendations.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Check product availability
     */
    @Transactional(readOnly = true)
    public boolean isProductAvailable(UUID productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));

        if (!product.getIsActive()) {
            return false;
        }

        if (!product.getTrackInventory()) {
            return true; // Infinite inventory
        }

        return product.getInventoryQuantity() >= quantity;
    }

    /**
     * Update product inventory
     */
    @Transactional
    public void updateInventory(UUID productId, int quantityChange) {
        log.debug("Updating inventory for product ID: {} with quantity change: {}", productId, quantityChange);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with ID: " + productId));

        if (!product.getTrackInventory()) {
            log.debug("Product does not track inventory, skipping update");
            return;
        }

        int newQuantity = product.getInventoryQuantity() + quantityChange;
        if (newQuantity < 0) {
            throw new BadRequestException("Insufficient inventory. Available: " + product.getInventoryQuantity());
        }

        product.setInventoryQuantity(newQuantity);
        productRepository.save(product);

        log.debug("Inventory updated for product ID: {}, new quantity: {}", productId, newQuantity);
    }

    /**
     * Get low stock products
     */
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(Optional<UUID> storeId) {
        log.debug("Getting low stock products for store ID: {}", storeId);

        List<Product> lowStockProducts;
        if (storeId.isPresent()) {
            lowStockProducts = productRepository.findLowStockProductsByStore(storeId.get());
        } else {
            lowStockProducts = productRepository.findLowStockProducts();
        }

        return lowStockProducts.stream()
                .map(ProductResponse::summaryFromEntity)
                .collect(Collectors.toList());
    }

    // Private helper methods

    private Specification<Product> createSpecification(ProductFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Active products only (unless specifically requested otherwise)
            if (filter.getActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), filter.getActive()));
            }

            // Store filter
            if (filter.getStoreId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("store").get("id"), filter.getStoreId()));
            }

            // Category filter
            if (filter.hasCategoryFilter()) {
                List<UUID> categoryIds = filter.getEffectiveCategoryIds();
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            // Price range filter
            if (filter.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), filter.getMinPrice()));
            }
            if (filter.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), filter.getMaxPrice()));
            }

            // Stock filter
            if (filter.getInStock() != null && filter.getInStock()) {
                Predicate noTracking = criteriaBuilder.equal(root.get("trackInventory"), false);
                Predicate hasStock = criteriaBuilder.and(
                    criteriaBuilder.equal(root.get("trackInventory"), true),
                    criteriaBuilder.greaterThan(root.get("inventoryQuantity"), 0)
                );
                predicates.add(criteriaBuilder.or(noTracking, hasStock));
            }

            // Featured filter
            if (filter.getFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), filter.getFeatured()));
            }

            // Digital filter
            if (filter.getDigital() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isDigital"), filter.getDigital()));
            }

            // Discount filter
            if (filter.getHasDiscount() != null && filter.getHasDiscount()) {
                predicates.add(criteriaBuilder.and(
                    criteriaBuilder.isNotNull(root.get("compareAtPrice")),
                    criteriaBuilder.greaterThan(root.get("compareAtPrice"), root.get("price"))
                ));
            }

            // Rating filter (requires join with reviews)
            if (filter.getMinRating() != null) {
                Join<Product, Review> reviewJoin = root.join("reviews", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(reviewJoin.get("isPublished"), true));
                
                // Group by product and filter by average rating
                query.groupBy(root.get("id"));
                query.having(criteriaBuilder.greaterThanOrEqualTo(
                    criteriaBuilder.avg(reviewJoin.get("rating")).as(BigDecimal.class),
                    filter.getMinRating()
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Specification<Product> createSearchSpecification(ProductFilter filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Apply base filters
            Specification<Product> baseSpec = createSpecification(filter);
            predicates.add(baseSpec.toPredicate(root, query, criteriaBuilder));

            // Text search
            if (filter.hasSearchQuery()) {
                String searchTerm = "%" + filter.getQuery().toLowerCase() + "%";
                Join<Product, Category> categoryJoin = root.join("category", JoinType.LEFT);

                Predicate nameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), searchTerm);
                Predicate descriptionMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), searchTerm);
                Predicate shortDescMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("shortDescription")), searchTerm);
                Predicate skuMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), searchTerm);
                Predicate categoryMatch = criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("name")), searchTerm);

                Predicate searchPredicate = criteriaBuilder.or(
                    nameMatch, descriptionMatch, shortDescMatch, skuMatch, categoryMatch
                );
                
                predicates.add(searchPredicate);
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Pageable createPageable(ProductFilter filter) {
        Sort sort = createSort(filter.getSortBy(), filter.getSortDirection());
        return PageRequest.of(filter.getPage(), filter.getSize(), sort);
    }

    private Sort createSort(String sortBy, String sortDirection) {
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection) ? 
                Sort.Direction.ASC : Sort.Direction.DESC;

        return switch (sortBy.toLowerCase()) {
            case "name" -> Sort.by(direction, "name");
            case "price" -> Sort.by(direction, "price");
            case "createdat" -> Sort.by(direction, "createdAt");
            case "updatedat" -> Sort.by(direction, "updatedAt");
            case "rating" -> Sort.by(direction, "createdAt"); // Simplified for now
            case "popularity" -> Sort.by(direction, "createdAt"); // Simplified for now
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private void validateStoreAccess(UUID storeId, UUID userId) {
        // This is a simplified validation - in a real system, you would check:
        // 1. If user owns the store
        // 2. If user has store manager permissions
        // 3. If user is an admin
        
        User user = userService.getUserById(userId);
        // For now, we'll assume any authenticated user can create products
        // This should be enhanced based on your business rules
    }

    private void validateUniqueConstraints(ProductRequest request, UUID excludeId) {
        // Check slug uniqueness within store
        if (StringUtils.hasText(request.getSlug())) {
            Optional<Product> existingBySlug = productRepository.findBySlugAndStoreId(
                    request.getSlug(), request.getStoreId());
                    
            if (existingBySlug.isPresent() && 
                (excludeId == null || !existingBySlug.get().getId().equals(excludeId))) {
                throw new ConflictException("Product with slug '" + request.getSlug() + "' already exists in this store");
            }
        }

        // Check SKU uniqueness within store
        if (StringUtils.hasText(request.getSku())) {
            Optional<Product> existingBySku = productRepository.findBySkuAndStoreId(
                    request.getSku(), request.getStoreId());
                    
            if (existingBySku.isPresent() && 
                (excludeId == null || !existingBySku.get().getId().equals(excludeId))) {
                throw new ConflictException("Product with SKU '" + request.getSku() + "' already exists in this store");
            }
        }
    }

    private Product buildProductFromRequest(ProductRequest request) {
        return Product.builder()
                // Basic fields will be set here
                // For now returning a minimal product
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .shortDescription(request.getShortDescription())
                .sku(request.getSku())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .cost(request.getCost())
                .trackInventory(request.getTrackInventory())
                .inventoryQuantity(request.getInventoryQuantity())
                .lowStockThreshold(request.getLowStockThreshold())
                .weight(request.getWeight())
                .dimensionsLength(request.getDimensionsLength())
                .dimensionsWidth(request.getDimensionsWidth())
                .dimensionsHeight(request.getDimensionsHeight())
                .requiresShipping(request.getRequiresShipping())
                .isDigital(request.getIsDigital())
                .isFeatured(request.getIsFeatured())
                .isActive(request.getIsActive())
                .seoTitle(request.getSeoTitle())
                .seoDescription(request.getSeoDescription())
                .build();
    }

    private void updateProductFromRequest(Product product, ProductRequest request) {
        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setSku(request.getSku());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCost(request.getCost());
        product.setTrackInventory(request.getTrackInventory());
        product.setInventoryQuantity(request.getInventoryQuantity());
        product.setLowStockThreshold(request.getLowStockThreshold());
        product.setWeight(request.getWeight());
        product.setDimensionsLength(request.getDimensionsLength());
        product.setDimensionsWidth(request.getDimensionsWidth());
        product.setDimensionsHeight(request.getDimensionsHeight());
        product.setRequiresShipping(request.getRequiresShipping());
        product.setIsDigital(request.getIsDigital());
        product.setIsFeatured(request.getIsFeatured());
        product.setIsActive(request.getIsActive());
        product.setSeoTitle(request.getSeoTitle());
        product.setSeoDescription(request.getSeoDescription());
    }
}