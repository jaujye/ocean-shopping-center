import { apiClient } from './api';
import {
  Product,
  PaginatedResponse,
  ApiResponse,
} from '../types';
import {
  ProductFilters,
  ProductFormData,
  BulkAction,
  ProductCategory,
  ProductStats,
  InventoryUpdate,
  ProductSearchResult,
} from '../types/product';

class ProductService {
  /**
   * Get products with advanced filtering for store owners
   */
  async getStoreProducts(filters: ProductFilters & {
    page?: number;
    limit?: number;
    storeId?: string;
  }): Promise<PaginatedResponse<Product>> {
    const params = {
      page: filters.page || 1,
      limit: filters.limit || 20,
      search: filters.search,
      category: filters.category,
      storeId: filters.storeId,
      minPrice: filters.minPrice,
      maxPrice: filters.maxPrice,
      sortBy: filters.sortBy,
      sortOrder: filters.sortOrder,
      inStock: filters.inStock,
      isActive: filters.isActive,
    };

    return await apiClient.getProducts(params);
  }

  /**
   * Create a new product
   */
  async createProduct(productData: ProductFormData & { images: string[] }): Promise<Product> {
    const payload = {
      ...productData,
      // Ensure required fields are present
      inStock: productData.stockQuantity > 0,
      rating: 0,
      reviewCount: 0,
      storeName: '', // This will be set by the backend
      storeId: '', // This will be set by the backend from auth context
    };

    return await apiClient.createProduct(payload);
  }

  /**
   * Update an existing product
   */
  async updateProduct(productId: string, productData: Partial<ProductFormData & { images: string[] }>): Promise<Product> {
    const payload = {
      ...productData,
      // Update inStock status based on stockQuantity if it's being updated
      ...(productData.stockQuantity !== undefined && {
        inStock: productData.stockQuantity > 0
      }),
    };

    return await apiClient.updateProduct(productId, payload);
  }

  /**
   * Delete a product
   */
  async deleteProduct(productId: string): Promise<void> {
    return await apiClient.deleteProduct(productId);
  }

  /**
   * Get a single product by ID
   */
  async getProduct(productId: string): Promise<Product> {
    return await apiClient.getProduct(productId);
  }

  /**
   * Upload product images
   */
  async uploadProductImages(files: File[]): Promise<string[]> {
    const uploadPromises = files.map(file => 
      apiClient.uploadFile(file, 'products')
    );
    
    return await Promise.all(uploadPromises);
  }

  /**
   * Perform bulk actions on products
   */
  async performBulkAction(productIds: string[], action: BulkAction): Promise<void> {
    const endpoint = '/products/bulk';
    
    const payload = {
      productIds,
      action: action.type,
      data: action.payload,
    };

    await apiClient.request({
      method: 'PATCH',
      url: endpoint,
      data: payload,
    });
  }

  /**
   * Get product categories
   */
  async getCategories(): Promise<ProductCategory[]> {
    const response = await apiClient.request<ProductCategory[]>({
      method: 'GET',
      url: '/categories',
    });
    
    return response;
  }

  /**
   * Search products with advanced faceted search
   */
  async searchProducts(query: string, filters?: ProductFilters): Promise<ProductSearchResult> {
    const params = {
      q: query,
      ...filters,
    };

    const response = await apiClient.request<ProductSearchResult>({
      method: 'GET',
      url: '/products/search',
      params,
    });

    return response;
  }

  /**
   * Get product statistics for store owner dashboard
   */
  async getProductStats(storeId?: string): Promise<ProductStats> {
    const params = storeId ? { storeId } : {};
    
    const response = await apiClient.request<ProductStats>({
      method: 'GET',
      url: '/products/stats',
      params,
    });

    return response;
  }

  /**
   * Update product inventory
   */
  async updateInventory(updates: InventoryUpdate[]): Promise<void> {
    await apiClient.request({
      method: 'PATCH',
      url: '/products/inventory',
      data: { updates },
    });
  }

  /**
   * Check product availability
   */
  async checkAvailability(productId: string, quantity: number): Promise<boolean> {
    const response = await apiClient.request<{ available: boolean }>({
      method: 'GET',
      url: `/products/${productId}/availability`,
      params: { quantity },
    });

    return response.available;
  }

  /**
   * Get low stock products
   */
  async getLowStockProducts(threshold: number = 10, storeId?: string): Promise<Product[]> {
    const params = {
      threshold,
      ...(storeId && { storeId }),
    };

    const response = await apiClient.request<Product[]>({
      method: 'GET',
      url: '/products/low-stock',
      params,
    });

    return response;
  }

  /**
   * Duplicate a product
   */
  async duplicateProduct(productId: string, modifications?: Partial<ProductFormData>): Promise<Product> {
    const payload = {
      sourceProductId: productId,
      modifications: modifications || {},
    };

    const response = await apiClient.request<Product>({
      method: 'POST',
      url: '/products/duplicate',
      data: payload,
    });

    return response;
  }

  /**
   * Export products to CSV
   */
  async exportProducts(filters?: ProductFilters): Promise<Blob> {
    const params = filters || {};
    
    const response = await apiClient.request({
      method: 'GET',
      url: '/products/export',
      params,
      responseType: 'blob',
    });

    return response as Blob;
  }

  /**
   * Import products from CSV
   */
  async importProducts(file: File, options?: { skipErrors?: boolean; updateExisting?: boolean }): Promise<{
    success: number;
    errors: Array<{ row: number; message: string }>;
  }> {
    const formData = new FormData();
    formData.append('file', file);
    
    if (options) {
      formData.append('options', JSON.stringify(options));
    }

    const response = await apiClient.request<{
      success: number;
      errors: Array<{ row: number; message: string }>;
    }>({
      method: 'POST',
      url: '/products/import',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response;
  }
}

// Create and export a singleton instance
export const productService = new ProductService();
export default productService;