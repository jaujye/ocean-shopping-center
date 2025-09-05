import React, { useState, useCallback } from 'react';
import { cn } from '../../utils/cn';
import Input from '../ui/Input';
import Button from '../ui/Button';
import LoadingSpinner from '../ui/LoadingSpinner';
import { couponService, CouponValidationRequest, CouponValidationResponse } from '../../services/couponService';

interface CouponInputProps {
  orderAmount: number;
  storeId?: number;
  customerEmail?: string;
  currency?: string;
  onCouponApplied?: (coupon: CouponValidationResponse) => void;
  onCouponRemoved?: () => void;
  appliedCoupon?: CouponValidationResponse | null;
  disabled?: boolean;
  className?: string;
  placeholder?: string;
}

const CouponInput: React.FC<CouponInputProps> = ({
  orderAmount,
  storeId,
  customerEmail,
  currency = 'USD',
  onCouponApplied,
  onCouponRemoved,
  appliedCoupon,
  disabled = false,
  className,
  placeholder = "Enter coupon code"
}) => {
  const [couponCode, setCouponCode] = useState('');
  const [isValidating, setIsValidating] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [validationSuccess, setValidationSuccess] = useState<string | null>(null);

  const handleValidateAndApply = useCallback(async () => {
    if (!couponCode.trim()) {
      setValidationError('Please enter a coupon code');
      return;
    }

    // Basic format validation
    if (!couponService.isValidCouponFormat(couponCode.trim().toUpperCase())) {
      setValidationError('Invalid coupon code format');
      return;
    }

    setIsValidating(true);
    setValidationError(null);
    setValidationSuccess(null);

    try {
      const request: CouponValidationRequest = {
        code: couponCode.trim().toUpperCase(),
        orderAmount,
        storeId,
        customerEmail,
        currency
      };

      const response = await couponService.validateCoupon(request);
      
      if (response.valid) {
        setValidationSuccess(`${response.message || 'Coupon applied successfully!'}`);
        onCouponApplied?.(response);
        setCouponCode('');
      } else {
        setValidationError(response.errorMessage || 'Invalid coupon code');
      }
    } catch (error: any) {
      console.error('Error validating coupon:', error);
      setValidationError(error.message || 'Failed to validate coupon');
    } finally {
      setIsValidating(false);
    }
  }, [couponCode, orderAmount, storeId, customerEmail, currency, onCouponApplied]);

  const handleRemoveCoupon = useCallback(() => {
    setValidationError(null);
    setValidationSuccess(null);
    setCouponCode('');
    onCouponRemoved?.();
  }, [onCouponRemoved]);

  const handleKeyPress = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !disabled && !isValidating) {
      e.preventDefault();
      handleValidateAndApply();
    }
  }, [disabled, isValidating, handleValidateAndApply]);

  const handleInputChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value.toUpperCase().replace(/[^A-Z0-9_-]/g, '');
    setCouponCode(value);
    setValidationError(null);
    setValidationSuccess(null);
  }, []);

  return (
    <div className={cn("space-y-3", className)}>
      {/* Applied Coupon Display */}
      {appliedCoupon && appliedCoupon.valid && (
        <div className="flex items-center justify-between p-3 bg-green-50 border border-green-200 rounded-lg">
          <div className="flex-1">
            <div className="flex items-center space-x-2">
              <div className="w-2 h-2 bg-green-500 rounded-full"></div>
              <span className="text-sm font-medium text-green-800">
                {appliedCoupon.code}
              </span>
              {appliedCoupon.name && (
                <span className="text-sm text-green-600">
                  - {appliedCoupon.name}
                </span>
              )}
            </div>
            <div className="mt-1 text-sm text-green-600">
              {appliedCoupon.freeShipping ? (
                'Free shipping applied'
              ) : (
                `Save ${currency === 'USD' ? '$' : ''}${appliedCoupon.discountAmount?.toFixed(2)} 
                ${appliedCoupon.discountPercentage ? `(${appliedCoupon.discountPercentage.toFixed(1)}%)` : ''}`
              )}
            </div>
          </div>
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleRemoveCoupon}
            disabled={disabled}
            className="text-green-700 hover:text-green-900 hover:bg-green-100"
          >
            Remove
          </Button>
        </div>
      )}

      {/* Coupon Input Form */}
      {!appliedCoupon?.valid && (
        <div className="space-y-3">
          <div className="flex space-x-2">
            <div className="flex-1">
              <Input
                type="text"
                value={couponCode}
                onChange={handleInputChange}
                onKeyPress={handleKeyPress}
                placeholder={placeholder}
                disabled={disabled || isValidating}
                maxLength={50}
                className={cn(
                  validationError && "border-red-300 focus:border-red-500",
                  validationSuccess && "border-green-300 focus:border-green-500"
                )}
              />
            </div>
            <Button
              type="button"
              onClick={handleValidateAndApply}
              disabled={disabled || isValidating || !couponCode.trim()}
              className="min-w-[100px]"
            >
              {isValidating ? (
                <LoadingSpinner size="sm" />
              ) : (
                'Apply'
              )}
            </Button>
          </div>

          {/* Validation Messages */}
          {validationError && (
            <div className="flex items-center space-x-2 text-sm text-red-600">
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
              </svg>
              <span>{validationError}</span>
            </div>
          )}

          {validationSuccess && (
            <div className="flex items-center space-x-2 text-sm text-green-600">
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
              </svg>
              <span>{validationSuccess}</span>
            </div>
          )}
        </div>
      )}

      {/* Help Text */}
      <div className="text-xs text-gray-500">
        Enter a valid coupon code to apply discounts or get free shipping
      </div>
    </div>
  );
};

export default CouponInput;