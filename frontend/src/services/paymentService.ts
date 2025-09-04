import { ApiResponse, ApiError } from '../types/api';

export interface PaymentIntent {
  id: string;
  clientSecret: string;
  amount: number;
  currency: string;
  status: string;
  metadata?: Record<string, string>;
}

export interface Payment {
  id: number;
  orderId: number;
  orderNumber: string;
  paymentMethodId?: number;
  transactionId: string;
  gatewayPaymentId?: string;
  provider: 'STRIPE' | 'PAYPAL';
  paymentType: 'CARD' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';
  status: 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED' | 'REFUNDED' | 'PARTIALLY_REFUNDED';
  amount: number;
  currency: string;
  refundedAmount: number;
  refundableAmount: number;
  gatewayTransactionFee?: number;
  failureReason?: string;
  authorizedAt?: string;
  capturedAt?: string;
  failedAt?: string;
  refundedAt?: string;
  cancelledAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface PaymentMethod {
  id: number;
  userId: string;
  provider: 'STRIPE' | 'PAYPAL';
  paymentType: 'CARD' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';
  displayName: string;
  cardLast4?: string;
  cardBrand?: string;
  cardExpMonth?: number;
  cardExpYear?: number;
  formattedExpiry?: string;
  cardHolderName?: string;
  maskedCardNumber?: string;
  bankLast4?: string;
  bankName?: string;
  accountType?: string;
  maskedAccountNumber?: string;
  walletEmail?: string;
  isDefault: boolean;
  isActive: boolean;
  isCardExpired: boolean;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreatePaymentIntentRequest {
  orderId: number;
  provider: 'STRIPE' | 'PAYPAL';
  amount?: number;
  currency?: string;
  metadata?: Record<string, string>;
}

export interface ProcessPaymentRequest {
  orderId: number;
  paymentMethodId: string;
  provider: 'STRIPE' | 'PAYPAL';
  paymentIntentId?: string;
  savePaymentMethod?: boolean;
  setAsDefault?: boolean;
  metadata?: Record<string, string>;
}

export interface SavePaymentMethodRequest {
  paymentMethodToken: string;
  provider: 'STRIPE' | 'PAYPAL';
  setAsDefault?: boolean;
}

export interface RefundPaymentRequest {
  paymentId: number;
  amount?: number;
  reason?: string;
}

class PaymentService {
  private baseUrl = '/api/payments';

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`;
    const token = localStorage.getItem('authToken');
    
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token && { Authorization: `Bearer ${token}` }),
        ...options.headers,
      },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({ message: 'An error occurred' }));
      throw new ApiError(error.message || 'Request failed', response.status, error);
    }

    return response.json();
  }

  /**
   * Create a payment intent for an order
   */
  async createPaymentIntent(request: CreatePaymentIntentRequest): Promise<PaymentIntent> {
    return this.request<PaymentIntent>('/intent', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  /**
   * Process a payment for an order
   */
  async processPayment(request: ProcessPaymentRequest): Promise<Payment> {
    return this.request<Payment>('/process', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  /**
   * Capture an authorized payment
   */
  async capturePayment(paymentId: number, amount?: number): Promise<Payment> {
    const body = amount ? JSON.stringify({ amount }) : undefined;
    
    return this.request<Payment>(`/${paymentId}/capture`, {
      method: 'POST',
      ...(body && { body }),
    });
  }

  /**
   * Refund a payment
   */
  async refundPayment(request: RefundPaymentRequest): Promise<Payment> {
    return this.request<Payment>('/refund', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  /**
   * Cancel a pending payment
   */
  async cancelPayment(paymentId: number): Promise<Payment> {
    return this.request<Payment>(`/${paymentId}/cancel`, {
      method: 'POST',
    });
  }

  /**
   * Get payments for a specific order
   */
  async getOrderPayments(orderId: number): Promise<Payment[]> {
    return this.request<Payment[]>(`/order/${orderId}`);
  }

  /**
   * Get current user's payments with pagination
   */
  async getUserPayments(page = 0, size = 20): Promise<ApiResponse<Payment[]>> {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });

    return this.request<ApiResponse<Payment[]>>(`/my?${params}`);
  }

  /**
   * Save a payment method for future use
   */
  async savePaymentMethod(request: SavePaymentMethodRequest): Promise<PaymentMethod> {
    return this.request<PaymentMethod>('/methods', {
      method: 'POST',
      body: JSON.stringify(request),
    });
  }

  /**
   * Get user's saved payment methods
   */
  async getPaymentMethods(): Promise<PaymentMethod[]> {
    return this.request<PaymentMethod[]>('/methods');
  }

  /**
   * Set a payment method as default
   */
  async setDefaultPaymentMethod(paymentMethodId: number): Promise<void> {
    await this.request<void>(`/methods/${paymentMethodId}/default`, {
      method: 'POST',
    });
  }

  /**
   * Remove a saved payment method
   */
  async removePaymentMethod(paymentMethodId: number): Promise<void> {
    await this.request<void>(`/methods/${paymentMethodId}`, {
      method: 'DELETE',
    });
  }

  /**
   * Format currency amount for display
   */
  formatAmount(amount: number, currency: string): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount / 100);
  }

  /**
   * Get payment status display text
   */
  getStatusDisplay(status: Payment['status']): { text: string; color: string } {
    switch (status) {
      case 'PENDING':
        return { text: 'Pending', color: 'yellow' };
      case 'PROCESSING':
        return { text: 'Processing', color: 'blue' };
      case 'SUCCEEDED':
        return { text: 'Completed', color: 'green' };
      case 'FAILED':
        return { text: 'Failed', color: 'red' };
      case 'CANCELLED':
        return { text: 'Cancelled', color: 'gray' };
      case 'REFUNDED':
        return { text: 'Refunded', color: 'orange' };
      case 'PARTIALLY_REFUNDED':
        return { text: 'Partially Refunded', color: 'orange' };
      default:
        return { text: status, color: 'gray' };
    }
  }

  /**
   * Check if payment can be refunded
   */
  canRefund(payment: Payment): boolean {
    return payment.status === 'SUCCEEDED' && payment.refundableAmount > 0;
  }

  /**
   * Check if payment can be cancelled
   */
  canCancel(payment: Payment): boolean {
    return ['PENDING', 'PROCESSING'].includes(payment.status);
  }

  /**
   * Validate card number using Luhn algorithm
   */
  validateCardNumber(cardNumber: string): boolean {
    const number = cardNumber.replace(/\s/g, '');
    
    if (!/^\d+$/.test(number) || number.length < 13 || number.length > 19) {
      return false;
    }

    // Luhn algorithm
    let sum = 0;
    let isEven = false;

    for (let i = number.length - 1; i >= 0; i--) {
      let digit = parseInt(number[i]);

      if (isEven) {
        digit *= 2;
        if (digit > 9) {
          digit -= 9;
        }
      }

      sum += digit;
      isEven = !isEven;
    }

    return sum % 10 === 0;
  }

  /**
   * Get card brand from card number
   */
  getCardBrand(cardNumber: string): string {
    const number = cardNumber.replace(/\s/g, '');
    
    if (number.startsWith('4')) return 'visa';
    if (number.startsWith('5') || (number.startsWith('2') && number.length >= 4)) return 'mastercard';
    if (number.startsWith('34') || number.startsWith('37')) return 'amex';
    if (number.startsWith('6011') || number.startsWith('65')) return 'discover';
    if (number.startsWith('35')) return 'jcb';
    
    return 'unknown';
  }

  /**
   * Format card number for display with spaces
   */
  formatCardNumber(cardNumber: string): string {
    const number = cardNumber.replace(/\s/g, '');
    return number.replace(/(.{4})/g, '$1 ').trim();
  }

  /**
   * Validate expiry date
   */
  validateExpiryDate(expiryDate: string): boolean {
    if (!/^\d{2}\/\d{2}$/.test(expiryDate)) {
      return false;
    }

    const [month, year] = expiryDate.split('/').map(Number);
    const currentDate = new Date();
    const currentYear = currentDate.getFullYear() % 100;
    const currentMonth = currentDate.getMonth() + 1;

    if (month < 1 || month > 12) {
      return false;
    }

    if (year < currentYear || (year === currentYear && month < currentMonth)) {
      return false;
    }

    return true;
  }
}

// Export singleton instance
export const paymentService = new PaymentService();
export default paymentService;