import React, { ButtonHTMLAttributes, ReactNode } from 'react';
import { cn } from '../../utils/cn';
import LoadingSpinner from './LoadingSpinner';
import { IconName } from './Icon';
import ThemedIcon from './ThemedIcon';

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  loading?: boolean;
  loadingText?: string;
  leftIcon?: ReactNode | IconName;
  rightIcon?: ReactNode | IconName;
  fullWidth?: boolean;
  asChild?: boolean;
  children: ReactNode;
}

const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  loading = false,
  loadingText,
  leftIcon,
  rightIcon,
  fullWidth = false,
  asChild = false,
  children,
  className,
  disabled,
  ...props
}) => {
  const baseClasses = 'inline-flex items-center justify-center font-medium transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed';

  const variantClasses = {
    primary: 'bg-ocean-500 hover:bg-ocean-600 text-white shadow-sm focus:ring-ocean-500 active:bg-ocean-700',
    secondary: 'bg-teal-500 hover:bg-teal-600 text-white shadow-sm focus:ring-teal-500 active:bg-teal-700',
    outline: 'border-2 border-ocean-500 text-ocean-500 hover:bg-ocean-500 hover:text-white focus:ring-ocean-500 active:bg-ocean-600 active:text-white',
    ghost: 'text-ocean-600 hover:bg-ocean-50 focus:ring-ocean-500 active:bg-ocean-100',
    danger: 'bg-red-500 hover:bg-red-600 text-white shadow-sm focus:ring-red-500 active:bg-red-700',
  };

  const sizeClasses = {
    sm: 'px-3 py-1.5 text-sm rounded-md',
    md: 'px-4 py-2 text-sm rounded-lg',
    lg: 'px-6 py-3 text-base rounded-lg',
    xl: 'px-8 py-4 text-lg rounded-xl',
  };

  const isDisabled = disabled || loading;

  // Helper to render icons consistently
  const renderIcon = (icon: ReactNode | IconName | undefined, position: 'left' | 'right') => {
    if (!icon) return null;
    
    if (typeof icon === 'string') {
      // It's an IconName, render with ThemedIcon
      const iconSize = size === 'sm' ? 'sm' : size === 'lg' || size === 'xl' ? 'md' : 'sm';
      const iconColor = variant === 'primary' || variant === 'secondary' || variant === 'danger' ? 'white' : 'current';
      
      return (
        <ThemedIcon
          name={icon as IconName}
          size={iconSize}
          color={iconColor}
        />
      );
    }
    
    // It's a ReactNode, render as-is
    return icon;
  };

  const buttonClasses = cn(
    baseClasses,
    variantClasses[variant],
    sizeClasses[size],
    fullWidth && 'w-full',
    loading && 'relative',
    className
  );

  // If asChild is true, return children with button styles applied
  if (asChild && React.isValidElement(children)) {
    return React.cloneElement(children, {
      ...(children.props || {}),
      className: cn(buttonClasses, (children.props as any)?.className),
    } as any);
  }

  return (
    <button
      className={buttonClasses}
      disabled={isDisabled}
      {...props}
    >
      {loading && (
        <div className="absolute inset-0 flex items-center justify-center">
          <LoadingSpinner 
            size={size === 'sm' ? 'sm' : size === 'lg' || size === 'xl' ? 'md' : 'sm'} 
            color="white" 
          />
          {loadingText && (
            <span className="ml-2 text-sm">{loadingText}</span>
          )}
        </div>
      )}
      
      <div className={cn('flex items-center', loading && 'invisible')}>
        {leftIcon && (
          <span className="mr-2 flex-shrink-0">
            {renderIcon(leftIcon, 'left')}
          </span>
        )}
        
        <span>{children}</span>
        
        {rightIcon && (
          <span className="ml-2 flex-shrink-0">
            {renderIcon(rightIcon, 'right')}
          </span>
        )}
      </div>
    </button>
  );
};

export default Button;