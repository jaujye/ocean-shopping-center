import React from 'react';
import { cn } from '../../utils/cn';
import { CouponValidationResponse, CouponType } from '../../services/couponService';

interface OrderSummaryData {
  subtotal: number;
  taxAmount?: number;
  shippingAmount?: number;
  discountAmount?: number;
  totalAmount: number;
  currency?: string;
}

interface DiscountSummaryProps {
  orderSummary: OrderSummaryData;
  appliedCoupons?: CouponValidationResponse[];
  showDetailedBreakdown?: boolean;
  className?: string;
}

const DiscountSummary: React.FC<DiscountSummaryProps> = ({
  orderSummary,
  appliedCoupons = [],
  showDetailedBreakdown = true,
  className
}) => {
  const { subtotal, taxAmount = 0, shippingAmount = 0, discountAmount = 0, totalAmount, currency = 'USD' } = orderSummary;
  
  const formatCurrency = (amount: number): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
      minimumFractionDigits: 2,
    }).format(amount);
  };

  const getDiscountIcon = (type?: CouponType) => {
    switch (type) {
      case CouponType.PERCENTAGE:
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM7 9a1 1 0 000 2h6a1 1 0 100-2H7z" clipRule="evenodd" />
          </svg>
        );
      case CouponType.FIXED_AMOUNT:
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M8.433 7.418c.155-.103.346-.196.567-.267v1.698a2.305 2.305 0 01-.567-.267C8.07 8.34 8 8.114 8 8c0-.114.07-.34.433-.582zM11 12.849v-1.698c.22.071.412.164.567.267.364.243.433.468.433.582 0 .114-.07.34-.433.582a2.305 2.305 0 01-.567.267z" />
            <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm1-13a1 1 0 10-2 0v.092a4.535 4.535 0 00-1.676.662C6.602 6.234 6 7.009 6 8c0 .99.602 1.765 1.324 2.246.48.32 1.054.545 1.676.662v1.941c-.391-.127-.68-.317-.843-.504a1 1 0 10-1.51 1.31c.562.649 1.413 1.076 2.353 1.253V15a1 1 0 102 0v-.092a4.535 4.535 0 001.676-.662C13.398 13.766 14 12.991 14 12c0-.99-.602-1.765-1.324-2.246A4.535 4.535 0 0011 9.092V7.151c.391.127.68.317.843.504a1 1 0 101.51-1.31c-.562-.649-1.413-1.076-2.353-1.253V5z" clipRule="evenodd" />
          </svg>
        );
      case CouponType.FREE_SHIPPING:
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path d="M8 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0zM15 16.5a1.5 1.5 0 11-3 0 1.5 1.5 0 013 0z" />
            <path d="M3 4a1 1 0 00-1 1v10a1 1 0 001 1h1.05a2.5 2.5 0 014.9 0H10a1 1 0 001-1V5a1 1 0 00-1-1H3zM14 7a1 1 0 00-1 1v6.05A2.5 2.5 0 0115.95 16H17a1 1 0 001-1v-5a1 1 0 00-.293-.707L16 7.586A1 1 0 0015.414 7H14z" />
          </svg>
        );
      default:
        return (
          <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M5 2a1 1 0 011 1v1h1a1 1 0 010 2H6v1a1 1 0 01-2 0V6H3a1 1 0 010-2h1V3a1 1 0 011-1zm0 10a1 1 0 011 1v1h1a1 1 0 110 2H6v1a1 1 0 11-2 0v-1H3a1 1 0 110-2h1v-1a1 1 0 011-1zM12 2a1 1 0 01.967.744L14.146 7.2 17.5 9.134a1 1 0 010 1.732l-3.354 1.935-1.18 4.455a1 1 0 01-1.933 0L9.854 12.8 6.5 10.866a1 1 0 010-1.732l3.354-1.935 1.18-4.455A1 1 0 0112 2z" clipRule="evenodd" />
          </svg>
        );
    }
  };

  const totalDiscountAmount = appliedCoupons.reduce((total, coupon) => {
    return total + (coupon.discountAmount || 0);
  }, 0);

  const hasFreeShipping = appliedCoupons.some(coupon => coupon.freeShipping);
  const originalShippingAmount = hasFreeShipping ? shippingAmount + appliedCoupons
    .filter(c => c.freeShipping)
    .reduce((total, c) => total + (c.discountAmount || 0), 0) : shippingAmount;

  return (
    <div className={cn("bg-gray-50 rounded-lg p-4 space-y-3", className)}>
      <h3 className="font-semibold text-gray-900">Order Summary</h3>
      
      {/* Subtotal */}
      <div className="flex justify-between text-sm">
        <span className="text-gray-600">Subtotal</span>
        <span className="text-gray-900">{formatCurrency(subtotal)}</span>
      </div>

      {/* Applied Coupons */}
      {appliedCoupons.length > 0 && (
        <div className="space-y-2">
          <div className="text-xs font-medium text-gray-500 uppercase tracking-wide">
            Applied Coupons
          </div>
          {appliedCoupons.map((coupon, index) => (
            <div key={index} className="flex items-center justify-between text-sm">
              <div className="flex items-center space-x-2">
                <div className="text-green-600">
                  {getDiscountIcon(coupon.type)}
                </div>
                <div>
                  <span className="font-medium text-green-700">{coupon.code}</span>
                  {coupon.name && (
                    <div className="text-xs text-gray-500">{coupon.name}</div>
                  )}
                </div>
              </div>
              <span className="font-medium text-green-700">
                {coupon.freeShipping ? (
                  'Free shipping'
                ) : (
                  `-${formatCurrency(coupon.discountAmount || 0)}`
                )}
              </span>
            </div>
          ))}
        </div>
      )}

      {/* Total Discount */}
      {totalDiscountAmount > 0 && (
        <div className="flex justify-between text-sm border-t pt-2">
          <span className="text-green-600 font-medium">Total Discount</span>
          <span className="text-green-700 font-medium">-{formatCurrency(totalDiscountAmount)}</span>
        </div>
      )}

      {/* Shipping */}
      {showDetailedBreakdown && (
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">
            Shipping
            {hasFreeShipping && (
              <span className="ml-1 text-xs text-green-600">(Free)</span>
            )}
          </span>
          <div className="text-right">
            {hasFreeShipping ? (
              <div className="space-y-1">
                <div className="text-gray-400 line-through text-xs">
                  {formatCurrency(originalShippingAmount)}
                </div>
                <div className="text-green-700 font-medium">
                  {formatCurrency(0)}
                </div>
              </div>
            ) : (
              <span className="text-gray-900">{formatCurrency(shippingAmount)}</span>
            )}
          </div>
        </div>
      )}

      {/* Tax */}
      {showDetailedBreakdown && taxAmount > 0 && (
        <div className="flex justify-between text-sm">
          <span className="text-gray-600">Tax</span>
          <span className="text-gray-900">{formatCurrency(taxAmount)}</span>
        </div>
      )}

      {/* Total */}
      <div className="border-t pt-3">
        <div className="flex justify-between items-center">
          <span className="text-lg font-semibold text-gray-900">Total</span>
          <div className="text-right">
            <div className="text-lg font-bold text-gray-900">
              {formatCurrency(totalAmount)}
            </div>
            {totalDiscountAmount > 0 && (
              <div className="text-sm text-green-600">
                You saved {formatCurrency(totalDiscountAmount)}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Savings Summary */}
      {appliedCoupons.length > 0 && (
        <div className="bg-green-50 border border-green-200 rounded-md p-3">
          <div className="flex items-center space-x-2">
            <svg className="w-5 h-5 text-green-600" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M6.267 3.455a3.066 3.066 0 001.745-.723 3.066 3.066 0 013.976 0 3.066 3.066 0 001.745.723 3.066 3.066 0 012.812 2.812c.051.643.304 1.254.723 1.745a3.066 3.066 0 010 3.976 3.066 3.066 0 00-.723 1.745 3.066 3.066 0 01-2.812 2.812 3.066 3.066 0 00-1.745.723 3.066 3.066 0 01-3.976 0 3.066 3.066 0 00-1.745-.723 3.066 3.066 0 01-2.812-2.812 3.066 3.066 0 00-.723-1.745 3.066 3.066 0 010-3.976 3.066 3.066 0 00.723-1.745 3.066 3.066 0 012.812-2.812zm7.44 5.252a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
            <div>
              <div className="text-sm font-medium text-green-800">
                {appliedCoupons.length === 1 ? 'Coupon Applied!' : `${appliedCoupons.length} Coupons Applied!`}
              </div>
              {totalDiscountAmount > 0 && (
                <div className="text-xs text-green-600">
                  Total savings: {formatCurrency(totalDiscountAmount)}
                  {hasFreeShipping && ' + Free shipping'}
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default DiscountSummary;