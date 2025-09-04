import { useState, useEffect, useCallback } from 'react';
import {
  Product,
  PaginatedResponse,
  ProductFilters,
  ProductFormData,
  ProductStats,
  BulkAction,
  InventoryUpdate,
} from '../types/product';
import { productService } from '../services/productService';

interface UseProductsState {
  products: Product[];
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
  stats: ProductStats | null;
  selectedProducts: string[];
}

interface UseProductsActions {
  fetchProducts: (filters?: ProductFilters) => Promise<void>;
  createProduct: (data: ProductFormData & { images: string[] }) => Promise<Product>;
  updateProduct: (id: string, data: Partial<ProductFormData & { images: string[] }>) => Promise<Product>;
  deleteProduct: (id: string) => Promise<void>;
  toggleProductSelection: (id: string) => void;
  selectAllProducts: () => void;
  clearSelection: () => void;
  performBulkAction: (action: BulkAction) => Promise<void>;
  updateInventory: (updates: InventoryUpdate[]) => Promise<void>;
  duplicateProduct: (id: string, modifications?: Partial<ProductFormData>) => Promise<Product>;
  fetchProductStats: () => Promise<void>;
  refreshProducts: () => Promise<void>;
  setPage: (page: number) => void;
  setLimit: (limit: number) => void;
}

interface UseProductsReturn extends UseProductsState, UseProductsActions {}

const initialState: UseProductsState = {
  products: [],
  loading: false,
  error: null,
  pagination: {
    page: 1,
    limit: 20,
    total: 0,
    totalPages: 0,
  },
  stats: null,
  selectedProducts: [],
};

export const useProducts = (initialFilters?: ProductFilters): UseProductsReturn => {
  const [state, setState] = useState<UseProductsState>(initialState);
  const [filters, setFilters] = useState<ProductFilters>(initialFilters || {});

  // Fetch products
  const fetchProducts = useCallback(async (newFilters?: ProductFilters) => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      const currentFilters = { ...filters, ...newFilters };
      setFilters(currentFilters);
      
      const response: PaginatedResponse<Product> = await productService.getStoreProducts({
        ...currentFilters,
        page: state.pagination.page,
        limit: state.pagination.limit,
      });
      
      setState(prev => ({
        ...prev,
        products: response.data,
        pagination: response.pagination,
        loading: false,
        selectedProducts: [], // Clear selection on new fetch
      }));
    } catch (error) {
      setState(prev => ({
        ...prev,
        loading: false,
        error: error instanceof Error ? error.message : 'Failed to fetch products',
      }));
    }
  }, [filters, state.pagination.page, state.pagination.limit]);

  // Create product
  const createProduct = useCallback(async (data: ProductFormData & { images: string[] }): Promise<Product> => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      const newProduct = await productService.createProduct(data);
      
      setState(prev => ({
        ...prev,
        products: [newProduct, ...prev.products],
        loading: false,
      }));
      
      return newProduct;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to create product';
      setState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      throw error;
    }
  }, []);

  // Update product
  const updateProduct = useCallback(async (id: string, data: Partial<ProductFormData & { images: string[] }>): Promise<Product> => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      const updatedProduct = await productService.updateProduct(id, data);
      
      setState(prev => ({
        ...prev,
        products: prev.products.map(p => p.id === id ? updatedProduct : p),
        loading: false,
      }));
      
      return updatedProduct;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update product';
      setState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      throw error;
    }
  }, []);

  // Delete product
  const deleteProduct = useCallback(async (id: string): Promise<void> => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      await productService.deleteProduct(id);
      
      setState(prev => ({
        ...prev,
        products: prev.products.filter(p => p.id !== id),
        selectedProducts: prev.selectedProducts.filter(selectedId => selectedId !== id),
        loading: false,
      }));
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to delete product';
      setState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      throw error;
    }
  }, []);

  // Toggle product selection
  const toggleProductSelection = useCallback((id: string) => {
    setState(prev => ({
      ...prev,
      selectedProducts: prev.selectedProducts.includes(id)
        ? prev.selectedProducts.filter(selectedId => selectedId !== id)
        : [...prev.selectedProducts, id],
    }));
  }, []);

  // Select all products
  const selectAllProducts = useCallback(() => {
    setState(prev => ({
      ...prev,
      selectedProducts: prev.products.map(p => p.id),
    }));
  }, []);

  // Clear selection
  const clearSelection = useCallback(() => {
    setState(prev => ({
      ...prev,
      selectedProducts: [],
    }));
  }, []);

  // Perform bulk action
  const performBulkAction = useCallback(async (action: BulkAction): Promise<void> => {
    if (state.selectedProducts.length === 0) {
      throw new Error('No products selected');
    }

    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      await productService.performBulkAction(state.selectedProducts, action);
      
      // Refresh products after bulk action
      await fetchProducts();
      
      setState(prev => ({
        ...prev,
        selectedProducts: [],
        loading: false,
      }));
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to perform bulk action';
      setState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      throw error;
    }
  }, [state.selectedProducts, fetchProducts]);

  // Update inventory
  const updateInventory = useCallback(async (updates: InventoryUpdate[]): Promise<void> => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      await productService.updateInventory(updates);
      
      // Update local state
      setState(prev => ({
        ...prev,
        products: prev.products.map(product => {
          const update = updates.find(u => u.productId === product.id);
          if (update) {
            const newQuantity = update.operation === 'set' 
              ? update.stockQuantity
              : update.operation === 'add'
              ? product.stockQuantity + update.stockQuantity
              : product.stockQuantity - update.stockQuantity;
            
            return {
              ...product,
              stockQuantity: Math.max(0, newQuantity),
              inStock: newQuantity > 0,
            };
          }
          return product;
        }),
        loading: false,
      }));
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update inventory';
      setState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      throw error;
    }
  }, []);

  // Duplicate product
  const duplicateProduct = useCallback(async (id: string, modifications?: Partial<ProductFormData>): Promise<Product> => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      const duplicatedProduct = await productService.duplicateProduct(id, modifications);
      
      setState(prev => ({
        ...prev,
        products: [duplicatedProduct, ...prev.products],
        loading: false,
      }));
      
      return duplicatedProduct;
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to duplicate product';
      setState(prev => ({
        ...prev,
        loading: false,
        error: errorMessage,
      }));
      throw error;
    }
  }, []);

  // Fetch product stats
  const fetchProductStats = useCallback(async (): Promise<void> => {
    try {
      const stats = await productService.getProductStats();
      setState(prev => ({ ...prev, stats }));
    } catch (error) {
      console.warn('Failed to fetch product stats:', error);
    }
  }, []);

  // Refresh products
  const refreshProducts = useCallback(async (): Promise<void> => {
    await fetchProducts(filters);
  }, [fetchProducts, filters]);

  // Set page
  const setPage = useCallback((page: number) => {
    setState(prev => ({
      ...prev,
      pagination: { ...prev.pagination, page },
    }));
  }, []);

  // Set limit
  const setLimit = useCallback((limit: number) => {
    setState(prev => ({
      ...prev,
      pagination: { ...prev.pagination, limit, page: 1 }, // Reset to page 1 when changing limit
    }));
  }, []);

  // Initial fetch
  useEffect(() => {
    fetchProducts();
  }, [state.pagination.page, state.pagination.limit]);

  // Fetch stats on mount
  useEffect(() => {
    fetchProductStats();
  }, [fetchProductStats]);

  return {
    ...state,
    fetchProducts,
    createProduct,
    updateProduct,
    deleteProduct,
    toggleProductSelection,
    selectAllProducts,
    clearSelection,
    performBulkAction,
    updateInventory,
    duplicateProduct,
    fetchProductStats,
    refreshProducts,
    setPage,
    setLimit,
  };
};

export default useProducts;