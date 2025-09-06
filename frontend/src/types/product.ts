import { Product } from './index';

export interface ProductFilters {
  search?: string;
  category?: string;
  subcategory?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
  isActive?: boolean;
  tags?: string[];
  sortBy?: 'name' | 'price' | 'createdAt' | 'updatedAt' | 'rating' | 'stockQuantity';
  sortOrder?: 'asc' | 'desc';
}

export interface ProductFormData {
  name: string;
  description: string;
  price: number;
  originalPrice?: number;
  category: string;
  subcategory?: string;
  stockQuantity: number;
  tags: string[];
  specifications?: Record<string, string>;
  isActive: boolean;
}

export interface ProductImageUpload {
  file: File;
  preview: string;
  isUploading?: boolean;
  uploadProgress?: number;
  error?: string;
}

export interface BulkAction {
  type: 'activate' | 'deactivate' | 'delete' | 'updateCategory' | 'updatePrice';
  payload?: any;
}

export interface ProductCategory {
  id: string;
  name: string;
  subcategories?: ProductSubcategory[];
}

export interface ProductSubcategory {
  id: string;
  name: string;
  categoryId: string;
}

export interface ProductValidationErrors {
  name?: string;
  description?: string;
  price?: string;
  category?: string;
  stockQuantity?: string;
  images?: string;
  general?: string;
}

export interface ProductStats {
  totalProducts: number;
  activeProducts: number;
  inactiveProducts: number;
  outOfStock: number;
  lowStock: number;
  totalValue: number;
}

export interface InventoryUpdate {
  productId: string;
  stockQuantity: number;
  operation: 'set' | 'add' | 'subtract';
}

export interface ProductSearchResult {
  products: Product[];
  totalCount: number;
  facets: {
    categories: Array<{ name: string; count: number }>;
    priceRanges: Array<{ min: number; max: number; count: number }>;
    tags: Array<{ name: string; count: number }>;
  };
}

// Re-export Product interface from main types for convenience
export type { Product, PaginatedResponse, ApiResponse } from './index';