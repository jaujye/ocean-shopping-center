import React, { useState, useEffect } from 'react';
import { ProductFilters as FiltersType, ProductCategory } from '../../types/product';
import Input from '../ui/Input';
import Button from '../ui/Button';
import Card from '../ui/Card';
import { cn } from '../../utils/cn';
import { productService } from '../../services/productService';

// Icons
const SearchIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
  </svg>
);

const FilterIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-5 h-5", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.707A1 1 0 013 7V4z" />
  </svg>
);

const XIcon = ({ className = "" }: { className?: string }) => (
  <svg className={cn("w-4 h-4", className)} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
  </svg>
);

interface ProductFiltersProps {
  filters: FiltersType;
  onFiltersChange: (filters: FiltersType) => void;
  onApplyFilters: () => void;
  onClearFilters: () => void;
  className?: string;
  showMobileToggle?: boolean;
  isLoading?: boolean;
}

const ProductFilters: React.FC<ProductFiltersProps> = ({
  filters,
  onFiltersChange,
  onApplyFilters,
  onClearFilters,
  className,
  showMobileToggle = true,
  isLoading = false,
}) => {
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const [localFilters, setLocalFilters] = useState<FiltersType>(filters);
  const [priceRange, setPriceRange] = useState({
    min: filters.minPrice?.toString() || '',
    max: filters.maxPrice?.toString() || '',
  });

  // Load categories on mount
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const categoriesData = await productService.getCategories();
        setCategories(categoriesData);
      } catch (error) {
        console.error('Failed to load categories:', error);
      }
    };

    loadCategories();
  }, []);

  // Sync local filters with props
  useEffect(() => {
    setLocalFilters(filters);
    setPriceRange({
      min: filters.minPrice?.toString() || '',
      max: filters.maxPrice?.toString() || '',
    });
  }, [filters]);

  // Handle filter changes
  const handleFilterChange = (key: keyof FiltersType, value: any) => {
    const updatedFilters = { ...localFilters, [key]: value };
    setLocalFilters(updatedFilters);
    onFiltersChange(updatedFilters);
  };

  // Handle price range changes
  const handlePriceRangeChange = (type: 'min' | 'max', value: string) => {
    const newPriceRange = { ...priceRange, [type]: value };
    setPriceRange(newPriceRange);

    // Convert to numbers and update filters
    const minPrice = newPriceRange.min ? parseFloat(newPriceRange.min) : undefined;
    const maxPrice = newPriceRange.max ? parseFloat(newPriceRange.max) : undefined;

    const updatedFilters = {
      ...localFilters,
      minPrice: !isNaN(minPrice!) ? minPrice : undefined,
      maxPrice: !isNaN(maxPrice!) ? maxPrice : undefined,
    };
    setLocalFilters(updatedFilters);
    onFiltersChange(updatedFilters);
  };

  // Handle tag input
  const handleTagsChange = (tagsString: string) => {
    const tags = tagsString.split(',').map(tag => tag.trim()).filter(tag => tag.length > 0);
    handleFilterChange('tags', tags.length > 0 ? tags : undefined);
  };

  // Clear all filters
  const handleClearAll = () => {
    const emptyFilters: FiltersType = {
      sortBy: 'createdAt',
      sortOrder: 'desc',
    };
    setLocalFilters(emptyFilters);
    setPriceRange({ min: '', max: '' });
    onFiltersChange(emptyFilters);
    onClearFilters();
  };

  // Get active filter count
  const getActiveFilterCount = () => {
    let count = 0;
    if (localFilters.search) count++;
    if (localFilters.category) count++;
    if (localFilters.subcategory) count++;
    if (localFilters.minPrice !== undefined) count++;
    if (localFilters.maxPrice !== undefined) count++;
    if (localFilters.tags && localFilters.tags.length > 0) count++;
    if (localFilters.inStock !== undefined) count++;
    return count;
  };

  const activeFilterCount = getActiveFilterCount();

  const filtersContent = (
    <div className="space-y-6">
      {/* Search */}
      <div>
        <Input
          label="Search Products"
          placeholder="Search by name or description..."
          value={localFilters.search || ''}
          onChange={(e) => handleFilterChange('search', e.target.value || undefined)}
          leftIcon={<SearchIcon />}
          fullWidth
        />
      </div>

      {/* Categories */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Category
        </label>
        <select
          value={localFilters.category || ''}
          onChange={(e) => handleFilterChange('category', e.target.value || undefined)}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
        >
          <option value="">All Categories</option>
          {categories.map((category) => (
            <option key={category.id} value={category.name}>
              {category.name}
            </option>
          ))}
        </select>
      </div>

      {/* Subcategories */}
      {localFilters.category && (
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Subcategory
          </label>
          <select
            value={localFilters.subcategory || ''}
            onChange={(e) => handleFilterChange('subcategory', e.target.value || undefined)}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
          >
            <option value="">All Subcategories</option>
            {categories
              .find(cat => cat.name === localFilters.category)
              ?.subcategories?.map((subcat) => (
                <option key={subcat.id} value={subcat.name}>
                  {subcat.name}
                </option>
              ))}
          </select>
        </div>
      )}

      {/* Price Range */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Price Range
        </label>
        <div className="grid grid-cols-2 gap-2">
          <Input
            placeholder="Min price"
            type="number"
            step="0.01"
            value={priceRange.min}
            onChange={(e) => handlePriceRangeChange('min', e.target.value)}
            fullWidth
          />
          <Input
            placeholder="Max price"
            type="number"
            step="0.01"
            value={priceRange.max}
            onChange={(e) => handlePriceRangeChange('max', e.target.value)}
            fullWidth
          />
        </div>
      </div>

      {/* Tags */}
      <div>
        <Input
          label="Tags"
          placeholder="Enter tags separated by commas"
          value={localFilters.tags?.join(', ') || ''}
          onChange={(e) => handleTagsChange(e.target.value)}
          fullWidth
        />
      </div>

      {/* Sort Options */}
      <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">
          Sort By
        </label>
        <select
          value={`${localFilters.sortBy}-${localFilters.sortOrder}`}
          onChange={(e) => {
            const [sortBy, sortOrder] = e.target.value.split('-');
            handleFilterChange('sortBy', sortBy as FiltersType['sortBy']);
            handleFilterChange('sortOrder', sortOrder as FiltersType['sortOrder']);
          }}
          className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
        >
          <option value="createdAt-desc">Newest First</option>
          <option value="createdAt-asc">Oldest First</option>
          <option value="name-asc">Name A-Z</option>
          <option value="name-desc">Name Z-A</option>
          <option value="price-asc">Price: Low to High</option>
          <option value="price-desc">Price: High to Low</option>
          <option value="rating-desc">Highest Rated</option>
          <option value="rating-asc">Lowest Rated</option>
        </select>
      </div>

      {/* Stock Status */}
      <div>
        <label className="flex items-center space-x-2">
          <input
            type="checkbox"
            checked={localFilters.inStock || false}
            onChange={(e) => handleFilterChange('inStock', e.target.checked || undefined)}
            className="w-4 h-4 text-ocean-600 border-gray-300 rounded focus:ring-ocean-500"
          />
          <span className="text-sm text-gray-700">In Stock Only</span>
        </label>
      </div>

      {/* Action Buttons */}
      <div className="flex flex-col sm:flex-row gap-2">
        <Button
          onClick={onApplyFilters}
          variant="primary"
          size="sm"
          fullWidth
          loading={isLoading}
          loadingText="Applying..."
        >
          Apply Filters
        </Button>
        
        <Button
          onClick={handleClearAll}
          variant="outline"
          size="sm"
          fullWidth
          disabled={activeFilterCount === 0}
        >
          Clear All
        </Button>
      </div>

      {/* Active Filters Count */}
      {activeFilterCount > 0 && (
        <div className="text-sm text-gray-600 text-center">
          {activeFilterCount} filter{activeFilterCount !== 1 ? 's' : ''} active
        </div>
      )}
    </div>
  );

  return (
    <>
      {/* Mobile Toggle Button */}
      {showMobileToggle && (
        <div className="lg:hidden mb-4">
          <Button
            onClick={() => setIsOpen(!isOpen)}
            variant="outline"
            leftIcon={<FilterIcon />}
            rightIcon={
              activeFilterCount > 0 ? (
                <span className="bg-ocean-500 text-white text-xs rounded-full px-2 py-0.5 ml-2">
                  {activeFilterCount}
                </span>
              ) : undefined
            }
          >
            Filters {isOpen ? 'Hide' : 'Show'}
          </Button>
        </div>
      )}

      {/* Desktop Filters */}
      <Card className={cn("hidden lg:block", className)} padding="lg">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-gray-900 flex items-center">
            <FilterIcon className="mr-2" />
            Filters
          </h3>
          {activeFilterCount > 0 && (
            <span className="bg-ocean-100 text-ocean-800 text-sm px-2 py-1 rounded-full">
              {activeFilterCount}
            </span>
          )}
        </div>
        {filtersContent}
      </Card>

      {/* Mobile Filters Modal/Drawer */}
      {showMobileToggle && isOpen && (
        <div className="lg:hidden fixed inset-0 z-50 overflow-y-auto">
          <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
            {/* Background overlay */}
            <div 
              className="fixed inset-0 transition-opacity bg-gray-500 bg-opacity-75"
              onClick={() => setIsOpen(false)}
            />

            {/* Modal content */}
            <div className="inline-block w-full max-w-md p-6 my-8 text-left align-middle transition-all transform bg-white shadow-xl rounded-2xl">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900 flex items-center">
                  <FilterIcon className="mr-2" />
                  Filters
                </h3>
                <button
                  onClick={() => setIsOpen(false)}
                  className="text-gray-400 hover:text-gray-600"
                >
                  <XIcon />
                </button>
              </div>
              {filtersContent}
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ProductFilters;