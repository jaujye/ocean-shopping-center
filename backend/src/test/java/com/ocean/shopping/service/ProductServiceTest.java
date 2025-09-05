package com.ocean.shopping.service;

import com.ocean.shopping.dto.product.ProductFilter;
import com.ocean.shopping.dto.product.ProductRequest;
import com.ocean.shopping.dto.product.ProductResponse;
import com.ocean.shopping.exception.BadRequestException;
import com.ocean.shopping.exception.ConflictException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.*;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive unit tests for ProductService
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;
    private Store testStore;
    private Category testCategory;
    private User testUser;
    private UUID testProductId;
    private UUID testStoreId;
    private UUID testCategoryId;
    private UUID testUserId;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        testCategoryId = UUID.randomUUID();
        testUserId = UUID.randomUUID();

        // Setup test user
        testUser = User.builder()
                .id(testUserId)
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.STORE_OWNER)
                .build();

        // Setup test store
        testStore = Store.builder()
                .id(testStoreId)
                .name("Test Store")
                .slug("test-store")
                .owner(testUser)
                .build();

        // Setup test category
        testCategory = Category.builder()
                .id(testCategoryId)
                .name("Electronics")
                .slug("electronics")
                .isActive(true)
                .build();

        // Setup test product
        testProduct = Product.builder()
                .id(testProductId)
                .store(testStore)
                .category(testCategory)
                .name("Test Product")
                .slug("test-product")
                .description("Test product description")
                .shortDescription("Test short description")
                .sku("TEST-001")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .trackInventory(true)
                .inventoryQuantity(100)
                .lowStockThreshold(10)
                .isActive(true)
                .isFeatured(false)
                .isDigital(false)
                .requiresShipping(true)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

        // Setup test product request
        testProductRequest = ProductRequest.builder()
                .storeId(testStoreId)
                .categoryId(testCategoryId)
                .name("Test Product")
                .slug("test-product")
                .description("Test product description")
                .shortDescription("Test short description")
                .sku("TEST-001")
                .price(new BigDecimal("99.99"))
                .compareAtPrice(new BigDecimal("149.99"))
                .trackInventory(true)
                .inventoryQuantity(100)
                .lowStockThreshold(10)
                .isActive(true)
                .isFeatured(false)
                .isDigital(false)
                .requiresShipping(true)
                .build();
    }

    @Test
    @DisplayName("Should get all products with pagination")
    void getAllProducts_ShouldReturnPagedProducts() {
        // Given
        ProductFilter filter = ProductFilter.builder()
                .page(0)
                .size(20)
                .sortBy("createdAt")
                .sortDirection("desc")
                .build();

        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getAllProducts(filter);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Product");

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void getProductById_ShouldReturnProduct_WhenProductExists() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        ProductResponse result = productService.getProductById(testProductId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(testProductId);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getSlug()).isEqualTo("test-product");

        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found by ID")
    void getProductById_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(testProductId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product is inactive")
    void getProductById_ShouldThrowException_WhenProductIsInactive() {
        // Given
        testProduct.setIsActive(false);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> productService.getProductById(testProductId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found or not active");

        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should get product by slug successfully")
    void getProductBySlug_ShouldReturnProduct_WhenProductExists() {
        // Given
        String slug = "test-product";
        when(productRepository.findBySlugAndStoreId(slug, testStoreId))
                .thenReturn(Optional.of(testProduct));

        // When
        ProductResponse result = productService.getProductBySlug(slug, testStoreId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getSlug()).isEqualTo(slug);

        verify(productRepository).findBySlugAndStoreId(slug, testStoreId);
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_ShouldReturnCreatedProduct_WhenValidRequest() {
        // Given
        when(userService.getUserById(testUserId)).thenReturn(testUser);
        when(productRepository.findBySlugAndStoreId(testProductRequest.getSlug(), testStoreId))
                .thenReturn(Optional.empty());
        when(productRepository.findBySkuAndStoreId(testProductRequest.getSku(), testStoreId))
                .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductResponse result = productService.createProduct(testProductRequest, testUserId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Test Product");

        verify(userService).getUserById(testUserId);
        verify(productRepository).findBySlugAndStoreId(testProductRequest.getSlug(), testStoreId);
        verify(productRepository).findBySkuAndStoreId(testProductRequest.getSku(), testStoreId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when product slug already exists")
    void createProduct_ShouldThrowException_WhenSlugAlreadyExists() {
        // Given
        when(userService.getUserById(testUserId)).thenReturn(testUser);
        when(productRepository.findBySlugAndStoreId(testProductRequest.getSlug(), testStoreId))
                .thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(testProductRequest, testUserId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Product with slug 'test-product' already exists");

        verify(userService).getUserById(testUserId);
        verify(productRepository).findBySlugAndStoreId(testProductRequest.getSlug(), testStoreId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ConflictException when product SKU already exists")
    void createProduct_ShouldThrowException_WhenSkuAlreadyExists() {
        // Given
        when(userService.getUserById(testUserId)).thenReturn(testUser);
        when(productRepository.findBySlugAndStoreId(testProductRequest.getSlug(), testStoreId))
                .thenReturn(Optional.empty());
        when(productRepository.findBySkuAndStoreId(testProductRequest.getSku(), testStoreId))
                .thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> productService.createProduct(testProductRequest, testUserId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Product with SKU 'TEST-001' already exists");

        verify(userService).getUserById(testUserId);
        verify(productRepository).findBySlugAndStoreId(testProductRequest.getSlug(), testStoreId);
        verify(productRepository).findBySkuAndStoreId(testProductRequest.getSku(), testStoreId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should update product successfully")
    void updateProduct_ShouldReturnUpdatedProduct_WhenValidRequest() {
        // Given
        ProductRequest updateRequest = ProductRequest.builder()
                .storeId(testStoreId)
                .categoryId(testCategoryId)
                .name("Updated Product Name")
                .slug("updated-product")
                .price(new BigDecimal("199.99"))
                .build();

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
        when(userService.getUserById(testUserId)).thenReturn(testUser);
        when(productRepository.findBySlugAndStoreId("updated-product", testStoreId))
                .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        ProductResponse result = productService.updateProduct(testProductId, updateRequest, testUserId);

        // Then
        assertThat(result).isNotNull();
        verify(productRepository).findById(testProductId);
        verify(userService).getUserById(testUserId);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent product")
    void updateProduct_ShouldThrowException_WhenProductNotFound() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> productService.updateProduct(testProductId, testProductRequest, testUserId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should delete product successfully (soft delete)")
    void deleteProduct_ShouldSoftDeleteProduct_WhenProductExists() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
        when(userService.getUserById(testUserId)).thenReturn(testUser);
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.deleteProduct(testProductId, testUserId);

        // Then
        verify(productRepository).findById(testProductId);
        verify(userService).getUserById(testUserId);
        verify(productRepository).save(argThat(product -> !product.getIsActive()));
    }

    @Test
    @DisplayName("Should search products with query")
    void searchProducts_ShouldReturnFilteredProducts_WhenQueryProvided() {
        // Given
        ProductFilter filter = ProductFilter.builder()
                .query("test product")
                .page(0)
                .size(20)
                .build();

        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.searchProducts(filter);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get featured products")
    void getFeaturedProducts_ShouldReturnFeaturedProducts() {
        // Given
        testProduct.setIsFeatured(true);
        List<Product> featuredProducts = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(featuredProducts);

        when(productRepository.findByIsFeaturedTrueAndIsActiveTrue(any(Pageable.class)))
                .thenReturn(productPage);

        // When
        List<ProductResponse> result = productService.getFeaturedProducts(10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsFeatured()).isTrue();

        verify(productRepository).findByIsFeaturedTrueAndIsActiveTrue(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get related products")
    void getRelatedProducts_ShouldReturnRelatedProducts_WhenProductHasCategory() {
        // Given
        List<Product> relatedProducts = Arrays.asList(testProduct);

        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
        when(productRepository.findRelatedProducts(testCategoryId, testProductId, any(Pageable.class)))
                .thenReturn(relatedProducts);

        // When
        List<ProductResponse> result = productService.getRelatedProducts(testProductId, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(productRepository).findById(testProductId);
        verify(productRepository).findRelatedProducts(testCategoryId, testProductId, any(Pageable.class));
    }

    @Test
    @DisplayName("Should return empty list when product has no category")
    void getRelatedProducts_ShouldReturnEmptyList_WhenProductHasNoCategory() {
        // Given
        testProduct.setCategory(null);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        List<ProductResponse> result = productService.getRelatedProducts(testProductId, 10);

        // Then
        assertThat(result).isEmpty();

        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).findRelatedProducts(any(), any(), any());
    }

    @Test
    @DisplayName("Should check product availability correctly")
    void isProductAvailable_ShouldReturnTrue_WhenSufficientStock() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(testProductId, 5);

        // Then
        assertThat(result).isTrue();
        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should return false when insufficient stock")
    void isProductAvailable_ShouldReturnFalse_WhenInsufficientStock() {
        // Given
        testProduct.setInventoryQuantity(3);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(testProductId, 5);

        // Then
        assertThat(result).isFalse();
        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should return true when inventory tracking is disabled")
    void isProductAvailable_ShouldReturnTrue_WhenInventoryTrackingDisabled() {
        // Given
        testProduct.setTrackInventory(false);
        testProduct.setInventoryQuantity(0);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(testProductId, 100);

        // Then
        assertThat(result).isTrue();
        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should return false when product is inactive")
    void isProductAvailable_ShouldReturnFalse_WhenProductIsInactive() {
        // Given
        testProduct.setIsActive(false);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        boolean result = productService.isProductAvailable(testProductId, 1);

        // Then
        assertThat(result).isFalse();
        verify(productRepository).findById(testProductId);
    }

    @Test
    @DisplayName("Should update inventory successfully")
    void updateInventory_ShouldUpdateQuantity_WhenValidChange() {
        // Given
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // When
        productService.updateInventory(testProductId, -5);

        // Then
        verify(productRepository).findById(testProductId);
        verify(productRepository).save(argThat(product -> product.getInventoryQuantity() == 95));
    }

    @Test
    @DisplayName("Should throw BadRequestException when insufficient inventory")
    void updateInventory_ShouldThrowException_WhenInsufficientInventory() {
        // Given
        testProduct.setInventoryQuantity(5);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When & Then
        assertThatThrownBy(() -> productService.updateInventory(testProductId, -10))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient inventory");

        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should skip inventory update when tracking is disabled")
    void updateInventory_ShouldSkipUpdate_WhenTrackingDisabled() {
        // Given
        testProduct.setTrackInventory(false);
        when(productRepository.findById(testProductId)).thenReturn(Optional.of(testProduct));

        // When
        productService.updateInventory(testProductId, -10);

        // Then
        verify(productRepository).findById(testProductId);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    @DisplayName("Should get recommendations")
    void getRecommendations_ShouldReturnRecommendations() {
        // Given
        List<Product> popularProducts = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(popularProducts);

        when(productRepository.findPopularProducts(any(Pageable.class)))
                .thenReturn(productPage);

        // When
        List<ProductResponse> result = productService.getRecommendations(testUserId, 10);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(productRepository).findPopularProducts(any(Pageable.class));
    }

    @Test
    @DisplayName("Should get low stock products")
    void getLowStockProducts_ShouldReturnLowStockProducts() {
        // Given
        testProduct.setInventoryQuantity(5); // Below threshold of 10
        List<Product> lowStockProducts = Arrays.asList(testProduct);

        when(productRepository.findLowStockProducts()).thenReturn(lowStockProducts);

        // When
        List<ProductResponse> result = productService.getLowStockProducts(Optional.empty());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLowStock()).isTrue();

        verify(productRepository).findLowStockProducts();
    }

    @Test
    @DisplayName("Should get low stock products by store")
    void getLowStockProducts_ShouldReturnLowStockProductsByStore() {
        // Given
        testProduct.setInventoryQuantity(5); // Below threshold of 10
        List<Product> lowStockProducts = Arrays.asList(testProduct);

        when(productRepository.findLowStockProductsByStore(testStoreId)).thenReturn(lowStockProducts);

        // When
        List<ProductResponse> result = productService.getLowStockProducts(Optional.of(testStoreId));

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        verify(productRepository).findLowStockProductsByStore(testStoreId);
    }

    @Test
    @DisplayName("Should normalize product filter")
    void getAllProducts_ShouldNormalizeFilter() {
        // Given
        ProductFilter filter = ProductFilter.builder()
                .minPrice(new BigDecimal("200"))
                .maxPrice(new BigDecimal("100")) // Intentionally reversed
                .sortDirection("INVALID")
                .page(-1)
                .size(-5)
                .build();

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        // When
        productService.getAllProducts(filter);

        // Then
        // Verify that the filter was normalized (min/max prices swapped, invalid values corrected)
        assertThat(filter.getMinPrice()).isEqualTo(new BigDecimal("100"));
        assertThat(filter.getMaxPrice()).isEqualTo(new BigDecimal("200"));
        assertThat(filter.getSortDirection()).isEqualTo("desc");
        assertThat(filter.getPage()).isEqualTo(0);
        assertThat(filter.getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("Should get products by store")
    void getProductsByStore_ShouldReturnProductsForStore() {
        // Given
        ProductFilter filter = ProductFilter.builder().build();
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getProductsByStore(testStoreId, filter);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(filter.getStoreId()).isEqualTo(testStoreId);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("Should get products by category")
    void getProductsByCategory_ShouldReturnProductsForCategory() {
        // Given
        ProductFilter filter = ProductFilter.builder().build();
        List<Product> products = Arrays.asList(testProduct);
        Page<Product> productPage = new PageImpl<>(products);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(productPage);

        // When
        Page<ProductResponse> result = productService.getProductsByCategory(testCategoryId, filter);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(filter.getCategoryId()).isEqualTo(testCategoryId);

        verify(productRepository).findAll(any(Specification.class), any(Pageable.class));
    }
}