import React, { useState } from 'react';
import CouponInput from '../coupons/CouponInput';
import { CouponValidationResponse } from '../../services/couponService';

interface CartItem {
  id: number;
  product: {
    id: number;
    name: string;
    imageUrl?: string;
    price: number;
  };
  quantity: number;
  unitPrice: number;
  totalPrice: number;
}

interface Cart {
  id: number;
  items: CartItem[];
  itemCount: number;
  subtotal: number;
  taxAmount?: number;
  shippingAmount?: number;
  discountAmount?: number;
  totalAmount: number;
}

interface OrderSummaryProps {
  cart: Cart;
  couponCode?: string;
  onCouponCodeChange?: (code: string) => void;
  onApplyCoupon?: () => void;
  onRemoveCoupon?: () => void;
  appliedCoupon?: CouponValidationResponse | null;
  onCouponApplied?: (coupon: CouponValidationResponse) => void;
  onCouponRemoved?: () => void;
  storeId?: number;
  customerEmail?: string;
  className?: string;
  showAdvancedCouponInput?: boolean;
}

const OrderSummary: React.FC<OrderSummaryProps> = ({
  cart,
  couponCode = '',
  onCouponCodeChange,
  onApplyCoupon,
  onRemoveCoupon,
  appliedCoupon,
  onCouponApplied,
  onCouponRemoved,
  storeId,
  customerEmail,
  className = '',
  showAdvancedCouponInput = false
}) => {
  const [isExpanded, setIsExpanded] = useState(true);

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
    }).format(amount);
  };

  const subtotal = cart.subtotal || cart.items.reduce((sum, item) => sum + item.totalPrice, 0);
  const taxAmount = cart.taxAmount || 0;
  const shippingAmount = cart.shippingAmount || 0;
  const discountAmount = cart.discountAmount || 0;
  const totalAmount = subtotal + taxAmount + shippingAmount - discountAmount;

  return (
    <div className={`bg-white rounded-lg shadow-sm border border-gray-200 ${className}`}>
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-medium text-gray-900">
            Order Summary
          </h3>
          <button
            onClick={() => setIsExpanded(!isExpanded)}
            className="text-sm text-blue-600 hover:text-blue-500 lg:hidden"
          >
            {isExpanded ? 'Hide' : 'Show'} details
          </button>
        </div>
        <p className="text-sm text-gray-600 mt-1">
          {cart.itemCount} {cart.itemCount === 1 ? 'item' : 'items'} in your cart
        </p>
      </div>

      {/* Order Items */}
      <div className={`${isExpanded ? 'block' : 'hidden lg:block'}`}>
        <div className="p-6 border-b border-gray-200">
          <h4 className="text-sm font-medium text-gray-900 mb-4">Items</h4>
          <div className="space-y-4">
            {cart.items.map((item) => (
              <div key={item.id} className="flex items-start">
                <div className="flex-shrink-0">
                  {item.product.imageUrl ? (
                    <img
                      src={item.product.imageUrl}
                      alt={item.product.name}
                      className="w-12 h-12 object-cover rounded-md"
                    />
                  ) : (
                    <div className="w-12 h-12 bg-gray-200 rounded-md flex items-center justify-center">
                      <svg className="w-6 h-6 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z"></path>
                      </svg>
                    </div>
                  )}
                </div>
                <div className="ml-3 flex-1">
                  <p className="text-sm font-medium text-gray-900">{item.product.name}</p>
                  <div className="mt-1 flex items-center justify-between">
                    <p className="text-sm text-gray-600">Qty: {item.quantity}</p>
                    <p className="text-sm font-medium text-gray-900">
                      {formatCurrency(item.totalPrice)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Coupon Code */}
        {showAdvancedCouponInput ? (
          <div className="p-6 border-b border-gray-200">
            <h4 className="text-sm font-medium text-gray-900 mb-3">Promo Code</h4>
            <CouponInput
              orderAmount={subtotal}
              storeId={storeId}
              customerEmail={customerEmail}
              onCouponApplied={onCouponApplied}
              onCouponRemoved={onCouponRemoved}
              appliedCoupon={appliedCoupon}
            />
          </div>
        ) : (onCouponCodeChange || onApplyCoupon) && (
          <div className="p-6 border-b border-gray-200">
            <h4 className="text-sm font-medium text-gray-900 mb-3">Promo Code</h4>
            <div className="flex">
              <div className="flex-1">
                <input
                  type="text"
                  value={couponCode}
                  onChange={(e) => onCouponCodeChange?.(e.target.value)}
                  placeholder="Enter promo code"
                  className="w-full px-3 py-2 border border-gray-300 rounded-l-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                />
              </div>
              <button
                onClick={onApplyCoupon}
                disabled={!couponCode.trim()}
                className="px-4 py-2 bg-blue-600 text-white rounded-r-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                Apply
              </button>
            </div>
            {discountAmount > 0 && onRemoveCoupon && (
              <div className="mt-2 flex items-center justify-between bg-green-50 p-2 rounded">
                <span className="text-sm text-green-800">Promo applied!</span>
                <button
                  onClick={onRemoveCoupon}
                  className="text-sm text-red-600 hover:text-red-500"
                >
                  Remove
                </button>
              </div>
            )}
          </div>
        )}

        {/* Order Totals */}
        <div className="p-6">
          <h4 className="text-sm font-medium text-gray-900 mb-4">Order Total</h4>
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">Subtotal</span>
              <span className="text-sm text-gray-900">{formatCurrency(subtotal)}</span>
            </div>

            {discountAmount > 0 && (
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">Discount</span>
                <span className="text-sm text-green-600">-{formatCurrency(discountAmount)}</span>
              </div>
            )}

            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">Shipping</span>
              <span className="text-sm text-gray-900">
                {shippingAmount > 0 ? formatCurrency(shippingAmount) : 'Calculated at next step'}
              </span>
            </div>

            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">Tax</span>
              <span className="text-sm text-gray-900">
                {taxAmount > 0 ? formatCurrency(taxAmount) : 'Calculated at next step'}
              </span>
            </div>

            <hr className="my-3" />

            <div className="flex items-center justify-between">
              <span className="text-base font-medium text-gray-900">Total</span>
              <span className="text-base font-bold text-gray-900">
                {formatCurrency(totalAmount)}
              </span>
            </div>
          </div>

          {/* Free Shipping Indicator */}
          {subtotal < 100 && (
            <div className="mt-4 bg-blue-50 border border-blue-200 rounded-md p-3">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-blue-400" viewBox="0 0 20 20" fill="currentColor">
                    <path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z" />
                    <path d="M3 4a1 1 0 00-1 1v10a1 1 0 001 1h1.05a2.5 2.5 0 014.9 0H10a1 1 0 001-1V5a1 1 0 00-1-1H3zM14 7a1 1 0 00-1 1v6.05A2.5 2.5 0 0115.95 16H17a1 1 0 001-1V8a1 1 0 00-1-1h-3z" />
                  </svg>
                </div>
                <div className="ml-3">
                  <p className="text-sm text-blue-800">
                    Add {formatCurrency(100 - subtotal)} more for free shipping!
                  </p>
                </div>
              </div>
            </div>
          )}

          {subtotal >= 100 && (
            <div className="mt-4 bg-green-50 border border-green-200 rounded-md p-3">
              <div className="flex">
                <div className="flex-shrink-0">
                  <svg className="h-5 w-5 text-green-400" viewBox="0 0 20 20" fill="currentColor">
                    <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                  </svg>
                </div>
                <div className="ml-3">
                  <p className="text-sm text-green-800 font-medium">
                    🎉 You qualify for free shipping!
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Security Notice */}
        <div className="px-6 pb-6">
          <div className="bg-gray-50 border border-gray-200 rounded-md p-3">
            <div className="flex">
              <div className="flex-shrink-0">
                <svg className="h-5 w-5 text-gray-400" viewBox="0 0 20 20" fill="currentColor">
                  <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                </svg>
              </div>
              <div className="ml-3">
                <p className="text-xs text-gray-600">
                  Secure SSL encrypted checkout. Your payment information is safe.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default OrderSummary;