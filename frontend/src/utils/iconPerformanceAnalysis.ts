/**
 * Icon Performance Analysis
 * 
 * Comprehensive performance analysis and reporting for the unified icon system.
 * Measures bundle size impact, rendering performance, and provides optimization insights.
 */

import { IconName } from '../components/ui/Icon';

export interface PerformanceMetrics {
  bundleSize: {
    beforeOptimization: number; // KB
    afterOptimization: number; // KB
    reduction: number; // KB
    reductionPercentage: number;
  };
  renderingPerformance: {
    avgRenderTime: number; // ms
    minRenderTime: number; // ms
    maxRenderTime: number; // ms
    totalIconsRendered: number;
  };
  memoryUsage: {
    beforeOptimization: number; // MB
    afterOptimization: number; // MB
    reduction: number; // MB
  };
  loadingTime: {
    initialLoad: number; // ms
    subsequentLoads: number; // ms
  };
  accessibility: {
    ariaCompliance: number; // percentage
    keyboardNavigationScore: number; // percentage
    screenReaderCompatibility: number; // percentage
  };
}

export class IconPerformanceAnalyzer {
  private metrics: Partial<PerformanceMetrics> = {};
  private startTime: number = 0;
  private iconRenderTimes: Map<IconName, number[]> = new Map();
  
  // Start performance measurement
  startMeasurement(): void {
    this.startTime = performance.now();
    
    // Measure initial memory usage
    if ('memory' in performance) {
      const memoryInfo = (performance as any).memory;
      this.metrics.memoryUsage = {
        beforeOptimization: memoryInfo.usedJSHeapSize / (1024 * 1024), // Convert to MB
        afterOptimization: 0,
        reduction: 0,
      };
    }
  }

  // Record icon render time
  recordIconRender(iconName: IconName, renderTime: number): void {
    if (!this.iconRenderTimes.has(iconName)) {
      this.iconRenderTimes.set(iconName, []);
    }
    this.iconRenderTimes.get(iconName)!.push(renderTime);
  }

  // Measure bundle size impact
  measureBundleSize(beforeKB: number, afterKB: number): void {
    const reduction = beforeKB - afterKB;
    const reductionPercentage = (reduction / beforeKB) * 100;
    
    this.metrics.bundleSize = {
      beforeOptimization: beforeKB,
      afterOptimization: afterKB,
      reduction,
      reductionPercentage,
    };
  }

  // Analyze rendering performance
  analyzeRenderingPerformance(): void {
    const allRenderTimes: number[] = [];
    let totalIconsRendered = 0;
    
    this.iconRenderTimes.forEach((times) => {
      allRenderTimes.push(...times);
      totalIconsRendered += times.length;
    });

    if (allRenderTimes.length > 0) {
      const avgRenderTime = allRenderTimes.reduce((sum, time) => sum + time, 0) / allRenderTimes.length;
      const minRenderTime = Math.min(...allRenderTimes);
      const maxRenderTime = Math.max(...allRenderTimes);

      this.metrics.renderingPerformance = {
        avgRenderTime,
        minRenderTime,
        maxRenderTime,
        totalIconsRendered,
      };
    }
  }

  // Measure loading time
  measureLoadingTime(initialLoad: number, subsequentLoads: number): void {
    this.metrics.loadingTime = {
      initialLoad,
      subsequentLoads,
    };
  }

  // Assess accessibility compliance
  assessAccessibility(
    ariaCompliance: number,
    keyboardNavigationScore: number,
    screenReaderCompatibility: number
  ): void {
    this.metrics.accessibility = {
      ariaCompliance,
      keyboardNavigationScore,
      screenReaderCompatibility,
    };
  }

  // Complete measurement and finalize metrics
  completeMeasurement(): PerformanceMetrics {
    this.analyzeRenderingPerformance();
    
    // Final memory measurement
    if ('memory' in performance && this.metrics.memoryUsage) {
      const memoryInfo = (performance as any).memory;
      const afterOptimization = memoryInfo.usedJSHeapSize / (1024 * 1024);
      this.metrics.memoryUsage.afterOptimization = afterOptimization;
      this.metrics.memoryUsage.reduction = 
        this.metrics.memoryUsage.beforeOptimization - afterOptimization;
    }

    return this.metrics as PerformanceMetrics;
  }

  // Generate comprehensive performance report
  generateReport(): {
    summary: string;
    metrics: PerformanceMetrics;
    recommendations: string[];
    score: number; // Overall performance score out of 100
  } {
    const metrics = this.completeMeasurement();
    
    // Calculate performance score
    let score = 100;
    
    // Bundle size impact (20% of score)
    if (metrics.bundleSize) {
      if (metrics.bundleSize.reductionPercentage > 20) score += 20;
      else if (metrics.bundleSize.reductionPercentage > 10) score += 15;
      else if (metrics.bundleSize.reductionPercentage > 5) score += 10;
      else score -= 10;
    }
    
    // Rendering performance (30% of score)
    if (metrics.renderingPerformance) {
      if (metrics.renderingPerformance.avgRenderTime < 1) score += 30;
      else if (metrics.renderingPerformance.avgRenderTime < 2) score += 20;
      else if (metrics.renderingPerformance.avgRenderTime < 5) score += 10;
      else score -= 15;
    }
    
    // Accessibility (30% of score)
    if (metrics.accessibility) {
      const avgAccessibilityScore = (
        metrics.accessibility.ariaCompliance +
        metrics.accessibility.keyboardNavigationScore +
        metrics.accessibility.screenReaderCompatibility
      ) / 3;
      
      if (avgAccessibilityScore >= 95) score += 30;
      else if (avgAccessibilityScore >= 90) score += 25;
      else if (avgAccessibilityScore >= 80) score += 15;
      else score -= 20;
    }
    
    // Loading time (20% of score)
    if (metrics.loadingTime) {
      if (metrics.loadingTime.initialLoad < 100) score += 20;
      else if (metrics.loadingTime.initialLoad < 200) score += 15;
      else if (metrics.loadingTime.initialLoad < 500) score += 10;
      else score -= 10;
    }
    
    score = Math.max(0, Math.min(100, score));

    // Generate recommendations
    const recommendations: string[] = [];
    
    if (metrics.bundleSize && metrics.bundleSize.reductionPercentage < 10) {
      recommendations.push('Consider implementing dynamic imports for rarely used icons');
      recommendations.push('Use icon sprites for frequently used icons');
    }
    
    if (metrics.renderingPerformance && metrics.renderingPerformance.avgRenderTime > 2) {
      recommendations.push('Implement React.memo for icon components');
      recommendations.push('Consider icon caching strategies');
    }
    
    if (metrics.accessibility && metrics.accessibility.ariaCompliance < 90) {
      recommendations.push('Improve ARIA label coverage for interactive icons');
      recommendations.push('Ensure all icons have proper semantic meaning');
    }
    
    if (metrics.loadingTime && metrics.loadingTime.initialLoad > 200) {
      recommendations.push('Implement progressive loading for icon sets');
      recommendations.push('Consider reducing the number of initially loaded icons');
    }

    // Generate summary
    const summary = this.generateSummary(metrics, score);

    return {
      summary,
      metrics,
      recommendations,
      score,
    };
  }

  private generateSummary(metrics: PerformanceMetrics, score: number): string {
    const parts: string[] = [];
    
    parts.push(`Overall Performance Score: ${score}/100`);
    
    if (metrics.bundleSize) {
      parts.push(
        `Bundle Size: Reduced by ${metrics.bundleSize.reduction.toFixed(1)}KB (${metrics.bundleSize.reductionPercentage.toFixed(1)}%)`
      );
    }
    
    if (metrics.renderingPerformance) {
      parts.push(
        `Rendering: Average ${metrics.renderingPerformance.avgRenderTime.toFixed(2)}ms per icon`
      );
    }
    
    if (metrics.accessibility) {
      const avgAccessibility = (
        metrics.accessibility.ariaCompliance +
        metrics.accessibility.keyboardNavigationScore +
        metrics.accessibility.screenReaderCompatibility
      ) / 3;
      parts.push(`Accessibility: ${avgAccessibility.toFixed(1)}% compliance`);
    }
    
    if (metrics.loadingTime) {
      parts.push(`Loading: ${metrics.loadingTime.initialLoad}ms initial load`);
    }

    return parts.join('\n');
  }

  // Export metrics as JSON
  exportMetrics(): string {
    const report = this.generateReport();
    return JSON.stringify({
      ...report,
      timestamp: new Date().toISOString(),
      environment: {
        userAgent: navigator.userAgent,
        viewport: {
          width: window.innerWidth,
          height: window.innerHeight,
        },
      },
    }, null, 2);
  }
}

// Benchmark specific icon operations
export class IconBenchmark {
  static async benchmarkIconRender(
    iconName: IconName,
    renderFunction: () => any,
    iterations: number = 1000
  ): Promise<{
    iconName: IconName;
    iterations: number;
    totalTime: number;
    averageTime: number;
    minTime: number;
    maxTime: number;
  }> {
    const times: number[] = [];
    
    for (let i = 0; i < iterations; i++) {
      const start = performance.now();
      await renderFunction();
      const end = performance.now();
      times.push(end - start);
    }
    
    const totalTime = times.reduce((sum, time) => sum + time, 0);
    const averageTime = totalTime / iterations;
    const minTime = Math.min(...times);
    const maxTime = Math.max(...times);
    
    return {
      iconName,
      iterations,
      totalTime,
      averageTime,
      minTime,
      maxTime,
    };
  }

  static async benchmarkIconSet(
    iconNames: IconName[],
    renderFunction: (iconName: IconName) => any
  ): Promise<{
    results: Array<{
      iconName: IconName;
      averageTime: number;
    }>;
    totalTime: number;
    overallAverage: number;
  }> {
    const results: Array<{ iconName: IconName; averageTime: number }> = [];
    let totalTime = 0;
    
    for (const iconName of iconNames) {
      const benchmark = await this.benchmarkIconRender(
        iconName,
        () => renderFunction(iconName),
        100
      );
      
      results.push({
        iconName,
        averageTime: benchmark.averageTime,
      });
      
      totalTime += benchmark.totalTime;
    }
    
    return {
      results,
      totalTime,
      overallAverage: totalTime / (iconNames.length * 100),
    };
  }
}

// Performance comparison utilities
export const PerformanceComparison = {
  // Compare before and after optimization
  compareOptimization(
    beforeMetrics: PerformanceMetrics,
    afterMetrics: PerformanceMetrics
  ): {
    improvements: {
      bundleSize: number; // percentage improvement
      renderingSpeed: number;
      memoryUsage: number;
      accessibility: number;
    };
    summary: string;
  } {
    const improvements = {
      bundleSize: 0,
      renderingSpeed: 0,
      memoryUsage: 0,
      accessibility: 0,
    };
    
    // Bundle size improvement
    if (beforeMetrics.bundleSize && afterMetrics.bundleSize) {
      improvements.bundleSize = 
        ((beforeMetrics.bundleSize.beforeOptimization - afterMetrics.bundleSize.afterOptimization) / 
         beforeMetrics.bundleSize.beforeOptimization) * 100;
    }
    
    // Rendering speed improvement
    if (beforeMetrics.renderingPerformance && afterMetrics.renderingPerformance) {
      improvements.renderingSpeed = 
        ((beforeMetrics.renderingPerformance.avgRenderTime - afterMetrics.renderingPerformance.avgRenderTime) / 
         beforeMetrics.renderingPerformance.avgRenderTime) * 100;
    }
    
    // Memory usage improvement
    if (beforeMetrics.memoryUsage && afterMetrics.memoryUsage) {
      improvements.memoryUsage = 
        ((beforeMetrics.memoryUsage.beforeOptimization - afterMetrics.memoryUsage.afterOptimization) / 
         beforeMetrics.memoryUsage.beforeOptimization) * 100;
    }
    
    // Accessibility improvement
    if (beforeMetrics.accessibility && afterMetrics.accessibility) {
      const beforeAvg = (beforeMetrics.accessibility.ariaCompliance + 
                        beforeMetrics.accessibility.keyboardNavigationScore + 
                        beforeMetrics.accessibility.screenReaderCompatibility) / 3;
      const afterAvg = (afterMetrics.accessibility.ariaCompliance + 
                       afterMetrics.accessibility.keyboardNavigationScore + 
                       afterMetrics.accessibility.screenReaderCompatibility) / 3;
      improvements.accessibility = afterAvg - beforeAvg;
    }
    
    const summary = `
Performance Improvements:
- Bundle Size: ${improvements.bundleSize.toFixed(1)}% reduction
- Rendering Speed: ${improvements.renderingSpeed.toFixed(1)}% faster
- Memory Usage: ${improvements.memoryUsage.toFixed(1)}% reduction  
- Accessibility: ${improvements.accessibility.toFixed(1)}% improvement
    `.trim();
    
    return { improvements, summary };
  },
};

export default IconPerformanceAnalyzer;