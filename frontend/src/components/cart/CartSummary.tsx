import React from 'react';
import { Cart } from '../../types';
import { cn } from '../../utils/cn';
import Button from '../ui/Button';

// Icons
const ShoppingCartIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 3h2l.4 2M7 13h10l4-8H5.4m0 0L7 13m0 0l-2.5 5M7 13l2.5 5m6-5v5a2 2 0 01-2 2H9a2 2 0 01-2-2v-5m6-5V4a2 2 0 00-2-2H9a2 2 0 00-2 2v4.01" />
  </svg>
);

const TagIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
  </svg>
);

const TruckIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
  </svg>
);

interface CartSummaryProps {
  cart: Cart | null;
  onCheckout?: () => void;
  onContinueShopping?: () => void;
  isCheckoutLoading?: boolean;
  showCheckoutButton?: boolean;
  showContinueShoppingButton?: boolean;
  className?: string;
}

const CartSummary: React.FC<CartSummaryProps> = ({
  cart,
  onCheckout,
  onContinueShopping,
  isCheckoutLoading = false,
  showCheckoutButton = true,
  showContinueShoppingButton = true,
  className
}) => {
  if (!cart || cart.items.length === 0) {
    return (
      <div className={cn("bg-white rounded-lg shadow-sm border border-gray-200 p-6", className)}>
        <div className="text-center">
          <ShoppingCartIcon className="w-12 h-12 text-gray-300 mx-auto mb-4" />
          <h3 className="text-lg font-medium text-gray-900 mb-2">Your cart is empty</h3>
          <p className="text-gray-500 mb-4">Start shopping to add items to your cart</p>
          {showContinueShoppingButton && (
            <Button onClick={onContinueShopping} variant="primary">
              Start Shopping
            </Button>
          )}
        </div>
      </div>
    );
  }

  const hasDiscount = (cart.discountAmount ?? 0) > 0 || (cart.couponDiscount ?? 0) > 0;
  const totalSavings = (cart.discountAmount ?? 0) + (cart.couponDiscount ?? 0);

  return (
    <div className={cn("bg-white rounded-lg shadow-sm border border-gray-200", className)}>
      {/* Header */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-center justify-between">
          <h3 className="text-lg font-semibold text-gray-900">Order Summary</h3>
          <div className="flex items-center text-sm text-gray-500">
            <ShoppingCartIcon className="w-4 h-4 mr-1" />
            {cart.totalItems} {cart.totalItems === 1 ? 'item' : 'items'}
          </div>
        </div>
      </div>

      {/* Pricing Details */}
      <div className="p-6 space-y-4">
        {/* Subtotal */}
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">
            Subtotal ({cart.totalItems} {cart.totalItems === 1 ? 'item' : 'items'})
          </span>
          <span className="font-medium text-gray-900">
            ${cart.subtotal.toFixed(2)}
          </span>
        </div>

        {/* Applied Coupon */}
        {cart.appliedCouponCode && (cart.couponDiscount ?? 0) > 0 && (
          <div className="flex justify-between text-sm">
            <div className="flex items-center text-green-600">
              <TagIcon className="w-4 h-4 mr-1" />
              <span>Coupon ({cart.appliedCouponCode})</span>
            </div>
            <span className="font-medium text-green-600">
              -${(cart.couponDiscount ?? 0).toFixed(2)}
            </span>
          </div>
        )}

        {/* Item Discounts */}
        {(cart.discountAmount ?? 0) > 0 && (
          <div className="flex justify-between text-sm">
            <span className="text-green-600">Item discounts</span>
            <span className="font-medium text-green-600">
              -${(cart.discountAmount ?? 0).toFixed(2)}
            </span>
          </div>
        )}

        {/* Tax */}
        {cart.taxAmount > 0 && (
          <div className="flex justify-between text-sm">
            <span className="text-gray-600">Tax</span>
            <span className="font-medium text-gray-900">
              ${cart.taxAmount.toFixed(2)}
            </span>
          </div>
        )}

        {/* Shipping */}
        <div className="flex justify-between text-sm">
          <div className="flex items-center">
            <TruckIcon className="w-4 h-4 mr-1 text-gray-400" />
            <span className="text-gray-600">Shipping</span>
          </div>
          <span className="font-medium text-gray-900">
            {cart.shippingFee > 0 ? (
              `$${cart.shippingFee.toFixed(2)}`
            ) : (
              <span className="text-green-600">FREE</span>
            )}
          </span>
        </div>

        {/* Total Savings */}
        {hasDiscount && (
          <div className="flex justify-between text-sm pt-2 border-t border-gray-100">
            <span className="text-green-600 font-medium">Total Savings</span>
            <span className="font-medium text-green-600">
              ${totalSavings.toFixed(2)}
            </span>
          </div>
        )}

        {/* Total */}
        <div className="flex justify-between text-base font-semibold pt-4 border-t border-gray-200">
          <span className="text-gray-900">Total</span>
          <div className="text-right">
            <span className="text-ocean-600 text-lg">
              ${cart.total.toFixed(2)}
            </span>
            <div className="text-xs text-gray-500 mt-1">
              {cart.currency}
            </div>
          </div>
        </div>
      </div>

      {/* Action Buttons */}
      {(showCheckoutButton || showContinueShoppingButton) && (
        <div className="p-6 border-t border-gray-200 space-y-3">
          {showCheckoutButton && (
            <Button
              onClick={onCheckout}
              variant="primary"
              size="lg"
              fullWidth
              disabled={isCheckoutLoading || cart.items.length === 0}
              loading={isCheckoutLoading}
            >
              {isCheckoutLoading ? 'Processing...' : 'Proceed to Checkout'}
            </Button>
          )}
          
          {showContinueShoppingButton && (
            <Button
              onClick={onContinueShopping}
              variant="outline"
              size="md"
              fullWidth
            >
              Continue Shopping
            </Button>
          )}
        </div>
      )}

      {/* Free Shipping Promotion */}
      {cart.shippingFee === 0 && cart.subtotal >= 50 && (
        <div className="px-6 pb-6">
          <div className="bg-green-50 border border-green-200 rounded-lg p-3">
            <div className="flex items-center">
              <TruckIcon className="w-4 h-4 text-green-600 mr-2" />
              <span className="text-sm text-green-800 font-medium">
                Congratulations! You qualify for free shipping
              </span>
            </div>
          </div>
        </div>
      )}

      {/* Almost Free Shipping */}
      {cart.shippingFee > 0 && cart.subtotal < 50 && (
        <div className="px-6 pb-6">
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <TruckIcon className="w-4 h-4 text-blue-600 mr-2" />
                <span className="text-sm text-blue-800">
                  Add ${(50 - cart.subtotal).toFixed(2)} more for FREE shipping
                </span>
              </div>
            </div>
            <div className="mt-2">
              <div className="w-full bg-blue-200 rounded-full h-2">
                <div
                  className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${Math.min((cart.subtotal / 50) * 100, 100)}%` }}
                ></div>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Security Badge */}
      <div className="px-6 pb-6">
        <div className="text-center">
          <div className="inline-flex items-center text-xs text-gray-500">
            <svg className="w-4 h-4 mr-1 text-green-500" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M5 9V7a5 5 0 0110 0v2a2 2 0 012 2v5a2 2 0 01-2 2H5a2 2 0 01-2-2v-5a2 2 0 012-2zm8-2v2H7V7a3 3 0 016 0z" clipRule="evenodd" />
            </svg>
            Secure checkout protected by SSL encryption
          </div>
        </div>
      </div>
    </div>
  );
};

export default CartSummary;