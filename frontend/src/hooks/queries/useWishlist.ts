import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { wishlistService } from '../../services/wishlistService';

// Query Keys
export const wishlistKeys = {
  all: ['wishlist'] as const,
  detail: () => [...wishlistKeys.all, 'detail'] as const,
  items: (params?: any) => [...wishlistKeys.all, 'items', params] as const,
  check: (productId: string) => [...wishlistKeys.all, 'check', productId] as const,
  count: () => [...wishlistKeys.all, 'count'] as const,
  stats: () => [...wishlistKeys.all, 'stats'] as const,
  shared: (shareToken: string) => [...wishlistKeys.all, 'shared', shareToken] as const,
};

// Fetch wishlist
export const useWishlist = () => {
  return useQuery({
    queryKey: wishlistKeys.detail(),
    queryFn: () => wishlistService.getWishlist(),
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Check if product is in wishlist
export const useIsInWishlist = (productId: string) => {
  return useQuery({
    queryKey: wishlistKeys.check(productId),
    queryFn: () => wishlistService.isInWishlist(productId),
    enabled: !!productId,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Add to wishlist mutation
export const useAddToWishlist = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (productId: string) => wishlistService.addToWishlist(productId),
    onSuccess: (_, productId) => {
      // Invalidate wishlist queries
      queryClient.invalidateQueries({ queryKey: wishlistKeys.all });
      // Set the check query to true
      queryClient.setQueryData(wishlistKeys.check(productId), true);
    },
  });
};

// Remove from wishlist mutation
export const useRemoveFromWishlist = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (productId: string) => wishlistService.removeFromWishlist(productId),
    onSuccess: (_, productId) => {
      // Invalidate wishlist queries
      queryClient.invalidateQueries({ queryKey: wishlistKeys.detail() });
      // Set the check query to false
      queryClient.setQueryData(wishlistKeys.check(productId), false);
    },
  });
};

// Toggle wishlist mutation (add or remove)
export const useToggleWishlist = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: async (productId: string) => {
      const isInWishlist = await wishlistService.isInWishlist(productId);
      if (isInWishlist) {
        return wishlistService.removeFromWishlist(productId);
      } else {
        return wishlistService.addToWishlist(productId);
      }
    },
    onSuccess: (_, productId) => {
      queryClient.invalidateQueries({ queryKey: wishlistKeys.all });
    },
  });
};

// Clear wishlist mutation
export const useClearWishlist = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: () => wishlistService.clearWishlist(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: wishlistKeys.all });
    },
  });
};