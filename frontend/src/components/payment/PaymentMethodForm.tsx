import React, { useState, useEffect } from 'react';
import { cn } from '../../utils/cn';
import Input from '../ui/Input';
import Button from '../ui/Button';
import Card from '../ui/Card';
import LoadingSpinner from '../ui/LoadingSpinner';

// Payment method types
type PaymentProvider = 'STRIPE' | 'PAYPAL';
type PaymentType = 'CARD' | 'BANK_TRANSFER' | 'DIGITAL_WALLET';

interface PaymentMethod {
  id: number;
  provider: PaymentProvider;
  paymentType: PaymentType;
  displayName: string;
  cardLast4?: string;
  cardBrand?: string;
  cardExpMonth?: number;
  cardExpYear?: number;
  maskedCardNumber?: string;
  isDefault: boolean;
  isActive: boolean;
  isCardExpired: boolean;
}

interface PaymentMethodFormProps {
  orderId?: number;
  amount?: number;
  currency?: string;
  onPaymentSuccess?: (payment: any) => void;
  onPaymentError?: (error: string) => void;
  className?: string;
  showSavedMethods?: boolean;
  allowSaveMethod?: boolean;
}

interface CardFormData {
  cardNumber: string;
  expiryDate: string;
  cvv: string;
  cardholderName: string;
  saveForFuture: boolean;
  setAsDefault: boolean;
}

const PaymentMethodForm: React.FC<PaymentMethodFormProps> = ({
  orderId,
  amount,
  currency = 'USD',
  onPaymentSuccess,
  onPaymentError,
  className,
  showSavedMethods = true,
  allowSaveMethod = true,
}) => {
  const [paymentMethods, setPaymentMethods] = useState<PaymentMethod[]>([]);
  const [selectedMethod, setSelectedMethod] = useState<'new' | number>('new');
  const [isLoading, setIsLoading] = useState(false);
  const [isLoadingMethods, setIsLoadingMethods] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  
  const [cardForm, setCardForm] = useState<CardFormData>({
    cardNumber: '',
    expiryDate: '',
    cvv: '',
    cardholderName: '',
    saveForFuture: false,
    setAsDefault: false,
  });

  // Load saved payment methods
  useEffect(() => {
    if (showSavedMethods) {
      loadPaymentMethods();
    }
  }, [showSavedMethods]);

  const loadPaymentMethods = async () => {
    setIsLoadingMethods(true);
    try {
      const response = await fetch('/api/payments/methods', {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('authToken')}`,
          'Content-Type': 'application/json',
        },
      });

      if (response.ok) {
        const methods = await response.json();
        setPaymentMethods(methods);
        
        // Auto-select default method if available
        const defaultMethod = methods.find((m: PaymentMethod) => m.isDefault);
        if (defaultMethod && selectedMethod === 'new') {
          setSelectedMethod(defaultMethod.id);
        }
      }
    } catch (error) {
      console.error('Failed to load payment methods:', error);
    } finally {
      setIsLoadingMethods(false);
    }
  };

  const validateCardForm = (): boolean => {
    const newErrors: Record<string, string> = {};

    // Card number validation (basic)
    const cleanCardNumber = cardForm.cardNumber.replace(/\s/g, '');
    if (!cleanCardNumber) {
      newErrors.cardNumber = 'Card number is required';
    } else if (cleanCardNumber.length < 13 || cleanCardNumber.length > 19) {
      newErrors.cardNumber = 'Invalid card number';
    }

    // Expiry date validation
    if (!cardForm.expiryDate) {
      newErrors.expiryDate = 'Expiry date is required';
    } else if (!/^\d{2}\/\d{2}$/.test(cardForm.expiryDate)) {
      newErrors.expiryDate = 'Invalid format (MM/YY)';
    } else {
      const [month, year] = cardForm.expiryDate.split('/').map(Number);
      const currentDate = new Date();
      const currentYear = currentDate.getFullYear() % 100;
      const currentMonth = currentDate.getMonth() + 1;
      
      if (month < 1 || month > 12) {
        newErrors.expiryDate = 'Invalid month';
      } else if (year < currentYear || (year === currentYear && month < currentMonth)) {
        newErrors.expiryDate = 'Card has expired';
      }
    }

    // CVV validation
    if (!cardForm.cvv) {
      newErrors.cvv = 'CVV is required';
    } else if (!/^\d{3,4}$/.test(cardForm.cvv)) {
      newErrors.cvv = 'Invalid CVV';
    }

    // Cardholder name validation
    if (!cardForm.cardholderName.trim()) {
      newErrors.cardholderName = 'Cardholder name is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleCardNumberChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\s/g, '');
    // Add spaces every 4 digits for display
    const formatted = value.replace(/(.{4})/g, '$1 ').trim();
    setCardForm(prev => ({ ...prev, cardNumber: formatted }));
  };

  const handleExpiryChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    let value = e.target.value.replace(/\D/g, '');
    if (value.length >= 2) {
      value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    setCardForm(prev => ({ ...prev, expiryDate: value }));
  };

  const getCardBrand = (cardNumber: string): string => {
    const number = cardNumber.replace(/\s/g, '');
    if (number.startsWith('4')) return 'visa';
    if (number.startsWith('5') || number.startsWith('2')) return 'mastercard';
    if (number.startsWith('3')) return 'amex';
    if (number.startsWith('6')) return 'discover';
    return 'unknown';
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setErrors({});

    try {
      if (selectedMethod === 'new') {
        // Validate new card form
        if (!validateCardForm()) {
          setIsLoading(false);
          return;
        }

        // In a real implementation, you would:
        // 1. Create payment intent with backend
        // 2. Use Stripe.js or similar to tokenize card
        // 3. Confirm payment with tokenized method
        
        // Simulate payment processing
        await new Promise(resolve => setTimeout(resolve, 2000));
        
        // Mock successful payment
        const mockPayment = {
          id: Math.floor(Math.random() * 1000),
          status: 'succeeded',
          amount: amount || 0,
          currency: currency,
          orderId: orderId,
        };

        onPaymentSuccess?.(mockPayment);
      } else {
        // Process with saved payment method
        const savedMethod = paymentMethods.find(m => m.id === selectedMethod);
        if (!savedMethod) {
          throw new Error('Selected payment method not found');
        }

        // Simulate payment processing with saved method
        await new Promise(resolve => setTimeout(resolve, 1500));
        
        const mockPayment = {
          id: Math.floor(Math.random() * 1000),
          status: 'succeeded',
          amount: amount || 0,
          currency: currency,
          orderId: orderId,
          paymentMethodId: savedMethod.id,
        };

        onPaymentSuccess?.(mockPayment);
      }
    } catch (error) {
      const errorMessage = error instanceof Error ? error.message : 'Payment failed';
      onPaymentError?.(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  const formatAmount = (amount: number, currency: string): string => {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: currency,
    }).format(amount / 100);
  };

  return (
    <Card className={cn('max-w-md mx-auto', className)}>
      <div className="p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-6">
          Payment Method
        </h3>

        {amount && (
          <div className="mb-6 p-4 bg-gray-50 rounded-lg">
            <div className="flex justify-between items-center">
              <span className="text-sm text-gray-600">Total Amount:</span>
              <span className="text-lg font-bold text-gray-900">
                {formatAmount(amount, currency)}
              </span>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* Saved Payment Methods */}
          {showSavedMethods && paymentMethods.length > 0 && (
            <div className="space-y-3">
              <label className="text-sm font-medium text-gray-700">
                Select Payment Method
              </label>
              
              {isLoadingMethods ? (
                <div className="flex justify-center py-4">
                  <LoadingSpinner />
                </div>
              ) : (
                <div className="space-y-2">
                  {paymentMethods.map((method) => (
                    <label
                      key={method.id}
                      className={cn(
                        'flex items-center p-3 border rounded-lg cursor-pointer transition-colors',
                        selectedMethod === method.id
                          ? 'border-ocean-500 bg-ocean-50'
                          : 'border-gray-200 hover:border-gray-300'
                      )}
                    >
                      <input
                        type="radio"
                        name="paymentMethod"
                        value={method.id}
                        checked={selectedMethod === method.id}
                        onChange={(e) => setSelectedMethod(parseInt(e.target.value))}
                        className="text-ocean-600 focus:ring-ocean-500"
                      />
                      <div className="ml-3 flex-1">
                        <div className="flex items-center justify-between">
                          <div>
                            <p className="text-sm font-medium text-gray-900">
                              {method.displayName}
                            </p>
                            {method.maskedCardNumber && (
                              <p className="text-sm text-gray-500">
                                {method.maskedCardNumber}
                              </p>
                            )}
                          </div>
                          {method.isDefault && (
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-ocean-100 text-ocean-800">
                              Default
                            </span>
                          )}
                        </div>
                        {method.isCardExpired && (
                          <p className="text-sm text-red-600 mt-1">
                            Card expired
                          </p>
                        )}
                      </div>
                    </label>
                  ))}
                  
                  <label
                    className={cn(
                      'flex items-center p-3 border rounded-lg cursor-pointer transition-colors',
                      selectedMethod === 'new'
                        ? 'border-ocean-500 bg-ocean-50'
                        : 'border-gray-200 hover:border-gray-300'
                    )}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value="new"
                      checked={selectedMethod === 'new'}
                      onChange={(e) => setSelectedMethod('new')}
                      className="text-ocean-600 focus:ring-ocean-500"
                    />
                    <div className="ml-3">
                      <p className="text-sm font-medium text-gray-900">
                        Use new payment method
                      </p>
                    </div>
                  </label>
                </div>
              )}
            </div>
          )}

          {/* New Card Form */}
          {selectedMethod === 'new' && (
            <div className="space-y-4 mt-6">
              <Input
                label="Card Number"
                type="text"
                value={cardForm.cardNumber}
                onChange={handleCardNumberChange}
                placeholder="1234 5678 9012 3456"
                maxLength={19}
                error={errors.cardNumber}
                rightIcon={
                  cardForm.cardNumber && (
                    <span className={cn(
                      'text-xs font-medium px-2 py-1 rounded',
                      getCardBrand(cardForm.cardNumber) === 'visa' ? 'bg-blue-100 text-blue-800' :
                      getCardBrand(cardForm.cardNumber) === 'mastercard' ? 'bg-red-100 text-red-800' :
                      getCardBrand(cardForm.cardNumber) === 'amex' ? 'bg-green-100 text-green-800' :
                      'bg-gray-100 text-gray-800'
                    )}>
                      {getCardBrand(cardForm.cardNumber).toUpperCase()}
                    </span>
                  )
                }
              />

              <div className="grid grid-cols-2 gap-4">
                <Input
                  label="Expiry Date"
                  type="text"
                  value={cardForm.expiryDate}
                  onChange={handleExpiryChange}
                  placeholder="MM/YY"
                  maxLength={5}
                  error={errors.expiryDate}
                />
                <Input
                  label="CVV"
                  type="text"
                  value={cardForm.cvv}
                  onChange={(e) => setCardForm(prev => ({ 
                    ...prev, 
                    cvv: e.target.value.replace(/\D/g, '').substring(0, 4) 
                  }))}
                  placeholder="123"
                  maxLength={4}
                  error={errors.cvv}
                />
              </div>

              <Input
                label="Cardholder Name"
                type="text"
                value={cardForm.cardholderName}
                onChange={(e) => setCardForm(prev => ({ 
                  ...prev, 
                  cardholderName: e.target.value 
                }))}
                placeholder="John Doe"
                error={errors.cardholderName}
              />

              {allowSaveMethod && (
                <div className="space-y-2">
                  <label className="flex items-center">
                    <input
                      type="checkbox"
                      checked={cardForm.saveForFuture}
                      onChange={(e) => setCardForm(prev => ({ 
                        ...prev, 
                        saveForFuture: e.target.checked,
                        setAsDefault: e.target.checked ? prev.setAsDefault : false
                      }))}
                      className="rounded border-gray-300 text-ocean-600 focus:ring-ocean-500"
                    />
                    <span className="ml-2 text-sm text-gray-700">
                      Save payment method for future purchases
                    </span>
                  </label>
                  
                  {cardForm.saveForFuture && (
                    <label className="flex items-center ml-6">
                      <input
                        type="checkbox"
                        checked={cardForm.setAsDefault}
                        onChange={(e) => setCardForm(prev => ({ 
                          ...prev, 
                          setAsDefault: e.target.checked 
                        }))}
                        className="rounded border-gray-300 text-ocean-600 focus:ring-ocean-500"
                      />
                      <span className="ml-2 text-sm text-gray-700">
                        Set as default payment method
                      </span>
                    </label>
                  )}
                </div>
              )}
            </div>
          )}

          {/* Submit Button */}
          <Button
            type="submit"
            disabled={isLoading}
            className="w-full mt-6"
            variant="primary"
          >
            {isLoading ? (
              <>
                <LoadingSpinner className="w-4 h-4 mr-2" />
                Processing Payment...
              </>
            ) : (
              `Pay ${amount ? formatAmount(amount, currency) : ''}`
            )}
          </Button>
        </form>

        {/* Security Notice */}
        <div className="mt-4 p-3 bg-gray-50 rounded-lg">
          <p className="text-xs text-gray-600 flex items-center">
            <svg className="w-4 h-4 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M2.166 4.999A11.954 11.954 0 0010 1.944 11.954 11.954 0 0017.834 5c.11.65.166 1.32.166 2.001 0 5.225-3.34 9.67-8 11.317C5.34 16.67 2 12.225 2 7c0-.682.057-1.35.166-2.001zm11.541 3.708a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
            </svg>
            Your payment information is secure and encrypted. We never store your card details.
          </p>
        </div>
      </div>
    </Card>
  );
};

export default PaymentMethodForm;