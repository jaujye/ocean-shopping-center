import { useQuery, useMutation, useQueryClient, useInfiniteQuery } from '@tanstack/react-query';
import { productService } from '../../services/productService';
import { Product, PaginatedResponse } from '../../types';
import { ProductFilters, ProductFormData, BulkAction, ProductCategory, ProductStats } from '../../types/product';

// Query Keys - Updated to match backend API
export const productKeys = {
  all: ['products'] as const,
  lists: () => [...productKeys.all, 'list'] as const,
  list: (filters: any) => [...productKeys.lists(), filters] as const,
  details: () => [...productKeys.all, 'detail'] as const,
  detail: (id: string) => [...productKeys.details(), id] as const,
  categories: () => [...productKeys.all, 'categories'] as const,
  featured: (limit?: number) => [...productKeys.all, 'featured', limit] as const,
  recommendations: (limit?: number) => [...productKeys.all, 'recommendations', limit] as const,
  related: (productId: string, limit?: number) => [...productKeys.all, 'related', productId, limit] as const,
  availability: (productId: string, quantity: number) => [...productKeys.all, 'availability', productId, quantity] as const,
  lowStock: (storeId?: string) => [...productKeys.all, 'lowStock', storeId] as const,
  search: (query: string, filters?: any) => [...productKeys.all, 'search', query, filters] as const,
  storeProducts: (storeId: string, filters?: any) => [...productKeys.all, 'store', storeId, filters] as const,
  categoryProducts: (categoryId: string, filters?: any) => [...productKeys.all, 'category', categoryId, filters] as const,
};

// Fetch all products with filters (matches /api/products)
export const useProducts = (filters?: {
  query?: string;
  storeId?: string;
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  featured?: boolean;
  digital?: boolean;
  hasDiscount?: boolean;
  minRating?: number;
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}) => {
  return useQuery({
    queryKey: productKeys.list(filters),
    queryFn: () => productService.getStoreProducts(filters as any),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Infinite query for product loading (matches /api/products)
export const useInfiniteProducts = (filters?: {
  query?: string;
  storeId?: string;
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  featured?: boolean;
  digital?: boolean;
  hasDiscount?: boolean;
  minRating?: number;
  sortBy?: string;
  sortDirection?: string;
  size?: number;
}) => {
  return useInfiniteQuery({
    queryKey: ['products', 'infinite', filters],
    queryFn: ({ pageParam = 0 }) => 
      productService.getStoreProducts({ ...filters, page: pageParam, size: filters?.size || 20 } as any),
    getNextPageParam: (lastPage: PaginatedResponse<Product>) => {
      if (lastPage.pagination.page < lastPage.pagination.totalPages - 1) {
        return lastPage.pagination.page + 1;
      }
      return undefined;
    },
    initialPageParam: 0,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Fetch single product (matches /api/products/{id})
export const useProduct = (productId: string) => {
  return useQuery({
    queryKey: productKeys.detail(productId),
    queryFn: () => productService.getProduct(productId),
    enabled: !!productId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Fetch product categories
export const useProductCategories = () => {
  return useQuery({
    queryKey: productKeys.categories(),
    queryFn: () => productService.getCategories(),
    staleTime: 1000 * 60 * 30, // 30 minutes - categories don't change often
  });
};

// Get low stock products (matches /api/products/low-stock)  
export const useLowStockProductsBackend = (storeId?: string) => {
  return useQuery({
    queryKey: productKeys.lowStock(storeId),
    queryFn: () => productService.getLowStockProducts(10, storeId),
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Search products (matches /api/products/search)
export const useProductSearch = (query: string, filters?: {
  storeId?: string;
  categoryId?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  minRating?: number;
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}, enabled = true) => {
  return useQuery({
    queryKey: productKeys.search(query, filters),
    queryFn: () => productService.searchProducts(query, filters as any),
    enabled: enabled && !!query && query.length > 2,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Create product mutation
export const useCreateProduct = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (productData: ProductFormData & { images: string[] }) => 
      productService.createProduct(productData),
    onSuccess: () => {
      // Invalidate and refetch product lists
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
      // Note: Stats method removed from productKeys
    },
  });
};

// Update product mutation
export const useUpdateProduct = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ productId, data }: { 
      productId: string; 
      data: Partial<ProductFormData & { images: string[] }> 
    }) => productService.updateProduct(productId, data),
    onSuccess: (updatedProduct) => {
      // Update the specific product in cache
      queryClient.setQueryData(
        productKeys.detail(updatedProduct.id), 
        updatedProduct
      );
      // Invalidate lists to refetch
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
      // Note: Stats method removed from productKeys
    },
  });
};

// Delete product mutation
export const useDeleteProduct = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (productId: string) => productService.deleteProduct(productId),
    onSuccess: (_, productId) => {
      // Remove from cache
      queryClient.removeQueries({ queryKey: productKeys.detail(productId) });
      // Invalidate lists
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
      // Note: Stats method removed from productKeys
    },
  });
};

// Bulk action mutation
export const useBulkProductAction = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ productIds, action }: { productIds: string[]; action: BulkAction }) => 
      productService.performBulkAction(productIds, action),
    onSuccess: () => {
      // Invalidate all product queries
      queryClient.invalidateQueries({ queryKey: productKeys.all });
      // Note: Stats method removed from productKeys
    },
  });
};

// Upload product images mutation
export const useUploadProductImages = () => {
  return useMutation({
    mutationFn: (files: File[]) => productService.uploadProductImages(files),
  });
};

// Update inventory mutation
export const useUpdateInventory = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: productService.updateInventory,
    onSuccess: () => {
      // Invalidate product lists and low stock queries
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
      queryClient.invalidateQueries({ queryKey: ['lowStock'] });
    },
  });
};

// Additional hooks to match backend API endpoints

// Fetch product by slug (matches /api/products/store/{storeId}/slug/{slug})
export const useProductBySlug = (slug: string, storeId: string) => {
  return useQuery({
    queryKey: productKeys.detail(`${storeId}-${slug}`),
    queryFn: () => Promise.resolve({} as Product), // Placeholder - method doesn't exist in service
    enabled: !!slug && !!storeId,
    staleTime: 1000 * 60 * 10, // 10 minutes
  });
};

// Fetch featured products (matches /api/products/featured)
export const useFeaturedProducts = (limit: number = 10) => {
  return useQuery({
    queryKey: productKeys.featured(limit),
    queryFn: () => Promise.resolve([] as Product[]), // Placeholder - method doesn't exist in service
    staleTime: 1000 * 60 * 15, // 15 minutes
  });
};

// Fetch product recommendations for user (matches /api/products/recommendations)
export const useRecommendations = (limit: number = 10) => {
  return useQuery({
    queryKey: productKeys.recommendations(limit),
    queryFn: () => Promise.resolve([] as Product[]), // Placeholder - method doesn't exist in service
    staleTime: 1000 * 60 * 20, // 20 minutes
  });
};

// Fetch related products (matches /api/products/{id}/related)
export const useRelatedProducts = (productId: string, limit: number = 10) => {
  return useQuery({
    queryKey: productKeys.related(productId, limit),
    queryFn: () => Promise.resolve([] as Product[]), // Placeholder - method doesn't exist in service
    enabled: !!productId,
    staleTime: 1000 * 60 * 10, // 10 minutes
  });
};

// Fetch products by store (matches /api/products/store/{storeId})
export const useStoreProducts = (storeId: string, filters?: {
  categoryId?: string;
  featured?: boolean;
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}) => {
  return useInfiniteQuery({
    queryKey: productKeys.storeProducts(storeId, filters),
    queryFn: ({ pageParam = 0 }) =>
      productService.getStoreProducts({ storeId, ...filters, page: pageParam, size: filters?.size || 20 } as any),
    getNextPageParam: (lastPage: PaginatedResponse<Product>) => {
      if (lastPage.pagination.page < lastPage.pagination.totalPages - 1) {
        return lastPage.pagination.page + 1;
      }
      return undefined;
    },
    initialPageParam: 0,
    enabled: !!storeId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Fetch products by category (matches /api/products/category/{categoryId})
export const useCategoryProducts = (categoryId: string, filters?: {
  storeId?: string;
  minPrice?: number;
  maxPrice?: number;
  sortBy?: string;
  sortDirection?: string;
  page?: number;
  size?: number;
}) => {
  return useInfiniteQuery({
    queryKey: productKeys.categoryProducts(categoryId, filters),
    queryFn: ({ pageParam = 0 }) =>
      productService.getStoreProducts({ categoryId, ...filters, page: pageParam, size: filters?.size || 20 } as any),
    getNextPageParam: (lastPage: PaginatedResponse<Product>) => {
      if (lastPage.pagination.page < lastPage.pagination.totalPages - 1) {
        return lastPage.pagination.page + 1;
      }
      return undefined;
    },
    initialPageParam: 0,
    enabled: !!categoryId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Check product availability (matches /api/products/{id}/availability)
export const useProductAvailability = (productId: string, quantity: number = 1) => {
  return useQuery({
    queryKey: productKeys.availability(productId, quantity),
    queryFn: () => productService.checkAvailability(productId, quantity),
    enabled: !!productId,
    staleTime: 1000 * 30, // 30 seconds
  });
};

// Duplicate product mutation
export const useDuplicateProduct = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ productId, modifications }: { 
      productId: string; 
      modifications?: Partial<ProductFormData> 
    }) => productService.duplicateProduct(productId, modifications),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: productKeys.lists() });
    },
  });
};

// Export products mutation
export const useExportProducts = () => {
  return useMutation({
    mutationFn: (filters?: ProductFilters) => productService.exportProducts(filters),
  });
};

// Import products mutation
export const useImportProducts = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ file, options }: { 
      file: File; 
      options?: { skipErrors?: boolean; updateExisting?: boolean } 
    }) => productService.importProducts(file, options),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: productKeys.all });
      // Note: Stats method removed from productKeys
    },
  });
};