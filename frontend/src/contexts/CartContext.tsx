import React, { createContext, useContext, useReducer, useEffect, useCallback } from 'react';
import { Cart, CartItem, Product } from '../types';
import { cartService } from '../services/cartService';
import { useAuth } from './AuthContext';
import { useNotification } from './NotificationContext';

// Cart Context Types
interface CartContextType {
  cart: Cart | null;
  isLoading: boolean;
  isUpdating: boolean;
  error: string | null;
  itemCount: number;
  isEmpty: boolean;
  
  // Actions
  refreshCart: () => Promise<void>;
  addToCart: (productId: string, quantity?: number, selectedOptions?: Record<string, string>) => Promise<void>;
  updateCartItem: (itemId: string, quantity: number) => Promise<void>;
  removeFromCart: (itemId: string) => Promise<void>;
  clearCart: () => Promise<void>;
  moveToWishlist: (itemId: string) => Promise<void>;
  moveToCart: (itemId: string) => Promise<void>;
  applyCoupon: (couponCode: string) => Promise<void>;
  removeCoupon: () => Promise<void>;
  validateCart: () => Promise<string[]>;
  mergeGuestCart: (guestSessionId: string) => Promise<void>;
}

// Cart State
interface CartState {
  cart: Cart | null;
  isLoading: boolean;
  isUpdating: boolean;
  error: string | null;
}

// Cart Actions
type CartAction =
  | { type: 'SET_LOADING'; payload: boolean }
  | { type: 'SET_UPDATING'; payload: boolean }
  | { type: 'SET_CART'; payload: Cart | null }
  | { type: 'SET_ERROR'; payload: string | null }
  | { type: 'RESET' };

// Initial State
const initialState: CartState = {
  cart: null,
  isLoading: false,
  isUpdating: false,
  error: null,
};

// Cart Reducer
function cartReducer(state: CartState, action: CartAction): CartState {
  switch (action.type) {
    case 'SET_LOADING':
      return { ...state, isLoading: action.payload };
    case 'SET_UPDATING':
      return { ...state, isUpdating: action.payload };
    case 'SET_CART':
      return { ...state, cart: action.payload, error: null };
    case 'SET_ERROR':
      return { ...state, error: action.payload };
    case 'RESET':
      return initialState;
    default:
      return state;
  }
}

// Create Context
const CartContext = createContext<CartContextType | undefined>(undefined);

// Cart Provider Component
interface CartProviderProps {
  children: React.ReactNode;
}

export const CartProvider: React.FC<CartProviderProps> = ({ children }) => {
  const [state, dispatch] = useReducer(cartReducer, initialState);
  const { user, isAuthenticated } = useAuth();
  const { showNotification } = useNotification();

  // Computed values
  const itemCount = state.cart?.items.reduce((total, item) => total + item.quantity, 0) || 0;
  const isEmpty = !state.cart || state.cart.items.length === 0;

  // Load cart on mount and auth changes
  useEffect(() => {
    loadCart();
  }, [isAuthenticated]);

  // Auto-refresh cart periodically
  useEffect(() => {
    const interval = setInterval(() => {
      if (!state.isLoading && !state.isUpdating) {
        validateCart();
      }
    }, 5 * 60 * 1000); // Every 5 minutes

    return () => clearInterval(interval);
  }, [state.isLoading, state.isUpdating]);

  const loadCart = async () => {
    dispatch({ type: 'SET_LOADING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const cart = await cartService.getCart();
      dispatch({ type: 'SET_CART', payload: cart });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to load cart';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      console.error('Failed to load cart:', error);
    } finally {
      dispatch({ type: 'SET_LOADING', payload: false });
    }
  };

  const refreshCart = useCallback(async () => {
    await loadCart();
  }, []);

  const addToCart = useCallback(async (
    productId: string, 
    quantity: number = 1, 
    selectedOptions?: Record<string, string>
  ) => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.addToCart(productId, quantity, selectedOptions);
      dispatch({ type: 'SET_CART', payload: updatedCart });
      
      showNotification({
        type: 'success',
        title: 'Added to Cart',
        message: `Item added to your cart successfully`,
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to add item to cart';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Failed to Add Item',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const updateCartItem = useCallback(async (itemId: string, quantity: number) => {
    if (quantity < 1) {
      await removeFromCart(itemId);
      return;
    }

    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.updateCartItem(itemId, quantity);
      dispatch({ type: 'SET_CART', payload: updatedCart });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to update item quantity';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Update Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const removeFromCart = useCallback(async (itemId: string) => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.removeFromCart(itemId);
      dispatch({ type: 'SET_CART', payload: updatedCart });
      
      showNotification({
        type: 'success',
        title: 'Item Removed',
        message: 'Item removed from your cart',
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to remove item';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Remove Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const clearCart = useCallback(async () => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      await cartService.clearCart();
      dispatch({ type: 'SET_CART', payload: { items: [], subtotal: 0, taxAmount: 0, shippingFee: 0, total: 0 } });
      
      showNotification({
        type: 'success',
        title: 'Cart Cleared',
        message: 'All items removed from your cart',
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to clear cart';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Clear Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const moveToWishlist = useCallback(async (itemId: string) => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.moveToWishlist(itemId);
      dispatch({ type: 'SET_CART', payload: updatedCart });
      
      showNotification({
        type: 'success',
        title: 'Saved for Later',
        message: 'Item moved to your wishlist',
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to save item';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Save Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const moveToCart = useCallback(async (itemId: string) => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.moveToCart(itemId);
      dispatch({ type: 'SET_CART', payload: updatedCart });
      
      showNotification({
        type: 'success',
        title: 'Moved to Cart',
        message: 'Item moved back to your cart',
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to move item';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Move Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const applyCoupon = useCallback(async (couponCode: string) => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.applyCoupon(couponCode);
      dispatch({ type: 'SET_CART', payload: updatedCart });
      
      showNotification({
        type: 'success',
        title: 'Coupon Applied',
        message: `Coupon "${couponCode}" applied successfully`,
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to apply coupon';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Coupon Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const removeCoupon = useCallback(async () => {
    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      const updatedCart = await cartService.removeCoupon();
      dispatch({ type: 'SET_CART', payload: updatedCart });
      
      showNotification({
        type: 'success',
        title: 'Coupon Removed',
        message: 'Coupon removed from your cart',
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to remove coupon';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Remove Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [showNotification]);

  const validateCart = useCallback(async (): Promise<string[]> => {
    try {
      // This would call the validation endpoint
      // For now, return empty array (no issues)
      return [];
    } catch (error) {
      console.error('Cart validation failed:', error);
      return ['Failed to validate cart'];
    }
  }, []);

  const mergeGuestCart = useCallback(async (guestSessionId: string) => {
    if (!isAuthenticated) {
      throw new Error('User must be authenticated to merge cart');
    }

    dispatch({ type: 'SET_UPDATING', payload: true });
    dispatch({ type: 'SET_ERROR', payload: null });

    try {
      // This would call the merge endpoint
      await loadCart(); // Reload cart after merge
      
      showNotification({
        type: 'success',
        title: 'Cart Merged',
        message: 'Your guest cart has been merged successfully',
        duration: 3000,
      });
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Failed to merge cart';
      dispatch({ type: 'SET_ERROR', payload: errorMessage });
      
      showNotification({
        type: 'error',
        title: 'Merge Failed',
        message: errorMessage,
        duration: 5000,
      });
      
      throw error;
    } finally {
      dispatch({ type: 'SET_UPDATING', payload: false });
    }
  }, [isAuthenticated, showNotification]);

  const contextValue: CartContextType = {
    cart: state.cart,
    isLoading: state.isLoading,
    isUpdating: state.isUpdating,
    error: state.error,
    itemCount,
    isEmpty,
    
    // Actions
    refreshCart,
    addToCart,
    updateCartItem,
    removeFromCart,
    clearCart,
    moveToWishlist,
    moveToCart,
    applyCoupon,
    removeCoupon,
    validateCart,
    mergeGuestCart,
  };

  return (
    <CartContext.Provider value={contextValue}>
      {children}
    </CartContext.Provider>
  );
};

// Custom hook to use cart context
export const useCart = (): CartContextType => {
  const context = useContext(CartContext);
  if (context === undefined) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
};

// Cart utilities for components
export const cartUtils = {
  getTotalSavings: (cart: Cart): number => {
    if (!cart) return 0;
    let savings = cart.discountAmount || 0;
    
    // Add item-level savings
    cart.items.forEach(item => {
      if (item.product.originalPrice && item.product.originalPrice > item.product.price) {
        savings += (item.product.originalPrice - item.product.price) * item.quantity;
      }
    });
    
    return savings;
  },

  hasDiscounts: (cart: Cart): boolean => {
    if (!cart) return false;
    return cart.discountAmount > 0 || 
           cart.items.some(item => 
             item.product.originalPrice && item.product.originalPrice > item.product.price
           );
  },

  isEligibleForFreeShipping: (cart: Cart, threshold: number = 50): boolean => {
    return cart ? cart.subtotal >= threshold : false;
  },

  getShippingProgressPercentage: (cart: Cart, threshold: number = 50): number => {
    if (!cart) return 0;
    return Math.min((cart.subtotal / threshold) * 100, 100);
  },

  getAmountForFreeShipping: (cart: Cart, threshold: number = 50): number => {
    if (!cart) return threshold;
    return Math.max(threshold - cart.subtotal, 0);
  },
};

export default CartContext;