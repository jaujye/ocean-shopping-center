import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../../contexts/CartContext';
import { useAuth } from '../../contexts/AuthContext';
import CheckoutSteps from './CheckoutSteps';
import ShippingAddressForm from './ShippingAddressForm';
import BillingAddressForm from './BillingAddressForm';
import PaymentMethodForm from '../payment/PaymentMethodForm';
import OrderSummary from './OrderSummary';
import orderService, { CheckoutRequest, Address, CheckoutResponse } from '../../services/orderService';

interface CheckoutPageProps {
  className?: string;
}

type CheckoutStep = 'shipping' | 'billing' | 'payment' | 'review';

const CheckoutPage: React.FC<CheckoutPageProps> = ({ className = '' }) => {
  const navigate = useNavigate();
  const { cart, clearCart } = useCart();
  const { user } = useAuth();

  const [currentStep, setCurrentStep] = useState<CheckoutStep>('shipping');
  const [isProcessing, setIsProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<CheckoutResponse | null>(null);

  // Form data state
  const [shippingAddress, setShippingAddress] = useState<Address>({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'US'
  });

  const [billingAddress, setBillingAddress] = useState<Address>({
    firstName: user?.firstName || '',
    lastName: user?.lastName || '',
    addressLine1: '',
    addressLine2: '',
    city: '',
    state: '',
    postalCode: '',
    country: 'US'
  });

  const [paymentMethodId, setPaymentMethodId] = useState<string>('');
  const [sameAsShipping, setSameAsShipping] = useState(false);
  const [customerEmail, setCustomerEmail] = useState(user?.email || '');
  const [customerPhone, setCustomerPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [couponCode, setCouponCode] = useState('');

  // Check if cart is empty and redirect
  useEffect(() => {
    if (!cart || cart.items.length === 0) {
      navigate('/cart');
    }
  }, [cart, navigate]);

  // Auto-fill billing address when same as shipping is selected
  useEffect(() => {
    if (sameAsShipping) {
      setBillingAddress({ ...shippingAddress });
    }
  }, [sameAsShipping, shippingAddress]);

  const steps: { key: CheckoutStep; title: string; description: string }[] = [
    { key: 'shipping', title: 'Shipping', description: 'Delivery address' },
    { key: 'billing', title: 'Billing', description: 'Billing information' },
    { key: 'payment', title: 'Payment', description: 'Payment method' },
    { key: 'review', title: 'Review', description: 'Confirm order' }
  ];

  const currentStepIndex = steps.findIndex(step => step.key === currentStep);

  const handleNextStep = () => {
    const nextIndex = currentStepIndex + 1;
    if (nextIndex < steps.length) {
      setCurrentStep(steps[nextIndex].key);
    }
  };

  const handlePrevStep = () => {
    const prevIndex = currentStepIndex - 1;
    if (prevIndex >= 0) {
      setCurrentStep(steps[prevIndex].key);
    }
  };

  const validateCurrentStep = (): boolean => {
    setError(null);
    
    switch (currentStep) {
      case 'shipping':
        return validateAddress(shippingAddress, 'Shipping');
      case 'billing':
        return validateAddress(billingAddress, 'Billing');
      case 'payment':
        if (!paymentMethodId) {
          setError('Please select a payment method');
          return false;
        }
        if (!customerEmail) {
          setError('Please provide your email address');
          return false;
        }
        return true;
      case 'review':
        return true;
      default:
        return false;
    }
  };

  const validateAddress = (address: Address, type: string): boolean => {
    if (!address.firstName) {
      setError(`${type} first name is required`);
      return false;
    }
    if (!address.lastName) {
      setError(`${type} last name is required`);
      return false;
    }
    if (!address.addressLine1) {
      setError(`${type} address is required`);
      return false;
    }
    if (!address.city) {
      setError(`${type} city is required`);
      return false;
    }
    if (!address.postalCode) {
      setError(`${type} postal code is required`);
      return false;
    }
    if (!address.country) {
      setError(`${type} country is required`);
      return false;
    }
    return true;
  };

  const handleNext = () => {
    if (validateCurrentStep()) {
      handleNextStep();
    }
  };

  const handleSubmitOrder = async () => {
    if (!validateCurrentStep()) {
      return;
    }

    setIsProcessing(true);
    setError(null);

    try {
      const checkoutData: CheckoutRequest = {
        customerEmail,
        customerPhone: customerPhone || undefined,
        shippingAddress,
        billingAddress: sameAsShipping ? shippingAddress : billingAddress,
        paymentMethodId,
        couponCode: couponCode || undefined,
        notes: notes || undefined,
        sameAsShipping
      };

      const response = await orderService.processCheckout(checkoutData);

      if (response.success) {
        setSuccess(response);
        clearCart(); // Clear cart on successful order
        // Navigate to order confirmation page
        setTimeout(() => {
          navigate(`/order-confirmation/${response.orderNumber}`);
        }, 2000);
      } else {
        setError(response.message || 'Checkout failed');
      }
    } catch (error: any) {
      console.error('Checkout error:', error);
      setError(error.response?.data?.message || 'An unexpected error occurred');
    } finally {
      setIsProcessing(false);
    }
  };

  const applyCoupon = async () => {
    if (!couponCode.trim()) return;
    
    try {
      await orderService.applyCoupon(couponCode);
      // Refresh cart or checkout summary
    } catch (error: any) {
      setError(error.response?.data?.message || 'Invalid coupon code');
    }
  };

  if (!cart || cart.items.length === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-xl font-semibold mb-4">Your cart is empty</h2>
          <button
            onClick={() => navigate('/products')}
            className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700"
          >
            Continue Shopping
          </button>
        </div>
      </div>
    );
  }

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="max-w-md w-full bg-white p-8 rounded-lg shadow-md text-center">
          <div className="mb-6">
            <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Order Placed Successfully!</h2>
            <p className="text-gray-600">
              Order #{success.orderNumber}
            </p>
          </div>
          
          <div className="bg-gray-50 p-4 rounded-lg mb-6">
            <p className="text-sm text-gray-600 mb-2">Total Amount</p>
            <p className="text-2xl font-bold text-gray-900">
              ${success.totalAmount?.toFixed(2)} {success.currency}
            </p>
          </div>

          <p className="text-sm text-gray-600 mb-6">
            A confirmation email has been sent to {success.customerEmail}
          </p>

          <button
            onClick={() => navigate('/orders')}
            className="w-full bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700"
          >
            View Order History
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className={`min-h-screen bg-gray-50 py-8 ${className}`}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Checkout</h1>
          <p className="mt-2 text-gray-600">Complete your order</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Checkout Form */}
          <div className="lg:col-span-2">
            <div className="bg-white rounded-lg shadow-sm">
              {/* Progress Steps */}
              <div className="border-b border-gray-200 px-6 py-4">
                <CheckoutSteps
                  steps={steps}
                  currentStep={currentStep}
                  onStepClick={(stepKey: string) => setCurrentStep(stepKey as CheckoutStep)}
                />
              </div>

              {/* Form Content */}
              <div className="p-6">
                {error && (
                  <div className="mb-6 bg-red-50 border border-red-200 rounded-md p-4">
                    <div className="flex">
                      <div className="flex-shrink-0">
                        <svg className="h-5 w-5 text-red-400" viewBox="0 0 20 20" fill="currentColor">
                          <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
                        </svg>
                      </div>
                      <div className="ml-3">
                        <p className="text-sm text-red-800">{error}</p>
                      </div>
                    </div>
                  </div>
                )}

                {/* Step Content */}
                {currentStep === 'shipping' && (
                  <ShippingAddressForm
                    address={shippingAddress}
                    onChange={setShippingAddress}
                  />
                )}

                {currentStep === 'billing' && (
                  <BillingAddressForm
                    address={billingAddress}
                    onChange={setBillingAddress}
                    sameAsShipping={sameAsShipping}
                    onSameAsShippingChange={setSameAsShipping}
                  />
                )}

                {currentStep === 'payment' && (
                  <div className="space-y-6">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Email Address *
                        </label>
                        <input
                          type="email"
                          value={customerEmail}
                          onChange={(e) => setCustomerEmail(e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                          required
                        />
                      </div>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Phone Number
                        </label>
                        <input
                          type="tel"
                          value={customerPhone}
                          onChange={(e) => setCustomerPhone(e.target.value)}
                          className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                      </div>
                    </div>

                    <PaymentMethodForm
                      onPaymentSuccess={(payment) => {
                        console.log('Payment successful:', payment);
                        setPaymentMethodId(payment.id || payment.paymentMethodId || 'default');
                      }}
                      onPaymentError={(error) => {
                        console.error('Payment error:', error);
                        setError(error);
                      }}
                    />

                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Order Notes (Optional)
                      </label>
                      <textarea
                        value={notes}
                        onChange={(e) => setNotes(e.target.value)}
                        rows={3}
                        className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                        placeholder="Special instructions for your order..."
                      />
                    </div>
                  </div>
                )}

                {currentStep === 'review' && (
                  <div className="space-y-6">
                    <h3 className="text-lg font-medium text-gray-900">Review Your Order</h3>
                    
                    {/* Order Summary */}
                    <div className="bg-gray-50 p-4 rounded-lg">
                      <h4 className="font-medium text-gray-900 mb-3">Order Details</h4>
                      <dl className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <div>
                          <dt className="text-sm font-medium text-gray-500">Shipping Address</dt>
                          <dd className="mt-1 text-sm text-gray-900">
                            {shippingAddress.firstName} {shippingAddress.lastName}<br />
                            {shippingAddress.addressLine1}<br />
                            {shippingAddress.addressLine2 && <>{shippingAddress.addressLine2}<br /></>}
                            {shippingAddress.city}, {shippingAddress.state} {shippingAddress.postalCode}<br />
                            {shippingAddress.country}
                          </dd>
                        </div>
                        <div>
                          <dt className="text-sm font-medium text-gray-500">Billing Address</dt>
                          <dd className="mt-1 text-sm text-gray-900">
                            {sameAsShipping ? (
                              <span className="text-gray-500">Same as shipping</span>
                            ) : (
                              <>
                                {billingAddress.firstName} {billingAddress.lastName}<br />
                                {billingAddress.addressLine1}<br />
                                {billingAddress.addressLine2 && <>{billingAddress.addressLine2}<br /></>}
                                {billingAddress.city}, {billingAddress.state} {billingAddress.postalCode}<br />
                                {billingAddress.country}
                              </>
                            )}
                          </dd>
                        </div>
                        <div>
                          <dt className="text-sm font-medium text-gray-500">Email</dt>
                          <dd className="mt-1 text-sm text-gray-900">{customerEmail}</dd>
                        </div>
                        <div>
                          <dt className="text-sm font-medium text-gray-500">Phone</dt>
                          <dd className="mt-1 text-sm text-gray-900">{customerPhone || 'Not provided'}</dd>
                        </div>
                      </dl>
                    </div>

                    {notes && (
                      <div>
                        <h4 className="font-medium text-gray-900 mb-2">Order Notes</h4>
                        <p className="text-sm text-gray-600 bg-gray-50 p-3 rounded">{notes}</p>
                      </div>
                    )}
                  </div>
                )}

                {/* Navigation Buttons */}
                <div className="flex justify-between pt-6 border-t border-gray-200 mt-8">
                  <button
                    onClick={handlePrevStep}
                    disabled={currentStepIndex === 0}
                    className="px-6 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Previous
                  </button>

                  {currentStep === 'review' ? (
                    <button
                      onClick={handleSubmitOrder}
                      disabled={isProcessing}
                      className="px-8 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                    >
                      {isProcessing ? (
                        <>
                          <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                          </svg>
                          Processing...
                        </>
                      ) : (
                        'Place Order'
                      )}
                    </button>
                  ) : (
                    <button
                      onClick={handleNext}
                      className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                    >
                      Next
                    </button>
                  )}
                </div>
              </div>
            </div>
          </div>

          {/* Order Summary Sidebar */}
          <div className="lg:col-span-1">
            <OrderSummary
              cart={cart}
              couponCode={couponCode}
              onCouponCodeChange={setCouponCode}
              onApplyCoupon={applyCoupon}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default CheckoutPage;