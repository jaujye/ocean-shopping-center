import React, { useState } from 'react';
import { CartItem } from '../../types';
import { cn } from '../../utils/cn';
import Button from '../ui/Button';
import LoadingSpinner from '../ui/LoadingSpinner';

// Icons
const TrashIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
  </svg>
);

const HeartIcon = ({ className = "", filled = false }: { className?: string; filled?: boolean }) => (
  <svg className={cn("w-4 h-4", className)} fill={filled ? "currentColor" : "none"} stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z" />
  </svg>
);

const MinusIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 12H4" />
  </svg>
);

const PlusIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
  </svg>
);

const GiftIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v13m0-13V6a2 2 0 112 2h-2zm0 0V5.5A2.5 2.5 0 109.5 8H12zm-7 4h14M5 12a2 2 0 110-4h14a2 2 0 110 4M5 12v7a2 2 0 002 2h10a2 2 0 002-2v-7" />
  </svg>
);

const ExclamationTriangleIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.732 16.5c-.77.833.192 2.5 1.732 2.5z" />
  </svg>
);

interface CartItemCardProps {
  item: CartItem;
  onUpdateQuantity: (itemId: string, quantity: number) => Promise<void>;
  onRemove: (itemId: string) => Promise<void>;
  onMoveToWishlist: (itemId: string) => Promise<void>;
  isUpdating?: boolean;
  showWishlistOption?: boolean;
  compact?: boolean;
  className?: string;
}

const CartItemCard: React.FC<CartItemCardProps> = ({
  item,
  onUpdateQuantity,
  onRemove,
  onMoveToWishlist,
  isUpdating = false,
  showWishlistOption = true,
  compact = false,
  className
}) => {
  const [isQuantityLoading, setIsQuantityLoading] = useState(false);
  const [isRemoving, setIsRemoving] = useState(false);
  const [isMovingToWishlist, setIsMovingToWishlist] = useState(false);

  const handleQuantityChange = async (newQuantity: number) => {
    if (newQuantity < 1 || isQuantityLoading) return;
    
    setIsQuantityLoading(true);
    try {
      await onUpdateQuantity(item.id, newQuantity);
    } catch (error) {
      console.error('Failed to update quantity:', error);
    } finally {
      setIsQuantityLoading(false);
    }
  };

  const handleRemove = async () => {
    if (isRemoving) return;
    
    setIsRemoving(true);
    try {
      await onRemove(item.id);
    } catch (error) {
      console.error('Failed to remove item:', error);
      setIsRemoving(false);
    }
  };

  const handleMoveToWishlist = async () => {
    if (isMovingToWishlist) return;
    
    setIsMovingToWishlist(true);
    try {
      await onMoveToWishlist(item.id);
    } catch (error) {
      console.error('Failed to move to wishlist:', error);
      setIsMovingToWishlist(false);
    }
  };

  const isLoading = isUpdating || isQuantityLoading || isRemoving || isMovingToWishlist;
  const maxQuantity = item.product.stockQuantity;
  const isOutOfStock = maxQuantity === 0;
  const hasLimitedStock = maxQuantity < item.quantity;
  const hasDiscount = item.product.originalPrice && item.product.originalPrice > item.product.price;
  const discountPercentage = hasDiscount 
    ? Math.round((1 - item.product.price / item.product.originalPrice!) * 100)
    : 0;

  return (
    <div className={cn(
      "bg-white rounded-lg border border-gray-200 p-4 transition-all duration-200",
      isLoading && "opacity-75",
      compact ? "space-y-2" : "space-y-3",
      className
    )}>
      <div className="flex space-x-4">
        {/* Product Image */}
        <div className="flex-shrink-0">
          <div className="relative">
            <img
              src={item.product.images[0] || '/placeholder-image.jpg'}
              alt={item.product.name}
              className={cn(
                "object-cover rounded-lg",
                compact ? "w-16 h-16" : "w-20 h-20"
              )}
            />
            {item.isGift && (
              <div className="absolute -top-2 -right-2 bg-red-500 text-white rounded-full p-1">
                <GiftIcon className="w-3 h-3" />
              </div>
            )}
            {hasDiscount && (
              <div className="absolute -top-2 -left-2 bg-red-500 text-white text-xs font-bold rounded-full px-2 py-1">
                -{discountPercentage}%
              </div>
            )}
          </div>
        </div>

        {/* Product Details */}
        <div className="flex-1 min-w-0">
          <div className="flex justify-between items-start">
            <div className="min-w-0 flex-1">
              <h4 className={cn(
                "font-medium text-gray-900 truncate",
                compact ? "text-sm" : "text-base"
              )}>
                {item.product.name}
              </h4>
              
              <p className="text-xs text-gray-500 mt-1">
                by {item.product.storeName}
              </p>

              {/* Selected Options */}
              {item.selectedOptions && Object.keys(item.selectedOptions).length > 0 && (
                <div className="mt-1">
                  {Object.entries(item.selectedOptions).map(([key, value]) => (
                    <span key={key} className="inline-block text-xs text-gray-500 mr-2">
                      {key}: {value}
                    </span>
                  ))}
                </div>
              )}

              {/* Gift Message */}
              {item.isGift && item.giftMessage && (
                <div className="mt-1">
                  <span className="inline-flex items-center text-xs text-purple-600">
                    <GiftIcon className="w-3 h-3 mr-1" />
                    Gift: {item.giftMessage}
                  </span>
                </div>
              )}

              {/* Stock Warning */}
              {(isOutOfStock || hasLimitedStock) && (
                <div className="mt-1">
                  <span className="inline-flex items-center text-xs text-red-600">
                    <ExclamationTriangleIcon className="w-3 h-3 mr-1" />
                    {isOutOfStock ? 'Out of stock' : `Only ${maxQuantity} left`}
                  </span>
                </div>
              )}
            </div>

            {/* Price */}
            <div className="text-right ml-4">
              <div className="flex flex-col items-end">
                <span className={cn(
                  "font-semibold text-ocean-600",
                  compact ? "text-sm" : "text-base"
                )}>
                  ${item.product.price.toFixed(2)}
                </span>
                
                {hasDiscount && (
                  <span className="text-xs text-gray-500 line-through">
                    ${item.product.originalPrice!.toFixed(2)}
                  </span>
                )}
                
                {item.quantity > 1 && (
                  <span className="text-xs text-gray-500 mt-1">
                    ${(item.product.price * item.quantity).toFixed(2)} total
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Quantity Controls and Actions */}
          <div className="flex items-center justify-between mt-3">
            {/* Quantity Controls */}
            <div className="flex items-center space-x-2">
              <div className="flex items-center border border-gray-300 rounded-md">
                <button
                  onClick={() => handleQuantityChange(item.quantity - 1)}
                  disabled={item.quantity <= 1 || isLoading || isOutOfStock}
                  className="p-2 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <MinusIcon className="w-3 h-3" />
                </button>
                
                <div className="px-3 py-2 min-w-[3rem] text-center border-x border-gray-300">
                  {isQuantityLoading ? (
                    <LoadingSpinner size="sm" />
                  ) : (
                    <span className="text-sm font-medium">{item.quantity}</span>
                  )}
                </div>
                
                <button
                  onClick={() => handleQuantityChange(item.quantity + 1)}
                  disabled={item.quantity >= maxQuantity || isLoading || isOutOfStock}
                  className="p-2 hover:bg-gray-100 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                  <PlusIcon className="w-3 h-3" />
                </button>
              </div>

              {maxQuantity < 10 && maxQuantity > 0 && (
                <span className="text-xs text-gray-500">
                  {maxQuantity} available
                </span>
              )}
            </div>

            {/* Item Actions */}
            <div className="flex items-center space-x-2">
              {showWishlistOption && (
                <button
                  onClick={handleMoveToWishlist}
                  disabled={isLoading}
                  className="text-xs text-gray-500 hover:text-red-500 flex items-center space-x-1 transition-colors disabled:opacity-50"
                >
                  {isMovingToWishlist ? (
                    <LoadingSpinner size="sm" />
                  ) : (
                    <HeartIcon />
                  )}
                  <span>Save</span>
                </button>
              )}

              <button
                onClick={handleRemove}
                disabled={isLoading}
                className="text-xs text-gray-500 hover:text-red-500 flex items-center space-x-1 transition-colors disabled:opacity-50"
              >
                {isRemoving ? (
                  <LoadingSpinner size="sm" />
                ) : (
                  <TrashIcon />
                )}
                <span>Remove</span>
              </button>
            </div>
          </div>

          {/* Notes */}
          {item.notes && (
            <div className="mt-2 text-xs text-gray-600 bg-gray-50 rounded p-2">
              <strong>Note:</strong> {item.notes}
            </div>
          )}
        </div>
      </div>

      {/* Availability Notice */}
      {isOutOfStock && (
        <div className="bg-red-50 border border-red-200 rounded-md p-3">
          <div className="flex items-center">
            <ExclamationTriangleIcon className="w-4 h-4 text-red-600 mr-2" />
            <div className="text-sm">
              <p className="text-red-800 font-medium">Item currently unavailable</p>
              <p className="text-red-700">This item is out of stock. You can save it for later or remove it from your cart.</p>
            </div>
          </div>
        </div>
      )}

      {/* Limited Stock Notice */}
      {hasLimitedStock && !isOutOfStock && (
        <div className="bg-yellow-50 border border-yellow-200 rounded-md p-3">
          <div className="flex items-center">
            <ExclamationTriangleIcon className="w-4 h-4 text-yellow-600 mr-2" />
            <div className="text-sm">
              <p className="text-yellow-800 font-medium">Limited stock available</p>
              <p className="text-yellow-700">
                Only {maxQuantity} {maxQuantity === 1 ? 'item' : 'items'} available. 
                {item.quantity > maxQuantity && ' Quantity has been adjusted.'}
              </p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CartItemCard;