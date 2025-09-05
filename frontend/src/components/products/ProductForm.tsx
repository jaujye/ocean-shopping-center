import React, { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { 
  TagIcon, 
  CurrencyDollarIcon, 
  BuildingStorefrontIcon,
  InformationCircleIcon
} from '@heroicons/react/24/outline';
import { cn } from '../../utils/cn';
import Button from '../ui/Button';
import Input from '../ui/Input';
import Card, { CardContent, CardHeader, CardTitle } from '../ui/Card';
import ImageUploader from './ImageUploader';
import { Product } from '../../types';
import { 
  ProductFormData, 
  ProductValidationErrors, 
  ProductCategory 
} from '../../types/product';
import { productService } from '../../services/productService';

interface ProductFormProps {
  product?: Product;
  onSubmit: (data: ProductFormData & { images: string[] }) => Promise<void>;
  onCancel: () => void;
  loading?: boolean;
  className?: string;
}

const ProductForm: React.FC<ProductFormProps> = ({
  product,
  onSubmit,
  onCancel,
  loading = false,
  className,
}) => {
  const [categories, setCategories] = useState<ProductCategory[]>([]);
  const [customTags, setCustomTags] = useState<string>('');
  const [specifications, setSpecifications] = useState<Record<string, string>>(
    product?.specifications || { 'Brand': '', 'Model': '', 'Color': '', 'Material': '' }
  );
  const [images, setImages] = useState<string[]>(product?.images || []);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    control,
    watch,
    setValue,
    formState: { errors, isValid },
    reset,
  } = useForm<ProductFormData>({
    mode: 'onChange',
    defaultValues: {
      name: product?.name || '',
      description: product?.description || '',
      price: product?.price || 0,
      originalPrice: product?.originalPrice || undefined,
      category: product?.category || '',
      subcategory: product?.subcategory || '',
      stockQuantity: product?.stockQuantity || 0,
      tags: product?.tags || [],
      isActive: product?.isActive ?? true,
    },
  });

  // Watch category changes to update subcategories
  const selectedCategory = watch('category');
  const currentPrice = watch('price');
  const originalPrice = watch('originalPrice');

  // Load categories on mount
  useEffect(() => {
    const loadCategories = async () => {
      try {
        const categoriesData = await productService.getCategories();
        setCategories(categoriesData);
      } catch (error) {
        console.warn('Failed to load categories:', error);
        // Fallback categories
        setCategories([
          {
            id: 'electronics',
            name: 'Electronics',
            subcategories: [
              { id: 'smartphones', name: 'Smartphones', categoryId: 'electronics' },
              { id: 'laptops', name: 'Laptops', categoryId: 'electronics' },
              { id: 'tablets', name: 'Tablets', categoryId: 'electronics' },
            ]
          },
          {
            id: 'clothing',
            name: 'Clothing',
            subcategories: [
              { id: 'mens', name: "Men's Clothing", categoryId: 'clothing' },
              { id: 'womens', name: "Women's Clothing", categoryId: 'clothing' },
              { id: 'accessories', name: 'Accessories', categoryId: 'clothing' },
            ]
          },
          {
            id: 'home',
            name: 'Home & Garden',
            subcategories: [
              { id: 'furniture', name: 'Furniture', categoryId: 'home' },
              { id: 'appliances', name: 'Appliances', categoryId: 'home' },
              { id: 'decor', name: 'Home Decor', categoryId: 'home' },
            ]
          },
        ]);
      }
    };

    loadCategories();
  }, []);

  // Get subcategories for selected category
  const getSubcategories = () => {
    const category = categories.find(cat => cat.id === selectedCategory);
    return category?.subcategories || [];
  };

  // Handle specification changes
  const handleSpecificationChange = (key: string, value: string) => {
    setSpecifications(prev => ({ ...prev, [key]: value }));
  };

  // Add new specification
  const addSpecification = () => {
    const key = prompt('Enter specification name:');
    if (key && !specifications[key]) {
      setSpecifications(prev => ({ ...prev, [key]: '' }));
    }
  };

  // Remove specification
  const removeSpecification = (key: string) => {
    const { [key]: removed, ...rest } = specifications;
    setSpecifications(rest);
  };

  // Handle tags
  const handleTagsChange = (tagString: string) => {
    setCustomTags(tagString);
    const tags = tagString
      .split(',')
      .map(tag => tag.trim())
      .filter(tag => tag.length > 0);
    setValue('tags', tags);
  };

  // Form submission
  const onFormSubmit = async (data: ProductFormData) => {
    setSubmitError(null);
    
    try {
      // Validate images
      if (images.length === 0) {
        setSubmitError('At least one product image is required');
        return;
      }

      // Prepare form data
      const formData = {
        ...data,
        images,
        specifications: Object.keys(specifications).length > 0 ? specifications : undefined,
      };

      await onSubmit(formData);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : 'Failed to save product');
    }
  };

  const selectedCategoryData = categories.find(cat => cat.id === selectedCategory);
  const subcategories = selectedCategoryData?.subcategories || [];

  const hasDiscount = originalPrice && originalPrice > currentPrice;
  const discountPercentage = hasDiscount ? Math.round(((originalPrice - currentPrice) / originalPrice) * 100) : 0;

  return (
    <form onSubmit={handleSubmit(onFormSubmit)} className={cn('space-y-6', className)}>
      {/* Basic Information */}
      <Card>
        <CardHeader>
          <CardTitle size="md">Basic Information</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <Input
            {...register('name', { 
              required: 'Product name is required',
              minLength: { value: 3, message: 'Name must be at least 3 characters' },
              maxLength: { value: 100, message: 'Name must be less than 100 characters' }
            })}
            label="Product Name *"
            placeholder="Enter product name"
            error={errors.name?.message}
            disabled={loading}
          />

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Description *
            </label>
            <textarea
              {...register('description', { 
                required: 'Product description is required',
                minLength: { value: 10, message: 'Description must be at least 10 characters' },
                maxLength: { value: 2000, message: 'Description must be less than 2000 characters' }
              })}
              className={cn(
                'block w-full px-3 py-2 border border-gray-300 rounded-lg shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500 transition-colors duration-200 min-h-24 resize-y',
                errors.description && 'border-red-300 focus:ring-red-500 focus:border-red-500'
              )}
              placeholder="Detailed product description..."
              disabled={loading}
              rows={4}
            />
            {errors.description && (
              <p className="mt-1 text-sm text-red-600">{errors.description.message}</p>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              {...register('category', { required: 'Category is required' })}
              label="Category *"
              placeholder="Select category"
              error={errors.category?.message}
              disabled={loading}
              list="categories"
            />
            <datalist id="categories">
              {categories.map((category) => (
                <option key={category.id} value={category.id}>
                  {category.name}
                </option>
              ))}
            </datalist>

            {subcategories.length > 0 && (
              <div>
                <Input
                  {...register('subcategory')}
                  label="Subcategory"
                  placeholder="Select subcategory"
                  disabled={loading}
                  list="subcategories"
                />
                <datalist id="subcategories">
                  {subcategories.map((subcategory) => (
                    <option key={subcategory.id} value={subcategory.id}>
                      {subcategory.name}
                    </option>
                  ))}
                </datalist>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Pricing & Inventory */}
      <Card>
        <CardHeader>
          <CardTitle size="md">Pricing & Inventory</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <Input
              {...register('price', { 
                required: 'Price is required',
                min: { value: 0.01, message: 'Price must be greater than 0' },
                valueAsNumber: true
              })}
              type="number"
              step="0.01"
              label="Price *"
              placeholder="0.00"
              leftIcon={<CurrencyDollarIcon className="h-4 w-4" />}
              error={errors.price?.message}
              disabled={loading}
            />

            <Input
              {...register('originalPrice', { 
                min: { value: 0.01, message: 'Original price must be greater than 0' },
                validate: (value) => {
                  if (value && currentPrice && value <= currentPrice) {
                    return 'Original price must be higher than current price';
                  }
                  return true;
                },
                valueAsNumber: true
              })}
              type="number"
              step="0.01"
              label="Original Price (for discounts)"
              placeholder="0.00"
              leftIcon={<CurrencyDollarIcon className="h-4 w-4" />}
              error={errors.originalPrice?.message}
              disabled={loading}
            />
          </div>

          {hasDiscount && (
            <div className="bg-green-50 border border-green-200 rounded-lg p-3">
              <div className="flex items-center">
                <InformationCircleIcon className="h-5 w-5 text-green-500 mr-2" />
                <span className="text-sm text-green-700">
                  This product has a {discountPercentage}% discount
                </span>
              </div>
            </div>
          )}

          <Input
            {...register('stockQuantity', { 
              required: 'Stock quantity is required',
              min: { value: 0, message: 'Stock quantity cannot be negative' },
              valueAsNumber: true
            })}
            type="number"
            label="Stock Quantity *"
            placeholder="0"
            leftIcon={<BuildingStorefrontIcon className="h-4 w-4" />}
            error={errors.stockQuantity?.message}
            disabled={loading}
          />
        </CardContent>
      </Card>

      {/* Product Images */}
      <Card>
        <CardHeader>
          <CardTitle size="md">Product Images</CardTitle>
        </CardHeader>
        <CardContent>
          <ImageUploader
            images={images}
            onImagesChange={setImages}
            disabled={loading}
            maxImages={10}
          />
        </CardContent>
      </Card>

      {/* Tags & Specifications */}
      <Card>
        <CardHeader>
          <CardTitle size="md">Tags & Specifications</CardTitle>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <Input
              value={customTags}
              onChange={(e) => handleTagsChange(e.target.value)}
              label="Tags"
              placeholder="Enter tags separated by commas (e.g., electronics, smartphone, wireless)"
              leftIcon={<TagIcon className="h-4 w-4" />}
              helpText="Tags help customers find your product through search"
              disabled={loading}
            />
          </div>

          <div>
            <div className="flex items-center justify-between mb-3">
              <label className="block text-sm font-medium text-gray-700">
                Specifications
              </label>
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={addSpecification}
                disabled={loading}
              >
                + Add Specification
              </Button>
            </div>
            
            <div className="space-y-3">
              {Object.entries(specifications).map(([key, value]) => (
                <div key={key} className="flex items-center space-x-2">
                  <div className="flex-1 grid grid-cols-2 gap-2">
                    <input
                      type="text"
                      value={key}
                      className="px-3 py-2 border border-gray-300 rounded-lg text-sm bg-gray-50"
                      disabled
                    />
                    <input
                      type="text"
                      value={value}
                      onChange={(e) => handleSpecificationChange(key, e.target.value)}
                      className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
                      placeholder={`Enter ${key.toLowerCase()}`}
                      disabled={loading}
                    />
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={() => removeSpecification(key)}
                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                    disabled={loading}
                  >
                    Remove
                  </Button>
                </div>
              ))}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Status */}
      <Card>
        <CardHeader>
          <CardTitle size="md">Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center space-x-3">
            <Controller
              name="isActive"
              control={control}
              render={({ field }) => (
                <label className="flex items-center cursor-pointer">
                  <input
                    type="checkbox"
                    checked={field.value}
                    onChange={field.onChange}
                    className="rounded border-gray-300 text-ocean-600 focus:ring-ocean-500"
                    disabled={loading}
                  />
                  <span className="ml-2 text-sm text-gray-700">
                    Product is active and visible to customers
                  </span>
                </label>
              )}
            />
          </div>
        </CardContent>
      </Card>

      {/* Error Display */}
      {submitError && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="text-sm text-red-700">{submitError}</div>
        </div>
      )}

      {/* Form Actions */}
      <div className="flex justify-end space-x-3">
        <Button
          type="button"
          variant="outline"
          onClick={onCancel}
          disabled={loading}
        >
          Cancel
        </Button>
        <Button
          type="submit"
          variant="primary"
          loading={loading}
          disabled={!isValid || loading}
        >
          {product ? 'Update Product' : 'Create Product'}
        </Button>
      </div>
    </form>
  );
};

export default ProductForm;