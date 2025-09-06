import { apiClient } from './api';
import { CartItem, Cart, Product } from '../types';

class CartService {
  /**
   * Get current user's cart
   */
  async getCart(): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'GET',
      url: '/cart',
    });
    return response;
  }

  /**
   * Add item to cart
   */
  async addToCart(productId: string, quantity: number = 1, selectedOptions?: Record<string, string>): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'POST',
      url: '/cart/add',
      data: {
        productId,
        quantity,
        selectedOptions,
      },
    });
    return response;
  }

  /**
   * Update cart item quantity
   */
  async updateCartItem(itemId: string, quantity: number): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'PATCH',
      url: `/cart/items/${itemId}`,
      data: { quantity },
    });
    return response;
  }

  /**
   * Remove item from cart
   */
  async removeFromCart(itemId: string): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'DELETE',
      url: `/cart/items/${itemId}`,
    });
    return response;
  }

  /**
   * Clear entire cart
   */
  async clearCart(): Promise<void> {
    await apiClient.request({
      method: 'DELETE',
      url: '/cart/clear',
    });
  }

  /**
   * Get cart item count
   */
  async getCartItemCount(): Promise<number> {
    const cart = await this.getCart();
    return cart.items.reduce((total, item) => total + item.quantity, 0);
  }

  /**
   * Check if product is in cart
   */
  async isInCart(productId: string): Promise<boolean> {
    const cart = await this.getCart();
    return cart.items.some(item => item.productId === productId);
  }

  /**
   * Move item to wishlist (if wishlist functionality exists)
   */
  async moveToWishlist(itemId: string): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'POST',
      url: `/cart/items/${itemId}/move-to-wishlist`,
    });
    return response;
  }

  /**
   * Move item from wishlist to cart
   */
  async moveToCart(productId: string, quantity: number = 1, selectedOptions?: Record<string, string>): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'POST',
      url: '/wishlist/move-to-cart',
      data: {
        productId,
        quantity,
        selectedOptions,
      },
    });
    return response;
  }

  /**
   * Apply coupon code
   */
  async applyCoupon(couponCode: string): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'POST',
      url: '/cart/coupon',
      data: { couponCode },
    });
    return response;
  }

  /**
   * Remove coupon
   */
  async removeCoupon(): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'DELETE',
      url: '/cart/coupon',
    });
    return response;
  }

  /**
   * Save cart for later (for guest users when they login)
   */
  async saveCart(cartData: { items: Array<{ productId: string; quantity: number; selectedOptions?: Record<string, string> }> }): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'POST',
      url: '/cart/save',
      data: cartData,
    });
    return response;
  }

  /**
   * Merge guest cart with user cart (when user logs in)
   */
  async mergeCart(guestCartItems: Array<{ productId: string; quantity: number; selectedOptions?: Record<string, string> }>): Promise<Cart> {
    const response = await apiClient.request<Cart>({
      method: 'POST',
      url: '/cart/merge',
      data: { items: guestCartItems },
    });
    return response;
  }
}

// Create and export a singleton instance
export const cartService = new CartService();
export default cartService;