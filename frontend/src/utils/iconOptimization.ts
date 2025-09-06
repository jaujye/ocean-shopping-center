/**
 * Icon Optimization Utilities
 * 
 * Provides tools and utilities for optimizing icon usage and bundle size
 * in the Ocean Shopping Center application.
 */

import { IconName } from '../components/ui/Icon';

// Track icon usage for bundle optimization
export class IconUsageTracker {
  private static instance: IconUsageTracker;
  private usedIcons: Set<IconName> = new Set();
  private usageStats: Map<IconName, number> = new Map();

  static getInstance(): IconUsageTracker {
    if (!IconUsageTracker.instance) {
      IconUsageTracker.instance = new IconUsageTracker();
    }
    return IconUsageTracker.instance;
  }

  // Track icon usage
  trackIcon(iconName: IconName): void {
    this.usedIcons.add(iconName);
    const currentCount = this.usageStats.get(iconName) || 0;
    this.usageStats.set(iconName, currentCount + 1);
  }

  // Get usage statistics
  getUsageStats(): Array<{ icon: IconName; count: number }> {
    return Array.from(this.usageStats.entries())
      .map(([icon, count]) => ({ icon, count }))
      .sort((a, b) => b.count - a.count);
  }

  // Get list of used icons
  getUsedIcons(): IconName[] {
    return Array.from(this.usedIcons).sort();
  }

  // Get unused icons (for potential tree-shaking)
  getUnusedIcons(allIcons: IconName[]): IconName[] {
    return allIcons.filter(icon => !this.usedIcons.has(icon));
  }

  // Reset statistics (useful for testing)
  reset(): void {
    this.usedIcons.clear();
    this.usageStats.clear();
  }

  // Export statistics as JSON
  exportStats(): string {
    return JSON.stringify({
      totalUsedIcons: this.usedIcons.size,
      mostUsedIcons: this.getUsageStats().slice(0, 10),
      allUsedIcons: this.getUsedIcons(),
      timestamp: new Date().toISOString(),
    }, null, 2);
  }
}

// Type definition for icon optimization utilities
interface IconOptimizationType {
  estimateBundleImpact(iconCount: number): {
    estimatedKB: number;
    description: string;
  };
  analyzeUsagePatterns(stats: Array<{ icon: IconName; count: number }>): {
    criticalIcons: IconName[];
    frequentIcons: IconName[];
    rareIcons: IconName[];
    recommendations: string[];
  };
  generateOptimizationReport(tracker: IconUsageTracker): {
    summary: {
      totalUsedIcons: number;
      estimatedBundleImpact: ReturnType<IconOptimizationType['estimateBundleImpact']>;
    };
    usageAnalysis: ReturnType<IconOptimizationType['analyzeUsagePatterns']>;
    recommendations: string[];
    timestamp: string;
  };
}

// Icon bundle optimization helpers
export const IconOptimization: IconOptimizationType = {
  // Get estimated bundle size impact of icons
  estimateBundleImpact(iconCount: number): {
    estimatedKB: number;
    description: string;
  } {
    // Each Heroicon is roughly 1-2KB when minified
    const avgSizePerIcon = 1.5;
    const estimatedKB = iconCount * avgSizePerIcon;
    
    let description = '';
    if (estimatedKB < 10) {
      description = 'Minimal impact on bundle size';
    } else if (estimatedKB < 50) {
      description = 'Small impact on bundle size';
    } else if (estimatedKB < 100) {
      description = 'Moderate impact on bundle size';
    } else {
      description = 'Significant impact - consider lazy loading';
    }

    return { estimatedKB, description };
  },

  // Analyze icon usage patterns
  analyzeUsagePatterns(stats: Array<{ icon: IconName; count: number }>): {
    criticalIcons: IconName[];
    frequentIcons: IconName[];
    rareIcons: IconName[];
    recommendations: string[];
  } {
    const totalUsage = stats.reduce((sum, { count }) => sum + count, 0);
    const criticalThreshold = totalUsage * 0.1; // 10% of total usage
    const frequentThreshold = totalUsage * 0.02; // 2% of total usage

    const criticalIcons: IconName[] = [];
    const frequentIcons: IconName[] = [];
    const rareIcons: IconName[] = [];

    stats.forEach(({ icon, count }) => {
      if (count >= criticalThreshold) {
        criticalIcons.push(icon);
      } else if (count >= frequentThreshold) {
        frequentIcons.push(icon);
      } else {
        rareIcons.push(icon);
      }
    });

    const recommendations: string[] = [];
    
    if (criticalIcons.length > 0) {
      recommendations.push(
        `Critical icons (${criticalIcons.length}): Consider preloading these high-usage icons`
      );
    }
    
    if (rareIcons.length > stats.length * 0.5) {
      recommendations.push(
        `Many rare icons (${rareIcons.length}): Consider lazy loading for better performance`
      );
    }
    
    if (stats.length > 50) {
      recommendations.push(
        'Large icon set detected: Implement dynamic imports for unused icons'
      );
    }

    return {
      criticalIcons,
      frequentIcons,
      rareIcons,
      recommendations,
    };
  },

  // Generate optimization report
  generateOptimizationReport(tracker: IconUsageTracker): {
    summary: {
      totalUsedIcons: number;
      estimatedBundleImpact: ReturnType<typeof IconOptimization.estimateBundleImpact>;
    };
    usageAnalysis: ReturnType<typeof IconOptimization.analyzeUsagePatterns>;
    recommendations: string[];
    timestamp: string;
  } {
    const stats = tracker.getUsageStats();
    const usedIconCount = tracker.getUsedIcons().length;
    const estimatedBundleImpact = this.estimateBundleImpact(usedIconCount);
    const usageAnalysis = this.analyzeUsagePatterns(stats);

    const recommendations = [
      ...usageAnalysis.recommendations,
      'Consider using semantic icon names for better maintainability',
      'Implement icon sprites for frequently used icons',
      'Monitor bundle size with webpack-bundle-analyzer',
    ];

    return {
      summary: {
        totalUsedIcons: usedIconCount,
        estimatedBundleImpact,
      },
      usageAnalysis,
      recommendations,
      timestamp: new Date().toISOString(),
    };
  },
};

// Performance monitoring utilities
export const IconPerformance = {
  // Measure icon rendering performance
  measureIconRenderTime<T>(
    iconName: IconName,
    renderFn: () => T,
    iterations: number = 1000
  ): {
    iconName: IconName;
    avgRenderTime: number;
    iterations: number;
    result: T;
  } {
    const start = performance.now();
    let result: T;
    
    for (let i = 0; i < iterations; i++) {
      result = renderFn();
    }
    
    const end = performance.now();
    const avgRenderTime = (end - start) / iterations;

    return {
      iconName,
      avgRenderTime,
      iterations,
      result: result!,
    };
  },

  // Create performance benchmark
  createBenchmark(iconNames: IconName[], renderFn: (icon: IconName) => any): {
    results: Array<{
      iconName: IconName;
      renderTime: number;
    }>;
    summary: {
      fastest: { icon: IconName; time: number };
      slowest: { icon: IconName; time: number };
      average: number;
    };
  } {
    const results = iconNames.map(iconName => {
      const benchmark = this.measureIconRenderTime(iconName, () => renderFn(iconName), 100);
      return {
        iconName,
        renderTime: benchmark.avgRenderTime,
      };
    });

    const times = results.map(r => r.renderTime);
    const fastest = results.reduce((min, curr) => 
      curr.renderTime < min.renderTime ? curr : min
    );
    const slowest = results.reduce((max, curr) => 
      curr.renderTime > max.renderTime ? curr : max
    );
    const average = times.reduce((sum, time) => sum + time, 0) / times.length;

    return {
      results,
      summary: {
        fastest: { icon: fastest.iconName, time: fastest.renderTime },
        slowest: { icon: slowest.iconName, time: slowest.renderTime },
        average,
      },
    };
  },
};

// Development utilities
export const IconDevelopment = {
  // Log icon usage in development
  logIconUsage(iconName: IconName, context?: string): void {
    if (process.env.NODE_ENV === 'development') {
      const tracker = IconUsageTracker.getInstance();
      tracker.trackIcon(iconName);
      
      if (context) {
        console.log(`🎨 Icon usage: ${iconName} in ${context}`);
      }
    }
  },

  // Validate icon usage
  validateIconName(iconName: string, availableIcons: IconName[]): boolean {
    return availableIcons.includes(iconName as IconName);
  },

  // Generate icon usage report for development
  generateDevReport(): void {
    if (process.env.NODE_ENV === 'development') {
      const tracker = IconUsageTracker.getInstance();
      const report = IconOptimization.generateOptimizationReport(tracker);
      
      console.group('📊 Icon Usage Report');
      console.log('Total used icons:', report.summary.totalUsedIcons);
      console.log('Estimated bundle impact:', report.summary.estimatedBundleImpact);
      console.log('Critical icons:', report.usageAnalysis.criticalIcons);
      console.log('Recommendations:', report.recommendations);
      console.groupEnd();
    }
  },
};

export default {
  IconUsageTracker,
  IconOptimization,
  IconPerformance,
  IconDevelopment,
};