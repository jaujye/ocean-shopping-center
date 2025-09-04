import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { Product } from '../../types';
import Card from '../ui/Card';
import Button from '../ui/Button';
import { cn } from '../../utils/cn';
import { cartService } from '../../services/cartService';

// Icons - using simple SVGs since no icon library is imported
const HeartIcon = ({ filled = false, className = "" }: { filled?: boolean; className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill={filled ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
  </svg>
);

const ShoppingCartIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4m0 0L7 13m0 0l-2.5 5M7 13l2.5 5m6-5v5a2 2 0 01-2 2H9a2 2 0 01-2-2v-5m6-5V4a2 2 0 00-2-2H9a2 2 0 00-2 2v4.01" />
  </svg>
);

const StarIcon = ({ filled = false, className = "" }: { filled?: boolean; className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill={filled ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
  </svg>
);

const EyeIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
  </svg>
);

interface ProductCardProps {
  product: Product;
  onQuickView?: (product: Product) => void;
  onAddToWishlist?: (productId: string) => void;
  className?: string;
  showQuickActions?: boolean;
  compact?: boolean;
}

const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onQuickView,
  onAddToWishlist,
  className,
  showQuickActions = true,
  compact = false,
}) => {
  const [isAddingToCart, setIsAddingToCart] = useState(false);
  const [isInWishlist, setIsInWishlist] = useState(false);
  const [currentImageIndex, setCurrentImageIndex] = useState(0);

  // Calculate discount percentage
  const discountPercentage = product.originalPrice 
    ? Math.round(((product.originalPrice - product.price) / product.originalPrice) * 100)
    : null;

  // Handle add to cart
  const handleAddToCart = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    if (!product.inStock) return;
    
    setIsAddingToCart(true);
    try {
      await cartService.addToCart(product.id, 1);
      // Could add a toast notification here
    } catch (error) {
      console.error('Failed to add to cart:', error);
      // Could show error toast
    } finally {
      setIsAddingToCart(false);
    }
  };

  // Handle wishlist toggle
  const handleWishlistToggle = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    
    setIsInWishlist(!isInWishlist);
    onAddToWishlist?.(product.id);
  };

  // Handle quick view
  const handleQuickView = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    onQuickView?.(product);
  };

  // Handle image navigation
  const nextImage = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setCurrentImageIndex((prev) => (prev + 1) % product.images.length);
  };

  const prevImage = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setCurrentImageIndex((prev) => (prev - 1 + product.images.length) % product.images.length);
  };

  // Render star rating
  const renderStarRating = () => {
    const stars = [];
    const fullStars = Math.floor(product.rating);
    const hasHalfStar = product.rating % 1 !== 0;

    for (let i = 0; i < 5; i++) {
      stars.push(
        <StarIcon
          key={i}
          filled={i < fullStars}
          className={cn(
            "text-yellow-400",
            i < fullStars ? "text-yellow-400" : "text-gray-300"
          )}
        />
      );
    }

    return (
      <div className="flex items-center space-x-1">
        <div className="flex">{stars}</div>
        <span className="text-sm text-gray-600 ml-1">
          {product.rating.toFixed(1)} ({product.reviewCount})
        </span>
      </div>
    );
  };

  return (
    <Link to={`/products/${product.id}`} className={cn("group block", className)}>
      <Card
        className={cn(
          "relative overflow-hidden transition-all duration-300 group-hover:shadow-xl",
          !compact && "h-full",
          !product.inStock && "opacity-75"
        )}
        padding="none"
        shadow="md"
        hover
      >
        {/* Image Section */}
        <div className={cn("relative overflow-hidden", compact ? "h-48" : "h-64")}>
          {product.images.length > 0 ? (
            <>
              <img
                src={product.images[currentImageIndex]}
                alt={product.name}
                className="w-full h-full object-cover transition-transform duration-500 group-hover:scale-110"
              />
              
              {/* Image Navigation */}
              {product.images.length > 1 && (
                <>
                  <button
                    onClick={prevImage}
                    className="absolute left-2 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-700 rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
                    </svg>
                  </button>
                  <button
                    onClick={nextImage}
                    className="absolute right-2 top-1/2 -translate-y-1/2 bg-white/80 hover:bg-white text-gray-700 rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity duration-200"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                    </svg>
                  </button>
                </>
              )}
            </>
          ) : (
            <div className="w-full h-full bg-gray-200 flex items-center justify-center">
              <span className="text-gray-400 text-sm">No Image</span>
            </div>
          )}

          {/* Discount Badge */}
          {discountPercentage && discountPercentage > 0 && (
            <div className="absolute top-2 left-2 bg-red-500 text-white px-2 py-1 rounded-md text-xs font-semibold">
              -{discountPercentage}%
            </div>
          )}

          {/* Stock Status */}
          {!product.inStock && (
            <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
              <span className="text-white font-semibold bg-red-500 px-3 py-1 rounded-md">
                Out of Stock
              </span>
            </div>
          )}

          {/* Quick Actions */}
          {showQuickActions && (
            <div className="absolute top-2 right-2 flex flex-col space-y-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
              <button
                onClick={handleWishlistToggle}
                className={cn(
                  "p-2 rounded-full shadow-md transition-colors duration-200",
                  isInWishlist
                    ? "bg-red-500 text-white"
                    : "bg-white text-gray-600 hover:text-red-500"
                )}
              >
                <HeartIcon filled={isInWishlist} />
              </button>
              
              {onQuickView && (
                <button
                  onClick={handleQuickView}
                  className="p-2 bg-white text-gray-600 hover:text-ocean-500 rounded-full shadow-md transition-colors duration-200"
                >
                  <EyeIcon />
                </button>
              )}
            </div>
          )}
        </div>

        {/* Content Section */}
        <div className="p-4">
          {/* Store Name */}
          <div className="text-xs text-gray-500 mb-1">{product.storeName}</div>

          {/* Product Name */}
          <h3 className="font-semibold text-gray-900 mb-2 line-clamp-2 group-hover:text-ocean-600 transition-colors duration-200">
            {product.name}
          </h3>

          {/* Rating */}
          {!compact && (
            <div className="mb-2">
              {renderStarRating()}
            </div>
          )}

          {/* Price */}
          <div className="flex items-center space-x-2 mb-3">
            <span className="text-lg font-bold text-ocean-600">
              ${product.price.toFixed(2)}
            </span>
            {product.originalPrice && product.originalPrice > product.price && (
              <span className="text-sm text-gray-500 line-through">
                ${product.originalPrice.toFixed(2)}
              </span>
            )}
          </div>

          {/* Tags */}
          {!compact && product.tags.length > 0 && (
            <div className="flex flex-wrap gap-1 mb-3">
              {product.tags.slice(0, 3).map((tag) => (
                <span
                  key={tag}
                  className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-full"
                >
                  {tag}
                </span>
              ))}
              {product.tags.length > 3 && (
                <span className="px-2 py-1 bg-gray-100 text-gray-600 text-xs rounded-full">
                  +{product.tags.length - 3}
                </span>
              )}
            </div>
          )}

          {/* Add to Cart Button */}
          <Button
            onClick={handleAddToCart}
            disabled={!product.inStock || isAddingToCart}
            loading={isAddingToCart}
            loadingText="Adding..."
            variant={product.inStock ? "primary" : "outline"}
            size="sm"
            fullWidth
            leftIcon={<ShoppingCartIcon />}
          >
            {product.inStock ? "Add to Cart" : "Out of Stock"}
          </Button>
        </div>
      </Card>
    </Link>
  );
};

export default ProductCard;