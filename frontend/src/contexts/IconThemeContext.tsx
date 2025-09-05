import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { IconSize, IconColor, IconVariant } from '../components/ui/Icon';

// Theme detection types
export type Theme = 'light' | 'dark' | 'system';

// Icon theme configuration interface
interface IconThemeConfig {
  defaultSize: IconSize;
  defaultColor: IconColor;
  defaultVariant: IconVariant;
  theme: Theme;
  
  // Theme-specific overrides
  themeColors: {
    light: {
      primary: IconColor;
      secondary: IconColor;
      muted: IconColor;
      accent: IconColor;
    };
    dark: {
      primary: IconColor;
      secondary: IconColor;
      muted: IconColor;
      accent: IconColor;
    };
  };
}

// Default icon theme configuration
const defaultIconTheme: IconThemeConfig = {
  defaultSize: 'md',
  defaultColor: 'current',
  defaultVariant: 'outline',
  theme: 'system',
  themeColors: {
    light: {
      primary: 'ocean',
      secondary: 'gray',
      muted: 'slate',
      accent: 'teal',
    },
    dark: {
      primary: 'ocean',
      secondary: 'gray',
      muted: 'slate',
      accent: 'teal',
    },
  },
};

// Icon theme context
interface IconThemeContextValue extends IconThemeConfig {
  // Theme management
  setTheme: (theme: Theme) => void;
  resolvedTheme: 'light' | 'dark';
  
  // Configuration updates
  updateConfig: (config: Partial<IconThemeConfig>) => void;
  
  // Helper methods
  getThemeColor: (colorKey: keyof IconThemeConfig['themeColors']['light']) => IconColor;
  resetToDefaults: () => void;
}

// Create context
const IconThemeContext = createContext<IconThemeContextValue | undefined>(undefined);

// Hook to use icon theme
export const useIconTheme = (): IconThemeContextValue => {
  const context = useContext(IconThemeContext);
  if (!context) {
    throw new Error('useIconTheme must be used within an IconThemeProvider');
  }
  return context;
};

// Icon theme provider props
interface IconThemeProviderProps {
  children: ReactNode;
  initialConfig?: Partial<IconThemeConfig>;
}

/**
 * Icon Theme Provider
 * 
 * Provides centralized theme management for icons across the application.
 * Features:
 * - System theme detection (prefers-color-scheme)
 * - Manual theme switching
 * - Theme-aware color mapping
 * - Persistent theme preferences
 * - Configuration overrides
 */
export const IconThemeProvider: React.FC<IconThemeProviderProps> = ({
  children,
  initialConfig,
}) => {
  // Merge initial config with defaults
  const [config, setConfig] = useState<IconThemeConfig>({
    ...defaultIconTheme,
    ...initialConfig,
  });
  
  const [resolvedTheme, setResolvedTheme] = useState<'light' | 'dark'>('light');
  
  // Detect system theme preference
  const getSystemTheme = (): 'light' | 'dark' => {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    return 'light';
  };
  
  // Update resolved theme based on theme setting
  useEffect(() => {
    if (config.theme === 'system') {
      setResolvedTheme(getSystemTheme());
      
      // Listen for system theme changes
      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
      const handleChange = () => setResolvedTheme(getSystemTheme());
      
      if (mediaQuery.addListener) {
        mediaQuery.addListener(handleChange);
      } else {
        mediaQuery.addEventListener('change', handleChange);
      }
      
      return () => {
        if (mediaQuery.removeListener) {
          mediaQuery.removeListener(handleChange);
        } else {
          mediaQuery.removeEventListener('change', handleChange);
        }
      };
    } else {
      setResolvedTheme(config.theme);
    }
  }, [config.theme]);
  
  // Save theme preference to localStorage
  useEffect(() => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('icon-theme-preference', JSON.stringify({
        theme: config.theme,
        defaultSize: config.defaultSize,
        defaultVariant: config.defaultVariant,
      }));
    }
  }, [config.theme, config.defaultSize, config.defaultVariant]);
  
  // Load theme preference from localStorage on mount
  useEffect(() => {
    if (typeof window !== 'undefined') {
      try {
        const saved = localStorage.getItem('icon-theme-preference');
        if (saved) {
          const savedConfig = JSON.parse(saved);
          setConfig(prev => ({
            ...prev,
            ...savedConfig,
          }));
        }
      } catch (error) {
        console.warn('Failed to load icon theme preference from localStorage:', error);
      }
    }
  }, []);
  
  // Set theme
  const setTheme = (theme: Theme) => {
    setConfig(prev => ({ ...prev, theme }));
  };
  
  // Update configuration
  const updateConfig = (newConfig: Partial<IconThemeConfig>) => {
    setConfig(prev => ({ ...prev, ...newConfig }));
  };
  
  // Get theme-appropriate color
  const getThemeColor = (colorKey: keyof IconThemeConfig['themeColors']['light']): IconColor => {
    return config.themeColors[resolvedTheme][colorKey];
  };
  
  // Reset to defaults
  const resetToDefaults = () => {
    setConfig(defaultIconTheme);
    if (typeof window !== 'undefined') {
      localStorage.removeItem('icon-theme-preference');
    }
  };
  
  const contextValue: IconThemeContextValue = {
    ...config,
    resolvedTheme,
    setTheme,
    updateConfig,
    getThemeColor,
    resetToDefaults,
  };
  
  return (
    <IconThemeContext.Provider value={contextValue}>
      {children}
    </IconThemeContext.Provider>
  );
};

// Optional: Higher-order component for class-based components
export const withIconTheme = <P extends object>(
  Component: React.ComponentType<P>
): React.FC<P> => {
  return (props: P) => {
    const iconTheme = useIconTheme();
    return <Component {...props} iconTheme={iconTheme} />;
  };
};

export default IconThemeProvider;