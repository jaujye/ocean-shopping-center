import React from 'react';

interface CheckoutStep {
  key: string;
  title: string;
  description: string;
}

interface CheckoutStepsProps {
  steps: CheckoutStep[];
  currentStep: string;
  onStepClick?: (stepKey: string) => void;
  className?: string;
}

const CheckoutSteps: React.FC<CheckoutStepsProps> = ({
  steps,
  currentStep,
  onStepClick,
  className = ''
}) => {
  const currentStepIndex = steps.findIndex(step => step.key === currentStep);

  const getStepStatus = (index: number) => {
    if (index < currentStepIndex) return 'completed';
    if (index === currentStepIndex) return 'current';
    return 'upcoming';
  };

  const handleStepClick = (stepKey: string, index: number) => {
    // Only allow clicking on completed steps or current step
    if (index <= currentStepIndex && onStepClick) {
      onStepClick(stepKey);
    }
  };

  return (
    <nav className={`${className}`} aria-label="Checkout Progress">
      <ol className="flex items-center">
        {steps.map((step, index) => {
          const status = getStepStatus(index);
          const isClickable = index <= currentStepIndex && onStepClick;
          
          return (
            <li key={step.key} className={`relative ${index !== steps.length - 1 ? 'pr-8 sm:pr-20' : ''}`}>
              {/* Connector Line */}
              {index !== steps.length - 1 && (
                <div className="absolute inset-0 flex items-center" aria-hidden="true">
                  <div className="h-0.5 w-full bg-gray-200" />
                </div>
              )}

              {/* Step Content */}
              <div
                className={`relative flex items-center justify-center w-8 h-8 rounded-full border-2 ${
                  status === 'completed'
                    ? 'bg-blue-600 border-blue-600'
                    : status === 'current'
                    ? 'border-blue-600 bg-white'
                    : 'border-gray-300 bg-white'
                } ${isClickable ? 'cursor-pointer hover:border-blue-500' : ''}`}
                onClick={() => handleStepClick(step.key, index)}
              >
                {status === 'completed' ? (
                  <svg
                    className="w-5 h-5 text-white"
                    xmlns="http://www.w3.org/2000/svg"
                    viewBox="0 0 20 20"
                    fill="currentColor"
                  >
                    <path
                      fillRule="evenodd"
                      d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z"
                      clipRule="evenodd"
                    />
                  </svg>
                ) : (
                  <span
                    className={`text-sm font-medium ${
                      status === 'current' ? 'text-blue-600' : 'text-gray-500'
                    }`}
                  >
                    {index + 1}
                  </span>
                )}
              </div>

              {/* Step Label */}
              <div className="absolute top-10 left-1/2 transform -translate-x-1/2 text-center min-w-max">
                <div
                  className={`text-sm font-medium ${
                    status === 'current'
                      ? 'text-blue-600'
                      : status === 'completed'
                      ? 'text-gray-900'
                      : 'text-gray-500'
                  }`}
                >
                  {step.title}
                </div>
                <div className="text-xs text-gray-500 mt-1">{step.description}</div>
              </div>
            </li>
          );
        })}
      </ol>
    </nav>
  );
};

export default CheckoutSteps;