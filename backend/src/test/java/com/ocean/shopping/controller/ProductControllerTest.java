package com.ocean.shopping.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocean.shopping.dto.product.ProductRequest;
import com.ocean.shopping.dto.product.ProductResponse;
import com.ocean.shopping.exception.ConflictException;
import com.ocean.shopping.exception.ResourceNotFoundException;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProductController
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProductResponse testProductResponse;
    private ProductRequest testProductRequest;
    private UUID testProductId;
    private UUID testStoreId;
    private UUID testCategoryId;
    private User testUser;

    @BeforeEach
    void setUp() {
        testProductId = UUID.randomUUID();
        testStoreId = UUID.randomUUID();
        testCategoryId = UUID.randomUUID();

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(UserRole.STORE_OWNER)
                .build();

        testProductResponse = ProductResponse.builder()
                .id(testProductId)
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
                .inStock(true)
                .lowStock(false)
                .hasDiscount(true)
                .discountAmount(new BigDecimal("50.00"))
                .discountPercentage(new BigDecimal("33.33"))
                .averageRating(new BigDecimal("4.5"))
                .reviewCount(150)
                .createdAt(ZonedDateTime.now())
                .updatedAt(ZonedDateTime.now())
                .build();

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
    @DisplayName("Should get all products with default parameters")
    void getAllProducts_ShouldReturnProducts_WhenCalled() throws Exception {
        // Given
        List<ProductResponse> products = Arrays.asList(testProductResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products);

        when(productService.getAllProducts(any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(testProductId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("Test Product"))
                .andExpect(jsonPath("$.content[0].price").value(99.99));

        verify(productService).getAllProducts(any());
    }

    @Test
    @DisplayName("Should get all products with filters")
    void getAllProducts_ShouldReturnFilteredProducts_WhenFiltersProvided() throws Exception {
        // Given
        List<ProductResponse> products = Arrays.asList(testProductResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products);

        when(productService.getAllProducts(any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/api/products")
                .param("query", "test")
                .param("storeId", testStoreId.toString())
                .param("categoryId", testCategoryId.toString())
                .param("minPrice", "50.00")
                .param("maxPrice", "200.00")
                .param("inStock", "true")
                .param("featured", "false")
                .param("sortBy", "price")
                .param("sortDirection", "asc")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(productService).getAllProducts(argThat(filter ->
                "test".equals(filter.getQuery()) &&
                testStoreId.equals(filter.getStoreId()) &&
                testCategoryId.equals(filter.getCategoryId()) &&
                new BigDecimal("50.00").equals(filter.getMinPrice()) &&
                new BigDecimal("200.00").equals(filter.getMaxPrice()) &&
                Boolean.TRUE.equals(filter.getInStock()) &&
                Boolean.FALSE.equals(filter.getFeatured()) &&
                "price".equals(filter.getSortBy()) &&
                "asc".equals(filter.getSortDirection()) &&
                0 == filter.getPage() &&
                10 == filter.getSize()
        ));
    }

    @Test
    @DisplayName("Should get product by ID")
    void getProductById_ShouldReturnProduct_WhenProductExists() throws Exception {
        // Given
        when(productService.getProductById(testProductId)).thenReturn(testProductResponse);

        // When & Then
        mockMvc.perform(get("/api/products/{id}", testProductId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(testProductId.toString()))
                .andExpect(jsonPath("$.name").value("Test Product"))
                .andExpect(jsonPath("$.price").value(99.99));

        verify(productService).getProductById(testProductId);
    }

    @Test
    @DisplayName("Should return 404 when product not found by ID")
    void getProductById_ShouldReturn404_WhenProductNotFound() throws Exception {
        // Given
        when(productService.getProductById(testProductId))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(get("/api/products/{id}", testProductId))
                .andExpect(status().isNotFound());

        verify(productService).getProductById(testProductId);
    }

    @Test
    @DisplayName("Should get product by slug")
    void getProductBySlug_ShouldReturnProduct_WhenProductExists() throws Exception {
        // Given
        String slug = "test-product";
        when(productService.getProductBySlug(slug, testStoreId)).thenReturn(testProductResponse);

        // When & Then
        mockMvc.perform(get("/api/products/store/{storeId}/slug/{slug}", testStoreId, slug))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.slug").value(slug))
                .andExpect(jsonPath("$.name").value("Test Product"));

        verify(productService).getProductBySlug(slug, testStoreId);
    }

    @Test
    @DisplayName("Should search products")
    void searchProducts_ShouldReturnSearchResults_WhenQueryProvided() throws Exception {
        // Given
        String query = "test product";
        List<ProductResponse> products = Arrays.asList(testProductResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products);

        when(productService.searchProducts(any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/api/products/search")
                .param("q", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].name").value("Test Product"));

        verify(productService).searchProducts(argThat(filter ->
                query.equals(filter.getQuery())
        ));
    }

    @Test
    @DisplayName("Should get featured products")
    void getFeaturedProducts_ShouldReturnFeaturedProducts() throws Exception {
        // Given
        testProductResponse.setIsFeatured(true);
        List<ProductResponse> featuredProducts = Arrays.asList(testProductResponse);

        when(productService.getFeaturedProducts(10)).thenReturn(featuredProducts);

        // When & Then
        mockMvc.perform(get("/api/products/featured"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].isFeatured").value(true));

        verify(productService).getFeaturedProducts(10);
    }

    @Test
    @DisplayName("Should get featured products with custom limit")
    void getFeaturedProducts_ShouldReturnFeaturedProducts_WithCustomLimit() throws Exception {
        // Given
        List<ProductResponse> featuredProducts = Arrays.asList(testProductResponse);
        when(productService.getFeaturedProducts(5)).thenReturn(featuredProducts);

        // When & Then
        mockMvc.perform(get("/api/products/featured")
                .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(productService).getFeaturedProducts(5);
    }

    @Test
    @DisplayName("Should get recommendations for authenticated user")
    @WithMockUser(roles = {"CUSTOMER"})
    void getRecommendations_ShouldReturnRecommendations_WhenUserAuthenticated() throws Exception {
        // Given
        List<ProductResponse> recommendations = Arrays.asList(testProductResponse);
        when(productService.getRecommendations(any(UUID.class), eq(10)))
                .thenReturn(recommendations);

        // When & Then
        mockMvc.perform(get("/api/products/recommendations")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(productService).getRecommendations(any(UUID.class), eq(10));
    }

    @Test
    @DisplayName("Should return 401 for recommendations when not authenticated")
    void getRecommendations_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/recommendations"))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).getRecommendations(any(), anyInt());
    }

    @Test
    @DisplayName("Should get related products")
    void getRelatedProducts_ShouldReturnRelatedProducts() throws Exception {
        // Given
        List<ProductResponse> relatedProducts = Arrays.asList(testProductResponse);
        when(productService.getRelatedProducts(testProductId, 10)).thenReturn(relatedProducts);

        // When & Then
        mockMvc.perform(get("/api/products/{id}/related", testProductId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(productService).getRelatedProducts(testProductId, 10);
    }

    @Test
    @DisplayName("Should get products by store")
    void getProductsByStore_ShouldReturnStoreProducts() throws Exception {
        // Given
        List<ProductResponse> products = Arrays.asList(testProductResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products);

        when(productService.getProductsByStore(eq(testStoreId), any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/api/products/store/{storeId}", testStoreId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(productService).getProductsByStore(eq(testStoreId), any());
    }

    @Test
    @DisplayName("Should get products by category")
    void getProductsByCategory_ShouldReturnCategoryProducts() throws Exception {
        // Given
        List<ProductResponse> products = Arrays.asList(testProductResponse);
        Page<ProductResponse> productPage = new PageImpl<>(products);

        when(productService.getProductsByCategory(eq(testCategoryId), any())).thenReturn(productPage);

        // When & Then
        mockMvc.perform(get("/api/products/category/{categoryId}", testCategoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)));

        verify(productService).getProductsByCategory(eq(testCategoryId), any());
    }

    @Test
    @DisplayName("Should create product successfully")
    @WithMockUser(roles = {"STORE_OWNER"})
    void createProduct_ShouldReturnCreatedProduct_WhenValidRequest() throws Exception {
        // Given
        when(productService.createProduct(any(ProductRequest.class), any(UUID.class)))
                .thenReturn(testProductResponse);

        // When & Then
        mockMvc.perform(post("/api/products")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testProductId.toString()))
                .andExpect(jsonPath("$.name").value("Test Product"));

        verify(productService).createProduct(any(ProductRequest.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should return 400 when creating product with invalid data")
    @WithMockUser(roles = {"STORE_OWNER"})
    void createProduct_ShouldReturn400_WhenInvalidRequest() throws Exception {
        // Given
        ProductRequest invalidRequest = ProductRequest.builder()
                .storeId(testStoreId)
                .name("") // Invalid: empty name
                .price(new BigDecimal("-10")) // Invalid: negative price
                .build();

        // When & Then
        mockMvc.perform(post("/api/products")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(), any());
    }

    @Test
    @DisplayName("Should return 409 when creating product with duplicate slug")
    @WithMockUser(roles = {"STORE_OWNER"})
    void createProduct_ShouldReturn409_WhenDuplicateSlug() throws Exception {
        // Given
        when(productService.createProduct(any(ProductRequest.class), any(UUID.class)))
                .thenThrow(new ConflictException("Product with slug already exists"));

        // When & Then
        mockMvc.perform(post("/api/products")
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isConflict());

        verify(productService).createProduct(any(ProductRequest.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should return 401 when creating product without authentication")
    void createProduct_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).createProduct(any(), any());
    }

    @Test
    @DisplayName("Should update product successfully")
    @WithMockUser(roles = {"STORE_OWNER"})
    void updateProduct_ShouldReturnUpdatedProduct_WhenValidRequest() throws Exception {
        // Given
        when(productService.updateProduct(eq(testProductId), any(ProductRequest.class), any(UUID.class)))
                .thenReturn(testProductResponse);

        // When & Then
        mockMvc.perform(put("/api/products/{id}", testProductId)
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testProductId.toString()));

        verify(productService).updateProduct(eq(testProductId), any(ProductRequest.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent product")
    @WithMockUser(roles = {"STORE_OWNER"})
    void updateProduct_ShouldReturn404_WhenProductNotFound() throws Exception {
        // Given
        when(productService.updateProduct(eq(testProductId), any(ProductRequest.class), any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        // When & Then
        mockMvc.perform(put("/api/products/{id}", testProductId)
                .with(user(testUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testProductRequest)))
                .andExpect(status().isNotFound());

        verify(productService).updateProduct(eq(testProductId), any(ProductRequest.class), any(UUID.class));
    }

    @Test
    @DisplayName("Should delete product successfully")
    @WithMockUser(roles = {"STORE_OWNER"})
    void deleteProduct_ShouldReturn204_WhenProductExists() throws Exception {
        // Given
        doNothing().when(productService).deleteProduct(testProductId, any(UUID.class));

        // When & Then
        mockMvc.perform(delete("/api/products/{id}", testProductId)
                .with(user(testUser)))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(eq(testProductId), any(UUID.class));
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent product")
    @WithMockUser(roles = {"STORE_OWNER"})
    void deleteProduct_ShouldReturn404_WhenProductNotFound() throws Exception {
        // Given
        doThrow(new ResourceNotFoundException("Product not found"))
                .when(productService).deleteProduct(testProductId, any(UUID.class));

        // When & Then
        mockMvc.perform(delete("/api/products/{id}", testProductId)
                .with(user(testUser)))
                .andExpect(status().isNotFound());

        verify(productService).deleteProduct(eq(testProductId), any(UUID.class));
    }

    @Test
    @DisplayName("Should check product availability")
    void checkAvailability_ShouldReturnAvailability() throws Exception {
        // Given
        when(productService.isProductAvailable(testProductId, 5)).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/products/{id}/availability", testProductId)
                .param("quantity", "5"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(productService).isProductAvailable(testProductId, 5);
    }

    @Test
    @DisplayName("Should get low stock products")
    @WithMockUser(roles = {"STORE_OWNER"})
    void getLowStockProducts_ShouldReturnLowStockProducts() throws Exception {
        // Given
        testProductResponse.setLowStock(true);
        List<ProductResponse> lowStockProducts = Arrays.asList(testProductResponse);

        when(productService.getLowStockProducts(any())).thenReturn(lowStockProducts);

        // When & Then
        mockMvc.perform(get("/api/products/low-stock")
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].lowStock").value(true));

        verify(productService).getLowStockProducts(any());
    }

    @Test
    @DisplayName("Should get low stock products by store")
    @WithMockUser(roles = {"STORE_OWNER"})
    void getLowStockProducts_ShouldReturnLowStockProductsByStore() throws Exception {
        // Given
        List<ProductResponse> lowStockProducts = Arrays.asList(testProductResponse);
        when(productService.getLowStockProducts(any())).thenReturn(lowStockProducts);

        // When & Then
        mockMvc.perform(get("/api/products/low-stock")
                .param("storeId", testStoreId.toString())
                .with(user(testUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(productService).getLowStockProducts(argThat(optional ->
                optional.isPresent() && optional.get().equals(testStoreId)
        ));
    }

    @Test
    @DisplayName("Should return 401 for low stock products when not authenticated")
    void getLowStockProducts_ShouldReturn401_WhenNotAuthenticated() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isUnauthorized());

        verify(productService, never()).getLowStockProducts(any());
    }
}