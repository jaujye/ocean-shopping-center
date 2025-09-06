import React from 'react';
import Icon, { IconProps, IconName, IconSize, IconColor, IconVariant } from './Icon';
import { useIconTheme } from '../../contexts/IconThemeContext';

// Extended themed icon props
interface ThemedIconProps extends Omit<IconProps, 'name'> {
  name: IconName;
  
  // Theme-aware shortcuts
  semantic?: 'primary' | 'secondary' | 'muted' | 'accent' | 'success' | 'warning' | 'error' | 'info';
  
  // Enhanced responsive sizing
  responsiveSize?: {
    xs?: IconSize;
    sm?: IconSize;
    md?: IconSize;
    lg?: IconSize;
    xl?: IconSize;
  };
  
  // Auto-adjust for context
  autoVariant?: boolean;
  autoColor?: boolean;
  autoSize?: boolean;
}

// Semantic color mapping with theme support
const getSemanticColor = (semantic: NonNullable<ThemedIconProps['semantic']>, resolvedTheme: 'light' | 'dark'): IconColor => {
  const semanticMap: Record<string, { light: IconColor; dark: IconColor }> = {
    primary: { light: 'ocean', dark: 'ocean' },
    secondary: { light: 'gray', dark: 'gray' },
    muted: { light: 'slate', dark: 'slate' },
    accent: { light: 'teal', dark: 'teal' },
    success: { light: 'green', dark: 'green' },
    warning: { light: 'amber', dark: 'amber' },
    error: { light: 'red', dark: 'red' },
    info: { light: 'blue', dark: 'blue' },
  };
  
  return semanticMap[semantic]?.[resolvedTheme] || 'current';
};

// Context-aware size detection based on container
const getContextualSize = (): IconSize => {
  // This could be enhanced with actual DOM measurements
  // For now, return sensible defaults
  const screenWidth = typeof window !== 'undefined' ? window.innerWidth : 1024;
  
  if (screenWidth < 640) return 'sm';
  if (screenWidth < 1024) return 'md';
  return 'lg';
};

// Responsive size utilities
const getResponsiveSize = (responsiveSize?: ThemedIconProps['responsiveSize'], fallback: IconSize = 'md'): IconSize => {
  if (!responsiveSize) return fallback;
  
  // Simple responsive logic - could be enhanced with proper breakpoint detection
  const screenWidth = typeof window !== 'undefined' ? window.innerWidth : 1024;
  
  if (screenWidth >= 1280 && responsiveSize.xl) return responsiveSize.xl;
  if (screenWidth >= 1024 && responsiveSize.lg) return responsiveSize.lg;
  if (screenWidth >= 768 && responsiveSize.md) return responsiveSize.md;
  if (screenWidth >= 640 && responsiveSize.sm) return responsiveSize.sm;
  if (responsiveSize.xs) return responsiveSize.xs;
  
  return fallback;
};

/**
 * Themed Icon Component
 * 
 * An enhanced version of the base Icon component that integrates with the IconTheme context
 * to provide intelligent defaults and theme-aware styling.
 * 
 * Features:
 * - Automatic theme detection and color adaptation
 * - Semantic color mapping (success, warning, error, etc.)
 * - Responsive sizing with breakpoint-aware adjustments
 * - Context-aware variant selection
 * - Inherited theme preferences
 * - Performance optimized with memoization
 * 
 * @example
 * ```tsx
 * // Basic usage with theme defaults
 * <ThemedIcon name="search" />
 * 
 * // Semantic coloring
 * <ThemedIcon name="check-circle" semantic="success" />
 * <ThemedIcon name="exclamation-triangle" semantic="warning" />
 * 
 * // Responsive sizing
 * <ThemedIcon 
 *   name="menu" 
 *   responsiveSize={{ xs: 'sm', md: 'md', lg: 'lg' }}
 * />
 * 
 * // Auto-adjustment based on context
 * <ThemedIcon 
 *   name="edit" 
 *   autoSize 
 *   autoColor 
 *   autoVariant
 * />
 * ```
 */
const ThemedIcon: React.FC<ThemedIconProps> = React.memo(({
  name,
  semantic,
  responsiveSize,
  autoVariant = false,
  autoColor = false,
  autoSize = false,
  variant: explicitVariant,
  color: explicitColor,
  size: explicitSize,
  className,
  ...props
}) => {
  const {
    defaultSize,
    defaultColor,
    defaultVariant,
    resolvedTheme,
    getThemeColor,
  } = useIconTheme();
  
  // Resolve final props with intelligent defaults
  let finalVariant: IconVariant = explicitVariant || defaultVariant;
  let finalColor: IconColor = explicitColor || defaultColor;
  let finalSize: IconSize = explicitSize || defaultSize;
  
  // Apply semantic coloring
  if (semantic && !explicitColor) {
    finalColor = getSemanticColor(semantic, resolvedTheme);
  }
  
  // Apply auto-adjustments
  if (autoVariant) {
    // Context-aware variant selection
    // Could be enhanced based on parent components or user interactions
    finalVariant = resolvedTheme === 'dark' ? 'outline' : 'outline';
  }
  
  if (autoColor && !semantic && !explicitColor) {
    // Use theme-appropriate colors
    finalColor = getThemeColor('primary');
  }
  
  if (autoSize && !explicitSize) {
    finalSize = getContextualSize();
  }
  
  // Apply responsive sizing
  if (responsiveSize) {
    finalSize = getResponsiveSize(responsiveSize, finalSize);
  }
  
  // Build enhanced className with theme-aware classes
  const enhancedClassName = [
    className,
    // Add theme-specific classes for enhanced styling
    resolvedTheme === 'dark' ? 'icon-dark-theme' : 'icon-light-theme',
    // Add semantic classes for CSS customization
    semantic && `icon-semantic-${semantic}`,
  ].filter(Boolean).join(' ') || undefined;
  
  return (
    <Icon
      name={name}
      variant={finalVariant}
      color={finalColor}
      size={finalSize}
      className={enhancedClassName}
      {...props}
    />
  );
});

ThemedIcon.displayName = 'ThemedIcon';

export default ThemedIcon;

// Re-export types for convenience
export type { ThemedIconProps };

// Utility hook for theme-aware icon properties
export const useThemedIconProps = (
  semantic?: ThemedIconProps['semantic']
): Pick<IconProps, 'color' | 'variant'> => {
  const { defaultVariant, resolvedTheme, getThemeColor } = useIconTheme();
  
  return {
    variant: defaultVariant,
    color: semantic ? getSemanticColor(semantic, resolvedTheme) : getThemeColor('primary'),
  };
};