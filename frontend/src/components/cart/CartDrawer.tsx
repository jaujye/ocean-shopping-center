import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { CartItem, Cart } from '../../types';
import { cartService } from '../../services/cartService';
import Button from '../ui/Button';
import LoadingSpinner from '../ui/LoadingSpinner';
import { cn } from '../../utils/cn';

// Icons
const XIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
  </svg>
);

const ShoppingCartIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-6 h-6", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4m0 0L7 13m0 0l-2.5 5M7 13l2.5 5m6-5v5a2 2 0 01-2 2H9a2 2 0 01-2-2v-5m6-5V4a2 2 0 00-2-2H9a2 2 0 00-2 2v4.01" />
  </svg>
);

const TrashIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
  </svg>
);

const HeartIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
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

interface CartDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  className?: string;
}

const CartDrawer: React.FC<CartDrawerProps> = ({ isOpen, onClose, className }) => {
  const navigate = useNavigate();
  const [cart, setCart] = useState<Cart | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [updatingItems, setUpdatingItems] = useState<Set<string>>(new Set());
  const [error, setError] = useState<string | null>(null);

  // Load cart data when drawer opens
  useEffect(() => {
    if (isOpen) {
      loadCart();
    }
  }, [isOpen]);

  const loadCart = async () => {
    setIsLoading(true);
    setError(null);
    try {
      const cartData = await cartService.getCart();
      setCart(cartData);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load cart');
      console.error('Failed to load cart:', err);
    } finally {
      setIsLoading(false);
    }
  };

  // Update item quantity
  const updateQuantity = async (itemId: string, newQuantity: number) => {
    if (newQuantity < 1) return;

    setUpdatingItems(prev => new Set(prev).add(itemId));
    try {
      const updatedCart = await cartService.updateCartItem(itemId, newQuantity);
      setCart(updatedCart);
    } catch (error) {
      console.error('Failed to update quantity:', error);
    } finally {
      setUpdatingItems(prev => {
        const newSet = new Set(prev);
        newSet.delete(itemId);
        return newSet;
      });
    }
  };

  // Remove item from cart
  const removeItem = async (itemId: string) => {
    setUpdatingItems(prev => new Set(prev).add(itemId));
    try {
      const updatedCart = await cartService.removeFromCart(itemId);
      setCart(updatedCart);
    } catch (error) {
      console.error('Failed to remove item:', error);
    } finally {
      setUpdatingItems(prev => {
        const newSet = new Set(prev);
        newSet.delete(itemId);
        return newSet;
      });
    }
  };

  // Move item to wishlist
  const moveToWishlist = async (itemId: string) => {
    setUpdatingItems(prev => new Set(prev).add(itemId));
    try {
      const updatedCart = await cartService.moveToWishlist(itemId);
      setCart(updatedCart);
    } catch (error) {
      console.error('Failed to move to wishlist:', error);
    } finally {
      setUpdatingItems(prev => {
        const newSet = new Set(prev);
        newSet.delete(itemId);
        return newSet;
      });
    }
  };

  // Clear entire cart
  const clearCart = async () => {
    setIsLoading(true);
    try {
      await cartService.clearCart();
      setCart({ 
        id: 'empty-cart', 
        items: [], 
        itemCount: 0, 
        subtotal: 0, 
        taxAmount: 0, 
        shippingFee: 0, 
        total: 0, 
        totalAmount: 0 
      });
    } catch (error) {
      console.error('Failed to clear cart:', error);
    } finally {
      setIsLoading(false);
    }
  };

  // Handle checkout
  const handleCheckout = () => {
    onClose();
    navigate('/checkout');
  };

  // Handle view cart
  const handleViewCart = () => {
    onClose();
    navigate('/cart');
  };

  // Handle continue shopping
  const handleContinueShopping = () => {
    onClose();
    navigate('/products');
  };

  // Calculate total items
  const totalItems = cart?.items.reduce((total, item) => total + item.quantity, 0) || 0;

  return (
    <>
      {/* Backdrop */}
      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-50 z-40 transition-opacity duration-300"
          onClick={onClose}
        />
      )}

      {/* Drawer */}
      <div
        className={cn(
          "fixed top-0 right-0 h-full w-full sm:w-96 bg-white shadow-xl z-50 transform transition-transform duration-300 ease-in-out",
          isOpen ? "translate-x-0" : "translate-x-full",
          className
        )}
      >
        <div className="flex flex-col h-full">
          {/* Header */}
          <div className="flex items-center justify-between p-4 border-b border-gray-200">
            <div className="flex items-center space-x-2">
              <ShoppingCartIcon className="text-ocean-600" />
              <h2 className="text-lg font-semibold text-gray-900">
                Shopping Cart {totalItems > 0 && `(${totalItems})`}
              </h2>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-full transition-colors duration-200"
            >
              <XIcon />
            </button>
          </div>

          {/* Content */}
          <div className="flex-1 overflow-y-auto">
            {isLoading ? (
              <div className="flex items-center justify-center py-8">
                <LoadingSpinner size="md" />
              </div>
            ) : error ? (
              <div className="p-4 text-center">
                <p className="text-red-600 mb-4">{error}</p>
                <Button onClick={loadCart} variant="outline" size="sm">
                  Try Again
                </Button>
              </div>
            ) : !cart || cart.items.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
                <ShoppingCartIcon className="w-16 h-16 text-gray-300 mb-4" />
                <h3 className="text-lg font-medium text-gray-900 mb-2">Your cart is empty</h3>
                <p className="text-gray-500 mb-6">Start shopping to add items to your cart</p>
                <Button onClick={handleContinueShopping} variant="primary">
                  Continue Shopping
                </Button>
              </div>
            ) : (
              <div className="p-4 space-y-4">
                {/* Cart Items */}
                <div className="space-y-4">
                  {cart.items.map((item) => (
                    <div key={item.id} className="flex space-x-3 p-3 bg-gray-50 rounded-lg">
                      {/* Product Image */}
                      <div className="flex-shrink-0">
                        <img
                          src={item.product.images[0] || '/placeholder-image.jpg'}
                          alt={item.product.name}
                          className="w-16 h-16 object-cover rounded-lg"
                        />
                      </div>

                      {/* Product Details */}
                      <div className="flex-1 min-w-0">
                        <h4 className="text-sm font-medium text-gray-900 truncate">
                          {item.product.name}
                        </h4>
                        <p className="text-xs text-gray-500 mt-1">
                          {item.product.storeName}
                        </p>
                        
                        {/* Selected Options */}
                        {item.selectedOptions && Object.keys(item.selectedOptions).length > 0 && (
                          <div className="mt-1">
                            {Object.entries(item.selectedOptions).map(([key, value]) => (
                              <span key={key} className="text-xs text-gray-500">
                                {key}: {value}
                              </span>
                            ))}
                          </div>
                        )}

                        {/* Price and Quantity */}
                        <div className="flex items-center justify-between mt-2">
                          <div className="flex items-center space-x-2">
                            <span className="text-sm font-medium text-ocean-600">
                              ${item.product.price.toFixed(2)}
                            </span>
                            {item.product.originalPrice && item.product.originalPrice > item.product.price && (
                              <span className="text-xs text-gray-500 line-through">
                                ${item.product.originalPrice.toFixed(2)}
                              </span>
                            )}
                          </div>

                          {/* Quantity Controls */}
                          <div className="flex items-center space-x-1">
                            <button
                              onClick={() => updateQuantity(item.id, item.quantity - 1)}
                              disabled={item.quantity <= 1 || updatingItems.has(item.id)}
                              className="p-1 hover:bg-gray-200 rounded disabled:opacity-50"
                            >
                              <MinusIcon />
                            </button>
                            <span className="w-8 text-center text-sm font-medium">
                              {updatingItems.has(item.id) ? (
                                <LoadingSpinner size="sm" />
                              ) : (
                                item.quantity
                              )}
                            </span>
                            <button
                              onClick={() => updateQuantity(item.id, item.quantity + 1)}
                              disabled={item.quantity >= item.product.stockQuantity || updatingItems.has(item.id)}
                              className="p-1 hover:bg-gray-200 rounded disabled:opacity-50"
                            >
                              <PlusIcon />
                            </button>
                          </div>
                        </div>

                        {/* Item Actions */}
                        <div className="flex items-center justify-between mt-2">
                          <div className="flex space-x-2">
                            <button
                              onClick={() => moveToWishlist(item.id)}
                              disabled={updatingItems.has(item.id)}
                              className="text-xs text-gray-500 hover:text-red-500 flex items-center space-x-1"
                            >
                              <HeartIcon />
                              <span>Wishlist</span>
                            </button>
                            <button
                              onClick={() => removeItem(item.id)}
                              disabled={updatingItems.has(item.id)}
                              className="text-xs text-gray-500 hover:text-red-500 flex items-center space-x-1"
                            >
                              <TrashIcon />
                              <span>Remove</span>
                            </button>
                          </div>

                          <div className="text-sm font-medium text-gray-900">
                            ${(item.product.price * item.quantity).toFixed(2)}
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Clear Cart Button */}
                {cart.items.length > 0 && (
                  <div className="text-center">
                    <button
                      onClick={clearCart}
                      className="text-sm text-red-600 hover:text-red-800 underline"
                    >
                      Clear Cart
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Footer with Totals and Actions */}
          {cart && cart.items.length > 0 && (
            <div className="border-t border-gray-200 p-4 space-y-4 bg-white">
              {/* Totals */}
              <div className="space-y-2">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-600">Subtotal</span>
                  <span className="font-medium">${cart.subtotal.toFixed(2)}</span>
                </div>
                
                {cart.taxAmount > 0 && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Tax</span>
                    <span className="font-medium">${cart.taxAmount.toFixed(2)}</span>
                  </div>
                )}
                
                {cart.shippingFee > 0 && (
                  <div className="flex justify-between text-sm">
                    <span className="text-gray-600">Shipping</span>
                    <span className="font-medium">${cart.shippingFee.toFixed(2)}</span>
                  </div>
                )}

                <div className="flex justify-between text-base font-semibold border-t border-gray-200 pt-2">
                  <span>Total</span>
                  <span className="text-ocean-600">${cart.total.toFixed(2)}</span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="space-y-2">
                <Button
                  onClick={handleCheckout}
                  variant="primary"
                  size="lg"
                  fullWidth
                >
                  Proceed to Checkout
                </Button>
                
                <Button
                  onClick={handleViewCart}
                  variant="outline"
                  size="md"
                  fullWidth
                >
                  View Cart
                </Button>
              </div>
            </div>
          )}
        </div>
      </div>
    </>
  );
};

export default CartDrawer;