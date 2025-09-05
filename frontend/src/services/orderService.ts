import api from './api';

// Order status enum
export enum OrderStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  PROCESSING = 'PROCESSING',
  SHIPPED = 'SHIPPED',
  DELIVERED = 'DELIVERED',
  CANCELLED = 'CANCELLED',
  RETURNED = 'RETURNED'
}

// Address interface
export interface Address {
  firstName: string;
  lastName: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  state?: string;
  postalCode: string;
  country: string;
}

// Checkout request interface
export interface CheckoutRequest {
  customerEmail: string;
  customerPhone?: string;
  billingAddress: Address;
  shippingAddress: Address;
  paymentMethodId: string;
  couponCode?: string;
  notes?: string;
  savePaymentMethod?: boolean;
  sameAsShipping?: boolean;
}

// Order item interface
export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  productSku: string;
  productImageUrl?: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  currency: string;
  variantName?: string;
  variantSku?: string;
  storeId: number;
  storeName: string;
}

// Order summary interface
export interface OrderSummary {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  customerEmail: string;
  customerName: string;
  totalAmount: number;
  currency: string;
  itemCount: number;
  storeId: number;
  storeName: string;
  createdAt: string;
  updatedAt: string;
  requiresAttention: boolean;
  statusDisplayText: string;
  shippingCity: string;
  shippingCountry: string;
  trackingNumber?: string;
}

// Full order interface
export interface Order {
  id: number;
  orderNumber: string;
  status: OrderStatus;
  customerEmail: string;
  customerPhone?: string;
  billingFirstName: string;
  billingLastName: string;
  billingAddressLine1: string;
  billingAddressLine2?: string;
  billingCity: string;
  billingState?: string;
  billingPostalCode: string;
  billingCountry: string;
  shippingFirstName: string;
  shippingLastName: string;
  shippingAddressLine1: string;
  shippingAddressLine2?: string;
  shippingCity: string;
  shippingState?: string;
  shippingPostalCode: string;
  shippingCountry: string;
  subtotal: number;
  taxAmount: number;
  shippingAmount: number;
  discountAmount: number;
  totalAmount: number;
  currency: string;
  orderItems: OrderItem[];
  storeId: number;
  storeName: string;
  createdAt: string;
  updatedAt: string;
  confirmedAt?: string;
  shippedAt?: string;
  deliveredAt?: string;
  cancelledAt?: string;
  notes?: string;
  internalNotes?: string;
  trackingNumber?: string;
}

// Checkout response interface
export interface CheckoutResponse {
  orderId?: number;
  orderNumber?: string;
  totalAmount?: number;
  currency?: string;
  paymentIntentId?: string;
  customerEmail?: string;
  paymentStatus?: string;
  paymentMethodLast4?: string;
  paymentMethodBrand?: string;
  success: boolean;
  message: string;
  redirectUrl?: string;
  requiresPaymentAction?: boolean;
  paymentActionUrl?: string;
}

// Paginated response interface
export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    pageSize: number;
    pageNumber: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}

// Order status update request
export interface OrderStatusUpdateRequest {
  status: OrderStatus;
  internalNotes?: string;
  trackingNumber?: string;
  notifyCustomer?: boolean;
}

class OrderService {
  // Checkout operations
  
  /**
   * Process checkout and create order
   */
  async processCheckout(checkoutData: CheckoutRequest): Promise<CheckoutResponse> {
    try {
      const response = await api.post('/checkout', checkoutData);
      return response.data;
    } catch (error) {
      console.error('Checkout failed:', error);
      throw error;
    }
  }

  /**
   * Validate checkout data
   */
  async validateCheckout(checkoutData: CheckoutRequest): Promise<string> {
    try {
      const response = await api.post('/checkout/validate', checkoutData);
      return response.data;
    } catch (error) {
      console.error('Checkout validation failed:', error);
      throw error;
    }
  }

  /**
   * Get checkout summary
   */
  async getCheckoutSummary(shippingCountry?: string): Promise<any> {
    try {
      const params = shippingCountry ? { shippingCountry } : {};
      const response = await api.get('/checkout/summary', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get checkout summary:', error);
      throw error;
    }
  }

  /**
   * Apply coupon code
   */
  async applyCoupon(couponCode: string): Promise<string> {
    try {
      const response = await api.post('/checkout/coupon', null, {
        params: { couponCode }
      });
      return response.data;
    } catch (error) {
      console.error('Failed to apply coupon:', error);
      throw error;
    }
  }

  /**
   * Remove coupon code
   */
  async removeCoupon(): Promise<string> {
    try {
      const response = await api.delete('/checkout/coupon');
      return response.data;
    } catch (error) {
      console.error('Failed to remove coupon:', error);
      throw error;
    }
  }

  /**
   * Calculate shipping costs
   */
  async calculateShipping(shippingAddress: Address): Promise<any> {
    try {
      const response = await api.post('/checkout/shipping/calculate', shippingAddress);
      return response.data;
    } catch (error) {
      console.error('Failed to calculate shipping:', error);
      throw error;
    }
  }

  // Customer order operations

  /**
   * Get user's orders with pagination
   */
  async getUserOrders(page: number = 0, size: number = 10, status?: OrderStatus): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params: any = { page, size };
      if (status) {
        params.status = status;
      }
      const response = await api.get('/orders', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get user orders:', error);
      throw error;
    }
  }

  /**
   * Get specific order by ID
   */
  async getOrder(orderId: number): Promise<Order> {
    try {
      const response = await api.get(`/orders/${orderId}`);
      return response.data;
    } catch (error) {
      console.error('Failed to get order:', error);
      throw error;
    }
  }

  /**
   * Get order by order number
   */
  async getOrderByNumber(orderNumber: string): Promise<Order> {
    try {
      const response = await api.get(`/orders/number/${orderNumber}`);
      return response.data;
    } catch (error) {
      console.error('Failed to get order by number:', error);
      throw error;
    }
  }

  /**
   * Cancel an order
   */
  async cancelOrder(orderId: number, reason?: string): Promise<string> {
    try {
      const params = reason ? { reason } : {};
      const response = await api.post(`/orders/${orderId}/cancel`, null, { params });
      return response.data;
    } catch (error) {
      console.error('Failed to cancel order:', error);
      throw error;
    }
  }

  /**
   * Get order history
   */
  async getOrderHistory(
    page: number = 0,
    size: number = 20,
    status?: OrderStatus,
    year?: number,
    month?: number
  ): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params: any = { page, size };
      if (status) params.status = status;
      if (year) params.year = year;
      if (month) params.month = month;
      
      const response = await api.get('/orders/history', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get order history:', error);
      throw error;
    }
  }

  /**
   * Search user's orders
   */
  async searchOrders(query: string, page: number = 0, size: number = 10): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params = { query, page, size };
      const response = await api.get('/orders/search', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to search orders:', error);
      throw error;
    }
  }

  /**
   * Get order tracking information
   */
  async getOrderTracking(orderId: number): Promise<any> {
    try {
      const response = await api.get(`/orders/${orderId}/tracking`);
      return response.data;
    } catch (error) {
      console.error('Failed to get order tracking:', error);
      throw error;
    }
  }

  /**
   * Request invoice for an order
   */
  async requestInvoice(orderId: number): Promise<string> {
    try {
      const response = await api.post(`/orders/${orderId}/invoice`);
      return response.data;
    } catch (error) {
      console.error('Failed to request invoice:', error);
      throw error;
    }
  }

  /**
   * Download invoice PDF
   */
  async downloadInvoice(orderId: number): Promise<Blob> {
    try {
      const response = await api.get(`/orders/${orderId}/invoice/download`, {
        responseType: 'blob'
      });
      return response.data;
    } catch (error) {
      console.error('Failed to download invoice:', error);
      throw error;
    }
  }

  /**
   * Reorder items from previous order
   */
  async reorderItems(orderId: number): Promise<string> {
    try {
      const response = await api.post(`/orders/${orderId}/reorder`);
      return response.data;
    } catch (error) {
      console.error('Failed to reorder items:', error);
      throw error;
    }
  }

  /**
   * Get user order statistics
   */
  async getUserOrderStatistics(): Promise<any> {
    try {
      const response = await api.get('/orders/statistics');
      return response.data;
    } catch (error) {
      console.error('Failed to get order statistics:', error);
      throw error;
    }
  }

  // Store management operations (for store owners)

  /**
   * Get store orders
   */
  async getStoreOrders(
    storeId: number,
    page: number = 0,
    size: number = 20,
    status?: OrderStatus,
    startDate?: string,
    endDate?: string
  ): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params: any = { page, size };
      if (status) params.status = status;
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      
      const response = await api.get(`/store/${storeId}/orders`, { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get store orders:', error);
      throw error;
    }
  }

  /**
   * Search store orders
   */
  async searchStoreOrders(
    storeId: number,
    query: string,
    page: number = 0,
    size: number = 20
  ): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params = { query, page, size };
      const response = await api.get(`/store/${storeId}/orders/search`, { params });
      return response.data;
    } catch (error) {
      console.error('Failed to search store orders:', error);
      throw error;
    }
  }

  /**
   * Get store order details
   */
  async getStoreOrder(storeId: number, orderId: number): Promise<Order> {
    try {
      const response = await api.get(`/store/${storeId}/orders/${orderId}`);
      return response.data;
    } catch (error) {
      console.error('Failed to get store order:', error);
      throw error;
    }
  }

  /**
   * Update order status
   */
  async updateOrderStatus(
    storeId: number,
    orderId: number,
    statusUpdate: OrderStatusUpdateRequest
  ): Promise<string> {
    try {
      const response = await api.put(`/store/${storeId}/orders/${orderId}/status`, statusUpdate);
      return response.data;
    } catch (error) {
      console.error('Failed to update order status:', error);
      throw error;
    }
  }

  /**
   * Process refund
   */
  async processRefund(
    storeId: number,
    orderId: number,
    amount: number,
    reason: string
  ): Promise<string> {
    try {
      const params = { amount, reason };
      const response = await api.post(`/store/${storeId}/orders/${orderId}/refund`, null, { params });
      return response.data;
    } catch (error) {
      console.error('Failed to process refund:', error);
      throw error;
    }
  }

  /**
   * Get store revenue analytics
   */
  async getStoreRevenueAnalytics(
    storeId: number,
    startDate: string,
    endDate: string
  ): Promise<any> {
    try {
      const params = { startDate, endDate };
      const response = await api.get(`/store/${storeId}/orders/analytics/revenue`, { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get store revenue analytics:', error);
      throw error;
    }
  }

  // Admin operations

  /**
   * Get all orders (admin)
   */
  async getAllOrders(
    page: number = 0,
    size: number = 20,
    status?: OrderStatus,
    startDate?: string,
    endDate?: string,
    storeId?: number
  ): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params: any = { page, size };
      if (status) params.status = status;
      if (startDate) params.startDate = startDate;
      if (endDate) params.endDate = endDate;
      if (storeId) params.storeId = storeId;
      
      const response = await api.get('/admin/orders', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get all orders:', error);
      throw error;
    }
  }

  /**
   * Search all orders (admin)
   */
  async searchAllOrders(query: string, page: number = 0, size: number = 20): Promise<PaginatedResponse<OrderSummary>> {
    try {
      const params = { query, page, size };
      const response = await api.get('/admin/orders/search', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to search all orders:', error);
      throw error;
    }
  }

  /**
   * Get order for admin
   */
  async getOrderForAdmin(orderId: number): Promise<Order> {
    try {
      const response = await api.get(`/admin/orders/${orderId}`);
      return response.data;
    } catch (error) {
      console.error('Failed to get order for admin:', error);
      throw error;
    }
  }

  /**
   * Get orders requiring attention
   */
  async getOrdersRequiringAttention(): Promise<OrderSummary[]> {
    try {
      const response = await api.get('/admin/orders/attention');
      return response.data;
    } catch (error) {
      console.error('Failed to get orders requiring attention:', error);
      throw error;
    }
  }

  /**
   * Get revenue analytics (admin)
   */
  async getRevenueAnalytics(startDate: string, endDate: string): Promise<any> {
    try {
      const params = { startDate, endDate };
      const response = await api.get('/admin/orders/analytics/revenue', { params });
      return response.data;
    } catch (error) {
      console.error('Failed to get revenue analytics:', error);
      throw error;
    }
  }

  // Utility methods

  /**
   * Get status display text
   */
  getStatusDisplayText(status: OrderStatus): string {
    switch (status) {
      case OrderStatus.PENDING:
        return 'Awaiting Payment';
      case OrderStatus.CONFIRMED:
        return 'Order Confirmed';
      case OrderStatus.PROCESSING:
        return 'Being Prepared';
      case OrderStatus.SHIPPED:
        return 'Shipped';
      case OrderStatus.DELIVERED:
        return 'Delivered';
      case OrderStatus.CANCELLED:
        return 'Cancelled';
      case OrderStatus.RETURNED:
        return 'Returned';
      default:
        return status;
    }
  }

  /**
   * Get status color class
   */
  getStatusColorClass(status: OrderStatus): string {
    switch (status) {
      case OrderStatus.PENDING:
        return 'text-yellow-600 bg-yellow-100';
      case OrderStatus.CONFIRMED:
        return 'text-blue-600 bg-blue-100';
      case OrderStatus.PROCESSING:
        return 'text-indigo-600 bg-indigo-100';
      case OrderStatus.SHIPPED:
        return 'text-purple-600 bg-purple-100';
      case OrderStatus.DELIVERED:
        return 'text-green-600 bg-green-100';
      case OrderStatus.CANCELLED:
        return 'text-red-600 bg-red-100';
      case OrderStatus.RETURNED:
        return 'text-gray-600 bg-gray-100';
      default:
        return 'text-gray-600 bg-gray-100';
    }
  }

  /**
   * Format currency
   */
  formatCurrency(amount: number, currency: string = 'USD'): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount);
  }

  /**
   * Format date
   */
  formatDate(dateString: string): string {
    return new Intl.DateTimeFormat('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(dateString));
  }
}

export default new OrderService();