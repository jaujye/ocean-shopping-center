import api from './api';

// Coupon types and enums
export enum CouponType {
  PERCENTAGE = 'PERCENTAGE',
  FIXED_AMOUNT = 'FIXED_AMOUNT',
  FREE_SHIPPING = 'FREE_SHIPPING',
  BUY_ONE_GET_ONE = 'BUY_ONE_GET_ONE'
}

export enum CouponStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  EXPIRED = 'EXPIRED',
  USED_UP = 'USED_UP'
}

// Coupon validation interfaces
export interface CouponValidationRequest {
  code: string;
  orderAmount: number;
  storeId?: number;
  customerEmail?: string;
  currency?: string;
}

export interface CouponValidationResponse {
  valid: boolean;
  code?: string;
  name?: string;
  type?: CouponType;
  discountAmount: number;
  originalAmount?: number;
  finalAmount?: number;
  discountPercentage?: number;
  currency?: string;
  freeShipping?: boolean;
  errorMessage?: string;
  message?: string;
}

// Coupon details interface
export interface Coupon {
  id: number;
  code: string;
  name: string;
  description?: string;
  type: CouponType;
  status: CouponStatus;
  storeId?: number;
  storeName?: string;
  discountPercentage?: number;
  discountAmount?: number;
  minimumOrderAmount?: number;
  maximumDiscount?: number;
  usageLimit?: number;
  usageLimitPerCustomer?: number;
  timesUsed: number;
  validFrom: string;
  validUntil: string;
  currency: string;
  appliesToSaleItems: boolean;
  firstTimeCustomerOnly: boolean;
  isActive: boolean;
  isExpired: boolean;
  isUsedUp: boolean;
  createdAt: string;
  updatedAt: string;
}

// API response interfaces
export interface CouponListResponse {
  content: Coupon[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

class CouponService {
  /**
   * Validate a coupon code and get discount details
   */
  async validateCoupon(request: CouponValidationRequest): Promise<CouponValidationResponse> {
    try {
      const response = await api.post<CouponValidationResponse>('/coupons/validate', request);
      return response.data;
    } catch (error: any) {
      console.error('Error validating coupon:', error);
      return {
        valid: false,
        code: request.code,
        discountAmount: 0,
        errorMessage: error.response?.data?.message || 'Failed to validate coupon'
      };
    }
  }

  /**
   * Quick validation to check if coupon exists and is valid
   */
  async quickValidate(code: string, orderAmount?: number, storeId?: number): Promise<boolean> {
    try {
      const params = new URLSearchParams();
      if (orderAmount !== undefined) params.append('orderAmount', orderAmount.toString());
      if (storeId !== undefined) params.append('storeId', storeId.toString());

      const url = `/coupons/quick-validate/${encodeURIComponent(code)}?${params.toString()}`;
      const response = await api.get<boolean>(url);
      return response.data;
    } catch (error) {
      console.error('Error in quick coupon validation:', error);
      return false;
    }
  }

  /**
   * Get discount preview for a coupon without applying it
   */
  async getDiscountPreview(
    code: string, 
    orderAmount: number, 
    storeId?: number, 
    customerEmail?: string
  ): Promise<CouponValidationResponse> {
    try {
      const params = new URLSearchParams();
      params.append('orderAmount', orderAmount.toString());
      if (storeId !== undefined) params.append('storeId', storeId.toString());
      if (customerEmail) params.append('customerEmail', customerEmail);

      const url = `/coupons/${encodeURIComponent(code)}/preview?${params.toString()}`;
      const response = await api.get<CouponValidationResponse>(url);
      return response.data;
    } catch (error: any) {
      console.error('Error getting discount preview:', error);
      return {
        valid: false,
        code,
        discountAmount: 0,
        errorMessage: error.response?.data?.message || 'Failed to get discount preview'
      };
    }
  }

  /**
   * Get coupon details by code (requires authentication)
   */
  async getCouponByCode(code: string): Promise<Coupon> {
    try {
      const response = await api.get<Coupon>(`/coupons/${encodeURIComponent(code)}`);
      return response.data;
    } catch (error: any) {
      console.error('Error getting coupon details:', error);
      throw new Error(error.response?.data?.message || 'Failed to get coupon details');
    }
  }

  /**
   * Get global coupons (available to all customers)
   */
  async getGlobalCoupons(page: number = 0, size: number = 20): Promise<CouponListResponse> {
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', size.toString());
      params.append('sort', 'createdAt,desc');

      const response = await api.get<CouponListResponse>(`/coupons/global?${params.toString()}`);
      return response.data;
    } catch (error: any) {
      console.error('Error getting global coupons:', error);
      throw new Error(error.response?.data?.message || 'Failed to get global coupons');
    }
  }

  /**
   * Get store-specific coupons (requires authentication)
   */
  async getStoreCoupons(storeId: number, page: number = 0, size: number = 20): Promise<CouponListResponse> {
    try {
      const params = new URLSearchParams();
      params.append('page', page.toString());
      params.append('size', size.toString());
      params.append('sort', 'createdAt,desc');

      const response = await api.get<CouponListResponse>(`/coupons/store/${storeId}?${params.toString()}`);
      return response.data;
    } catch (error: any) {
      console.error('Error getting store coupons:', error);
      throw new Error(error.response?.data?.message || 'Failed to get store coupons');
    }
  }

  /**
   * Check if a coupon is still valid (client-side validation)
   */
  isCouponValid(coupon: Coupon): boolean {
    const now = new Date();
    const validFrom = new Date(coupon.validFrom);
    const validUntil = new Date(coupon.validUntil);
    
    return coupon.status === CouponStatus.ACTIVE &&
           now >= validFrom &&
           now <= validUntil &&
           (coupon.usageLimit === null || coupon.usageLimit === undefined || coupon.timesUsed < coupon.usageLimit);
  }

  /**
   * Format coupon discount for display
   */
  formatDiscount(coupon: Coupon): string {
    switch (coupon.type) {
      case CouponType.PERCENTAGE:
        return `${coupon.discountPercentage}% off`;
      case CouponType.FIXED_AMOUNT:
        return `$${coupon.discountAmount} off`;
      case CouponType.FREE_SHIPPING:
        return 'Free shipping';
      case CouponType.BUY_ONE_GET_ONE:
        return 'Buy one, get one free';
      default:
        return 'Discount applied';
    }
  }

  /**
   * Get human-readable coupon restrictions
   */
  getCouponRestrictions(coupon: Coupon): string[] {
    const restrictions: string[] = [];
    
    if (coupon.minimumOrderAmount && coupon.minimumOrderAmount > 0) {
      restrictions.push(`Minimum order: $${coupon.minimumOrderAmount}`);
    }
    
    if (coupon.usageLimit && coupon.usageLimit > 0) {
      const remaining = coupon.usageLimit - coupon.timesUsed;
      restrictions.push(`${remaining} uses remaining`);
    }
    
    if (coupon.usageLimitPerCustomer && coupon.usageLimitPerCustomer > 0) {
      restrictions.push(`Limit ${coupon.usageLimitPerCustomer} per customer`);
    }
    
    if (coupon.firstTimeCustomerOnly) {
      restrictions.push('First-time customers only');
    }
    
    if (!coupon.appliesToSaleItems) {
      restrictions.push('Excludes sale items');
    }
    
    const validUntil = new Date(coupon.validUntil);
    restrictions.push(`Expires: ${validUntil.toLocaleDateString()}`);
    
    return restrictions;
  }

  /**
   * Calculate savings amount from discount percentage
   */
  calculateSavings(orderAmount: number, discountPercentage: number): number {
    return (orderAmount * discountPercentage) / 100;
  }

  /**
   * Validate coupon code format (client-side)
   */
  isValidCouponFormat(code: string): boolean {
    // Must be 3-50 characters, uppercase letters, numbers, hyphens, underscores
    const regex = /^[A-Z0-9_-]{3,50}$/;
    return regex.test(code);
  }
}

export const couponService = new CouponService();
export default couponService;