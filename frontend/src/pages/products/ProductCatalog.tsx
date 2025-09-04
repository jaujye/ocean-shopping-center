import React, { useState, useCallback, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { Product } from '../../types';
import { ProductFilters as FiltersType } from '../../types/product';
import ProductFilters from '../../components/products/ProductFilters';
import ProductGrid from '../../components/products/ProductGrid';
import Modal from '../../components/ui/Modal';
import { cn } from '../../utils/cn';

// Quick View Modal Component
interface ProductQuickViewProps {
  product: Product | null;
  isOpen: boolean;
  onClose: () => void;
  onAddToCart: (productId: string) => void;
}

const ProductQuickView: React.FC<ProductQuickViewProps> = ({
  product,
  isOpen,
  onClose,
  onAddToCart,
}) => {
  const navigate = useNavigate();

  if (!product) return null;

  const handleViewDetails = () => {
    navigate(`/products/${product.id}`);
    onClose();
  };

  const discountPercentage = product.originalPrice 
    ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
    : null;

  return (
    <Modal isOpen={isOpen} onClose={onClose} size="lg">
      <div className="max-h-[80vh] overflow-y-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Image Section */}
          <div className="space-y-4">
            <div className="relative aspect-square bg-gray-100 rounded-lg overflow-hidden">
              {product.images.length > 0 ? (
                <img
                  src={product.images[0]}
                  alt={product.name}
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="flex items-center justify-center h-full text-gray-400">
                  No Image Available
                </div>
              )}
              {discountPercentage && discountPercentage > 0 && (
                <div className="absolute top-4 left-4 bg-red-500 text-white px-2 py-1 rounded-md text-sm font-semibold">
                  -{discountPercentage}%
                </div>
              )}
            </div>
            
            {product.images.length > 1 && (
              <div className="grid grid-cols-4 gap-2">
                {product.images.slice(1, 5).map((image, index) => (
                  <div key={index} className="aspect-square bg-gray-100 rounded overflow-hidden">
                    <img
                      src={image}
                      alt={`${product.name} ${index + 2}`}
                      className="w-full h-full object-cover"
                    />
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Details Section */}
          <div className="space-y-4">
            <div>
              <p className="text-sm text-gray-500 mb-1">{product.storeName}</p>
              <h2 className="text-2xl font-bold text-gray-900 mb-2">{product.name}</h2>
              
              {/* Rating */}
              <div className="flex items-center space-x-2 mb-3">
                <div className="flex text-yellow-400">
                  {[...Array(5)].map((_, i) => (
                    <svg
                      key={i}
                      className={cn(
                        "w-4 h-4",
                        i < Math.floor(product.rating) ? "fill-current" : "fill-gray-300"
                      )}
                      viewBox="0 0 20 20"
                    >
                      <path d="M9.049 2.927c.3-.921 1.603-.921 1.902 0l1.518 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
                    </svg>
                  ))}
                </div>
                <span className="text-sm text-gray-600">
                  {product.rating.toFixed(1)} ({product.reviewCount} reviews)
                </span>
              </div>
            </div>

            {/* Price */}
            <div className="flex items-center space-x-3">
              <span className="text-3xl font-bold text-ocean-600">
                ${product.price.toFixed(2)}
              </span>
              {product.originalPrice && product.originalPrice > product.price && (
                <span className="text-lg text-gray-500 line-through">
                  ${product.originalPrice.toFixed(2)}
                </span>
              )}
            </div>

            {/* Stock Status */}
            <div className={cn(
              "text-sm font-medium",
              product.inStock ? "text-green-600" : "text-red-600"
            )}>
              {product.inStock 
                ? `In Stock (${product.stockQuantity} available)` 
                : "Out of Stock"
              }
            </div>

            {/* Description */}
            <div className="prose prose-sm max-w-none">
              <p className="text-gray-600 line-clamp-4">{product.description}</p>
            </div>

            {/* Tags */}
            {product.tags.length > 0 && (
              <div className="flex flex-wrap gap-2">
                {product.tags.map((tag) => (
                  <span
                    key={tag}
                    className="px-2 py-1 bg-gray-100 text-gray-600 text-sm rounded-full"
                  >
                    {tag}
                  </span>
                ))}
              </div>
            )}

            {/* Specifications */}
            {product.specifications && Object.keys(product.specifications).length > 0 && (
              <div className="space-y-2">
                <h4 className="font-medium text-gray-900">Specifications</h4>
                <div className="grid grid-cols-1 gap-1 text-sm">
                  {Object.entries(product.specifications).slice(0, 5).map(([key, value]) => (
                    <div key={key} className="flex justify-between">
                      <span className="text-gray-600">{key}:</span>
                      <span className="text-gray-900">{value}</span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Action Buttons */}
            <div className="space-y-3 pt-4 border-t border-gray-200">
              <button
                onClick={() => onAddToCart(product.id)}
                disabled={!product.inStock}
                className={cn(
                  "w-full py-3 px-4 rounded-lg font-medium transition-colors duration-200",
                  product.inStock
                    ? "bg-ocean-500 hover:bg-ocean-600 text-white"
                    : "bg-gray-300 text-gray-500 cursor-not-allowed"
                )}
              >
                {product.inStock ? "Add to Cart" : "Out of Stock"}
              </button>
              
              <button
                onClick={handleViewDetails}
                className="w-full py-3 px-4 border-2 border-ocean-500 text-ocean-500 rounded-lg font-medium hover:bg-ocean-50 transition-colors duration-200"
              >
                View Full Details
              </button>
            </div>
          </div>
        </div>
      </div>
    </Modal>
  );
};

const ProductCatalog: React.FC = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isQuickViewOpen, setIsQuickViewOpen] = useState(false);

  // Initialize filters from URL params
  const [filters, setFilters] = useState<FiltersType>(() => {
    return {
      search: searchParams.get('search') || undefined,
      category: searchParams.get('category') || undefined,
      subcategory: searchParams.get('subcategory') || undefined,
      minPrice: searchParams.get('minPrice') ? parseFloat(searchParams.get('minPrice')!) : undefined,
      maxPrice: searchParams.get('maxPrice') ? parseFloat(searchParams.get('maxPrice')!) : undefined,
      sortBy: (searchParams.get('sortBy') as FiltersType['sortBy']) || 'createdAt',
      sortOrder: (searchParams.get('sortOrder') as FiltersType['sortOrder']) || 'desc',
      inStock: searchParams.get('inStock') === 'true' ? true : undefined,
      tags: searchParams.get('tags') ? searchParams.get('tags')!.split(',') : undefined,
    };
  });

  // Update URL params when filters change
  useEffect(() => {
    const params = new URLSearchParams();
    
    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        if (Array.isArray(value)) {
          if (value.length > 0) {
            params.set(key, value.join(','));
          }
        } else {
          params.set(key, value.toString());
        }
      }
    });

    setSearchParams(params, { replace: true });
  }, [filters, setSearchParams]);

  // Handle filter changes
  const handleFiltersChange = useCallback((newFilters: FiltersType) => {
    setFilters(newFilters);
  }, []);

  // Handle apply filters (mainly for mobile)
  const handleApplyFilters = useCallback(() => {
    // Filters are already applied through handleFiltersChange
    // This is mainly for UI feedback on mobile
  }, []);

  // Handle clear filters
  const handleClearFilters = useCallback(() => {
    const clearedFilters: FiltersType = {
      sortBy: 'createdAt',
      sortOrder: 'desc',
    };
    setFilters(clearedFilters);
  }, []);

  // Handle quick view
  const handleQuickView = useCallback((product: Product) => {
    setSelectedProduct(product);
    setIsQuickViewOpen(true);
  }, []);

  // Handle add to wishlist
  const handleAddToWishlist = useCallback((productId: string) => {
    // TODO: Implement wishlist functionality
    console.log('Add to wishlist:', productId);
  }, []);

  // Handle add to cart from quick view
  const handleAddToCart = useCallback((productId: string) => {
    // TODO: Implement add to cart functionality
    console.log('Add to cart:', productId);
  }, []);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="container mx-auto px-4 py-8">
        {/* Page Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">Product Catalog</h1>
          <p className="text-gray-600">Discover amazing products from our marketplace</p>
        </div>

        {/* Main Content */}
        <div className="flex flex-col lg:flex-row gap-8">
          {/* Filters Sidebar */}
          <div className="lg:w-64 flex-shrink-0">
            <ProductFilters
              filters={filters}
              onFiltersChange={handleFiltersChange}
              onApplyFilters={handleApplyFilters}
              onClearFilters={handleClearFilters}
            />
          </div>

          {/* Products Grid */}
          <div className="flex-1">
            <ProductGrid
              filters={filters}
              onQuickView={handleQuickView}
              onAddToWishlist={handleAddToWishlist}
            />
          </div>
        </div>
      </div>

      {/* Quick View Modal */}
      <ProductQuickView
        product={selectedProduct}
        isOpen={isQuickViewOpen}
        onClose={() => setIsQuickViewOpen(false)}
        onAddToCart={handleAddToCart}
      />
    </div>
  );
};

export default ProductCatalog;