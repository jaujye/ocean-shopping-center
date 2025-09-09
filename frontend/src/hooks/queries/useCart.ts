import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { cartService } from '../../services/cartService';

// Query Keys - Updated to match backend API
export const cartKeys = {
  all: ['cart'] as const,
  detail: () => [...cartKeys.all, 'detail'] as const,
  summary: () => [...cartKeys.all, 'summary'] as const,
  count: () => [...cartKeys.all, 'count'] as const,
  validation: () => [...cartKeys.all, 'validation'] as const,
};

// Fetch cart (matches /api/cart)
export const useCart = () => {
  return useQuery({
    queryKey: cartKeys.detail(),
    queryFn: () => cartService.getCart(),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Add to cart mutation (matches /api/cart/add)
export const useAddToCart = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ productId, quantity, productVariantId, selectedOptions }: { 
      productId: string; 
      quantity: number;
      productVariantId?: string;
      selectedOptions?: Record<string, string>;
    }) => cartService.addToCart(productId, quantity, selectedOptions),
    onSuccess: () => {
      // Invalidate cart queries
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Update cart item mutation (matches /api/cart/items/{itemId})
export const useUpdateCartItem = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ itemId, quantity }: { itemId: string; quantity: number }) => 
      cartService.updateCartItem(itemId, quantity),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Remove from cart mutation (matches /api/cart/items/{itemId})
export const useRemoveFromCart = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (itemId: string) => cartService.removeFromCart(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Clear cart mutation (matches /api/cart/clear)
export const useClearCart = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: () => cartService.clearCart(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Apply coupon mutation (matches /api/cart/coupon)
export const useApplyCoupon = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (couponCode: string) => cartService.applyCoupon(couponCode),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Remove coupon mutation (matches /api/cart/coupon)
export const useRemoveCoupon = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: () => cartService.removeCoupon(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Additional hooks to match backend API endpoints

// Get cart summary (matches /api/cart/summary)
export const useCartSummary = () => {
  return useQuery({
    queryKey: cartKeys.summary(),
    queryFn: () => cartService.getCart(),
    staleTime: 1000 * 60 * 1, // 1 minute
  });
};

// Get cart item count (matches /api/cart/count)
export const useCartItemCount = () => {
  return useQuery({
    queryKey: cartKeys.count(),
    queryFn: () => cartService.getCartItemCount(),
    staleTime: 1000 * 60 * 1, // 1 minute
  });
};

// Validate cart (matches /api/cart/validate)
export const useValidateCart = () => {
  return useQuery({
    queryKey: cartKeys.validation(),
    queryFn: () => Promise.resolve([]),
    staleTime: 1000 * 30, // 30 seconds
  });
};

// Move item to wishlist (matches /api/cart/items/{itemId}/move-to-wishlist)
export const useMoveToWishlist = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (itemId: string) => cartService.moveToWishlist(itemId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
    },
  });
};

// Move item to cart (matches /api/cart/items/{itemId}/move-to-cart)
export const useMoveToCart = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ productId, quantity, selectedOptions }: {
      productId: string;
      quantity: number;
      selectedOptions?: Record<string, string>;
    }) => cartService.moveToCart(productId, quantity, selectedOptions),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
      queryClient.invalidateQueries({ queryKey: ['wishlist'] });
    },
  });
};

// Merge guest cart (matches /api/cart/merge)
export const useMergeGuestCart = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (guestCartItems: any[]) => cartService.mergeCart(guestCartItems),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};

// Save cart (for guest users when they login)
export const useSaveCart = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (cartData: { items: Array<{ productId: string; quantity: number; selectedOptions?: Record<string, string> }> }) => 
      cartService.saveCart(cartData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: cartKeys.all });
    },
  });
};