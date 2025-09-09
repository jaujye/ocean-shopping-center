import React, { useState, useMemo } from 'react';
import { Product } from '../../types';
import { ProductFilters as FiltersType } from '../../types/product';
import { useInfiniteProducts } from '../../hooks/queries';
import ProductCard from './ProductCard';
import LoadingSpinner from '../ui/LoadingSpinner';
import Button from '../ui/Button';
import { cn } from '../../utils/cn';

// Icons
const GridIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" />
  </svg>
);

const ListIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 10h16M4 14h16M4 18h16" />
  </svg>
);

const RefreshIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
  </svg>
);

type ViewMode = 'grid' | 'list';

interface ProductGridProps {
  filters: FiltersType;
  onQuickView?: (product: Product) => void;
  onAddToWishlist?: (productId: string) => void;
  className?: string;
  initialViewMode?: ViewMode;
  showViewModeToggle?: boolean;
  showRefreshButton?: boolean;
  itemsPerPage?: number;
}

const ProductGrid: React.FC<ProductGridProps> = ({
  filters,
  onQuickView,
  onAddToWishlist,
  className,
  initialViewMode = 'grid',
  showViewModeToggle = true,
  showRefreshButton = true,
  itemsPerPage = 20,
}) => {
  const [viewMode, setViewMode] = useState<ViewMode>(initialViewMode);

  // Use React Query for data fetching
  const {
    data,
    error,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
    refetch,
    isRefetching,
  } = useInfiniteProducts({
    ...filters,
    size: itemsPerPage,
  });

  // Flatten the pages into a single array of products
  const products = useMemo(() => {
    return data?.pages.flatMap(page => page.data) ?? [];
  }, [data]);

  // Get total count from the first page
  const totalCount = data?.pages[0]?.pagination.total ?? 0;

  // Memoized grid layout classes
  const gridClasses = useMemo(() => {
    if (viewMode === 'list') {
      return 'space-y-4';
    }
    return 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4 lg:gap-6';
  }, [viewMode]);

  // Render loading state
  if (isLoading && products.length === 0) {
    return (
      <div className={cn("flex items-center justify-center py-12", className)}>
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  // Render error state
  if (isError && products.length === 0) {
    return (
      <div className={cn("text-center py-12", className)}>
        <div className="text-red-500 mb-4">
          <svg className="w-16 h-16 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <p className="text-lg font-semibold">Failed to load products</p>
          <p className="text-sm text-gray-600 mt-2">
            {error instanceof Error ? error.message : 'An unexpected error occurred'}
          </p>
        </div>
        <Button onClick={() => refetch()} variant="primary">
          Try Again
        </Button>
      </div>
    );
  }

  // Render empty state
  if (!isLoading && products.length === 0) {
    return (
      <div className={cn("text-center py-12", className)}>
        <div className="text-gray-400 mb-4">
          <svg className="w-16 h-16 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
          <p className="text-lg font-semibold text-gray-500">No products found</p>
          <p className="text-sm text-gray-400 mt-2">
            Try adjusting your filters or search criteria
          </p>
        </div>
        {showRefreshButton && (
          <Button onClick={() => refetch()} variant="outline">
            Refresh
          </Button>
        )}
      </div>
    );
  }

  return (
    <div className={cn("space-y-4", className)}>
      {/* Header with view mode toggle and results info */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div className="text-sm text-gray-600">
          Showing {products.length} of {totalCount} products
          {filters.search && (
            <span> for "{filters.search}"</span>
          )}
        </div>

        <div className="flex items-center space-x-2">
          {showRefreshButton && (
            <Button
              onClick={() => refetch()}
              variant="outline"
              size="sm"
              leftIcon={<RefreshIcon className={isRefetching ? "animate-spin" : ""} />}
              disabled={isRefetching}
            >
              Refresh
            </Button>
          )}

          {showViewModeToggle && (
            <div className="flex items-center border border-gray-300 rounded-lg overflow-hidden">
              <button
                onClick={() => setViewMode('grid')}
                className={cn(
                  "p-2 transition-colors duration-200",
                  viewMode === 'grid'
                    ? "bg-ocean-500 text-white"
                    : "bg-white text-gray-600 hover:bg-gray-50"
                )}
              >
                <GridIcon />
              </button>
              <button
                onClick={() => setViewMode('list')}
                className={cn(
                  "p-2 transition-colors duration-200",
                  viewMode === 'list'
                    ? "bg-ocean-500 text-white"
                    : "bg-white text-gray-600 hover:bg-gray-50"
                )}
              >
                <ListIcon />
              </button>
            </div>
          )}
        </div>
      </div>

      {/* Products Grid */}
      <div className={gridClasses}>
        {products.map((product, index) => (
          <ProductCard
            key={`${product.id}-${index}`}
            product={product}
            onQuickView={onQuickView}
            onAddToWishlist={onAddToWishlist}
            compact={viewMode === 'list'}
            className={viewMode === 'list' ? "flex-row" : undefined}
          />
        ))}
      </div>

      {/* Loading more indicator */}
      {isFetchingNextPage && (
        <div className="flex justify-center py-8">
          <LoadingSpinner size="md" />
        </div>
      )}

      {/* Load More Button */}
      {hasNextPage && !isFetchingNextPage && (
        <div className="flex justify-center py-8">
          <Button
            onClick={() => fetchNextPage()}
            variant="outline"
            size="lg"
            disabled={isFetchingNextPage}
          >
            Load More Products
          </Button>
        </div>
      )}

      {/* End of results indicator */}
      {!hasNextPage && products.length > 0 && (
        <div className="text-center text-sm text-gray-500 py-4">
          You've reached the end of the results
        </div>
      )}
    </div>
  );
};

export default ProductGrid;