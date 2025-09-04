import { apiClient } from './api';
import { Product, PaginatedResponse } from '../types';

export interface WishlistItem {
  id: string;
  productId: string;
  product: Product;
  addedAt: string;
}

export interface Wishlist {
  items: WishlistItem[];
  totalItems: number;
}

class WishlistService {
  /**
   * Get user's wishlist
   */
  async getWishlist(): Promise<Wishlist> {
    const response = await apiClient.request<Wishlist>({
      method: 'GET',
      url: '/wishlist',
    });
    return response;
  }

  /**
   * Get paginated wishlist items
   */
  async getWishlistItems(page: number = 1, limit: number = 20): Promise<PaginatedResponse<WishlistItem>> {
    const response = await apiClient.request<PaginatedResponse<WishlistItem>>({
      method: 'GET',
      url: '/wishlist/items',
      params: { page, limit },
    });
    return response;
  }

  /**
   * Add product to wishlist
   */
  async addToWishlist(productId: string): Promise<Wishlist> {
    const response = await apiClient.request<Wishlist>({
      method: 'POST',
      url: '/wishlist/add',
      data: { productId },
    });
    return response;
  }

  /**
   * Remove product from wishlist
   */
  async removeFromWishlist(productId: string): Promise<Wishlist> {
    const response = await apiClient.request<Wishlist>({
      method: 'DELETE',
      url: `/wishlist/items/${productId}`,
    });
    return response;
  }

  /**
   * Check if product is in wishlist
   */
  async isInWishlist(productId: string): Promise<boolean> {
    try {
      const wishlist = await this.getWishlist();
      return wishlist.items.some(item => item.productId === productId);
    } catch (error) {
      console.error('Failed to check wishlist status:', error);
      return false;
    }
  }

  /**
   * Clear entire wishlist
   */
  async clearWishlist(): Promise<void> {
    await apiClient.request({
      method: 'DELETE',
      url: '/wishlist/clear',
    });
  }

  /**
   * Move multiple items from wishlist to cart
   */
  async moveToCart(productIds: string[]): Promise<{ success: string[]; failed: string[] }> {
    const response = await apiClient.request<{ success: string[]; failed: string[] }>({
      method: 'POST',
      url: '/wishlist/move-to-cart',
      data: { productIds },
    });
    return response;
  }

  /**
   * Get wishlist item count
   */
  async getWishlistCount(): Promise<number> {
    try {
      const wishlist = await this.getWishlist();
      return wishlist.totalItems;
    } catch (error) {
      console.error('Failed to get wishlist count:', error);
      return 0;
    }
  }

  /**
   * Share wishlist (get shareable link)
   */
  async shareWishlist(): Promise<{ shareUrl: string; expiresAt: string }> {
    const response = await apiClient.request<{ shareUrl: string; expiresAt: string }>({
      method: 'POST',
      url: '/wishlist/share',
    });
    return response;
  }

  /**
   * Get shared wishlist by share token
   */
  async getSharedWishlist(shareToken: string): Promise<{
    ownerName: string;
    items: WishlistItem[];
    createdAt: string;
  }> {
    const response = await apiClient.request<{
      ownerName: string;
      items: WishlistItem[];
      createdAt: string;
    }>({
      method: 'GET',
      url: `/wishlist/shared/${shareToken}`,
    });
    return response;
  }

  /**
   * Add multiple products to wishlist
   */
  async addMultipleToWishlist(productIds: string[]): Promise<Wishlist> {
    const response = await apiClient.request<Wishlist>({
      method: 'POST',
      url: '/wishlist/add-multiple',
      data: { productIds },
    });
    return response;
  }

  /**
   * Get wishlist statistics
   */
  async getWishlistStats(): Promise<{
    totalItems: number;
    totalValue: number;
    averagePrice: number;
    categoriesCount: number;
    mostWishedCategory: string;
  }> {
    const response = await apiClient.request<{
      totalItems: number;
      totalValue: number;
      averagePrice: number;
      categoriesCount: number;
      mostWishedCategory: string;
    }>({
      method: 'GET',
      url: '/wishlist/stats',
    });
    return response;
  }
}

// Create and export a singleton instance
export const wishlistService = new WishlistService();
export default wishlistService;