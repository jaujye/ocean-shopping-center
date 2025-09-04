import React, { useState, useMemo } from 'react';
import {
  MagnifyingGlassIcon,
  FunnelIcon,
  Squares2X2Icon,
  ListBulletIcon,
  EllipsisVerticalIcon,
  PencilIcon,
  TrashIcon,
  DocumentDuplicateIcon,
  EyeIcon,
  EyeSlashIcon,
  CheckIcon,
  XMarkIcon,
  ChevronUpIcon,
  ChevronDownIcon,
} from '@heroicons/react/24/outline';
import { cn } from '../../utils/cn';
import Button from '../ui/Button';
import Input from '../ui/Input';
import Card, { CardContent } from '../ui/Card';
import Modal from '../ui/Modal';
import LoadingSpinner from '../ui/LoadingSpinner';
import {
  Product,
  ProductFilters,
  BulkAction,
} from '../types/product';

interface ProductListProps {
  products: Product[];
  loading: boolean;
  selectedProducts: string[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
  onProductSelect: (id: string) => void;
  onSelectAll: () => void;
  onClearSelection: () => void;
  onFilterChange: (filters: ProductFilters) => void;
  onPageChange: (page: number) => void;
  onLimitChange: (limit: number) => void;
  onProductEdit: (product: Product) => void;
  onProductDelete: (id: string) => void;
  onProductDuplicate: (id: string) => void;
  onBulkAction: (action: BulkAction) => void;
  onProductView?: (product: Product) => void;
  className?: string;
}

type ViewMode = 'grid' | 'list';
type SortField = 'name' | 'price' | 'createdAt' | 'stockQuantity' | 'rating';
type SortOrder = 'asc' | 'desc';

const ProductList: React.FC<ProductListProps> = ({
  products,
  loading,
  selectedProducts,
  pagination,
  onProductSelect,
  onSelectAll,
  onClearSelection,
  onFilterChange,
  onPageChange,
  onLimitChange,
  onProductEdit,
  onProductDelete,
  onProductDuplicate,
  onBulkAction,
  onProductView,
  className,
}) => {
  const [viewMode, setViewMode] = useState<ViewMode>('grid');
  const [showFilters, setShowFilters] = useState(false);
  const [sortField, setSortField] = useState<SortField>('createdAt');
  const [sortOrder, setSortOrder] = useState<SortOrder>('desc');
  const [searchTerm, setSearchTerm] = useState('');
  const [filters, setFilters] = useState<ProductFilters>({});
  const [showBulkActions, setShowBulkActions] = useState(false);
  const [productToDelete, setProductToDelete] = useState<string | null>(null);
  const [activeProductMenu, setActiveProductMenu] = useState<string | null>(null);

  // Handle search
  const handleSearch = (value: string) => {
    setSearchTerm(value);
    onFilterChange({ ...filters, search: value });
  };

  // Handle sort
  const handleSort = (field: SortField) => {
    const newOrder = sortField === field && sortOrder === 'asc' ? 'desc' : 'asc';
    setSortField(field);
    setSortOrder(newOrder);
    onFilterChange({ ...filters, sortBy: field, sortOrder: newOrder });
  };

  // Handle filter change
  const handleFilterChange = (newFilters: Partial<ProductFilters>) => {
    const updatedFilters = { ...filters, ...newFilters };
    setFilters(updatedFilters);
    onFilterChange(updatedFilters);
  };

  // Clear all filters
  const clearFilters = () => {
    setFilters({});
    setSearchTerm('');
    onFilterChange({});
  };

  // Handle bulk actions
  const handleBulkAction = async (action: BulkAction) => {
    setShowBulkActions(false);
    await onBulkAction(action);
  };

  // Handle product deletion
  const handleProductDelete = (id: string) => {
    setProductToDelete(id);
  };

  const confirmDelete = async () => {
    if (productToDelete) {
      await onProductDelete(productToDelete);
      setProductToDelete(null);
    }
  };

  // Calculate selection stats
  const allSelected = products.length > 0 && selectedProducts.length === products.length;
  const someSelected = selectedProducts.length > 0 && selectedProducts.length < products.length;

  // Format price
  const formatPrice = (price: number, originalPrice?: number) => {
    const priceStr = `$${price.toFixed(2)}`;
    if (originalPrice && originalPrice > price) {
      return (
        <span className="space-x-2">
          <span className="text-red-600 font-semibold">{priceStr}</span>
          <span className="text-gray-500 line-through text-sm">${originalPrice.toFixed(2)}</span>
        </span>
      );
    }
    return <span className="font-semibold">{priceStr}</span>;
  };

  // Format stock status
  const getStockStatus = (stockQuantity: number) => {
    if (stockQuantity === 0) {
      return <span className="text-red-600 text-sm">Out of stock</span>;
    }
    if (stockQuantity <= 10) {
      return <span className="text-yellow-600 text-sm">Low stock ({stockQuantity})</span>;
    }
    return <span className="text-green-600 text-sm">In stock ({stockQuantity})</span>;
  };

  // Product card component
  const ProductCard: React.FC<{ product: Product }> = ({ product }) => (
    <Card
      key={product.id}
      className={cn(
        'transition-all duration-200 hover:shadow-md cursor-pointer',
        selectedProducts.includes(product.id) && 'ring-2 ring-ocean-500'
      )}
      onClick={() => onProductSelect(product.id)}
    >
      <CardContent className="p-4">
        <div className="flex items-start justify-between mb-3">
          <input
            type="checkbox"
            checked={selectedProducts.includes(product.id)}
            onChange={(e) => {
              e.stopPropagation();
              onProductSelect(product.id);
            }}
            className="rounded border-gray-300 text-ocean-600 focus:ring-ocean-500"
          />
          
          <div className="relative">
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => {
                e.stopPropagation();
                setActiveProductMenu(activeProductMenu === product.id ? null : product.id);
              }}
            >
              <EllipsisVerticalIcon className="h-4 w-4" />
            </Button>
            
            {activeProductMenu === product.id && (
              <div className="absolute right-0 mt-1 w-48 bg-white rounded-md shadow-lg z-10 border">
                <div className="py-1">
                  {onProductView && (
                    <button
                      className="flex items-center w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      onClick={(e) => {
                        e.stopPropagation();
                        onProductView(product);
                        setActiveProductMenu(null);
                      }}
                    >
                      <EyeIcon className="h-4 w-4 mr-2" />
                      View Details
                    </button>
                  )}
                  <button
                    className="flex items-center w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    onClick={(e) => {
                      e.stopPropagation();
                      onProductEdit(product);
                      setActiveProductMenu(null);
                    }}
                  >
                    <PencilIcon className="h-4 w-4 mr-2" />
                    Edit
                  </button>
                  <button
                    className="flex items-center w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    onClick={(e) => {
                      e.stopPropagation();
                      onProductDuplicate(product.id);
                      setActiveProductMenu(null);
                    }}
                  >
                    <DocumentDuplicateIcon className="h-4 w-4 mr-2" />
                    Duplicate
                  </button>
                  <button
                    className="flex items-center w-full px-4 py-2 text-sm text-red-600 hover:bg-red-50"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleProductDelete(product.id);
                      setActiveProductMenu(null);
                    }}
                  >
                    <TrashIcon className="h-4 w-4 mr-2" />
                    Delete
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* Product Image */}
        <div className="aspect-square bg-gray-100 rounded-lg mb-3 overflow-hidden">
          {product.images.length > 0 ? (
            <img
              src={product.images[0]}
              alt={product.name}
              className="w-full h-full object-cover"
            />
          ) : (
            <div className="w-full h-full flex items-center justify-center text-gray-400">
              No Image
            </div>
          )}
        </div>

        {/* Product Info */}
        <div className="space-y-2">
          <div className="flex items-center justify-between">
            <h3 className="font-medium text-gray-900 truncate">{product.name}</h3>
            <div className="flex items-center space-x-1">
              {product.isActive ? (
                <EyeIcon className="h-4 w-4 text-green-500" title="Active" />
              ) : (
                <EyeSlashIcon className="h-4 w-4 text-gray-400" title="Inactive" />
              )}
            </div>
          </div>
          
          <p className="text-sm text-gray-600 line-clamp-2">{product.description}</p>
          
          <div className="flex items-center justify-between">
            {formatPrice(product.price, product.originalPrice)}
            {getStockStatus(product.stockQuantity)}
          </div>
          
          <div className="flex items-center justify-between text-xs text-gray-500">
            <span>{product.category}</span>
            <span>★ {product.rating.toFixed(1)} ({product.reviewCount})</span>
          </div>
        </div>
      </CardContent>
    </Card>
  );

  // Product list row component
  const ProductRow: React.FC<{ product: Product }> = ({ product }) => (
    <tr
      key={product.id}
      className={cn(
        'hover:bg-gray-50 cursor-pointer',
        selectedProducts.includes(product.id) && 'bg-ocean-50'
      )}
      onClick={() => onProductSelect(product.id)}
    >
      <td className="px-6 py-4">
        <input
          type="checkbox"
          checked={selectedProducts.includes(product.id)}
          onChange={(e) => {
            e.stopPropagation();
            onProductSelect(product.id);
          }}
          className="rounded border-gray-300 text-ocean-600 focus:ring-ocean-500"
        />
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center space-x-3">
          <div className="w-12 h-12 bg-gray-100 rounded-lg overflow-hidden flex-shrink-0">
            {product.images.length > 0 ? (
              <img
                src={product.images[0]}
                alt={product.name}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full flex items-center justify-center text-gray-400 text-xs">
                No Image
              </div>
            )}
          </div>
          <div>
            <div className="font-medium text-gray-900">{product.name}</div>
            <div className="text-sm text-gray-500">{product.category}</div>
          </div>
        </div>
      </td>
      <td className="px-6 py-4">
        {formatPrice(product.price, product.originalPrice)}
      </td>
      <td className="px-6 py-4">
        {getStockStatus(product.stockQuantity)}
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center space-x-1">
          <span>★ {product.rating.toFixed(1)}</span>
          <span className="text-gray-500">({product.reviewCount})</span>
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center space-x-1">
          {product.isActive ? (
            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-green-100 text-green-800">
              Active
            </span>
          ) : (
            <span className="inline-flex items-center px-2 py-1 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
              Inactive
            </span>
          )}
        </div>
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center space-x-2">
          {onProductView && (
            <Button
              variant="ghost"
              size="sm"
              onClick={(e) => {
                e.stopPropagation();
                onProductView(product);
              }}
            >
              <EyeIcon className="h-4 w-4" />
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              onProductEdit(product);
            }}
          >
            <PencilIcon className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              onProductDuplicate(product.id);
            }}
          >
            <DocumentDuplicateIcon className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation();
              handleProductDelete(product.id);
            }}
            className="text-red-600 hover:text-red-700 hover:bg-red-50"
          >
            <TrashIcon className="h-4 w-4" />
          </Button>
        </div>
      </td>
    </tr>
  );

  return (
    <div className={cn('space-y-4', className)}>
      {/* Search and Filters */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="flex-1">
          <Input
            placeholder="Search products..."
            value={searchTerm}
            onChange={(e) => handleSearch(e.target.value)}
            leftIcon={<MagnifyingGlassIcon className="h-4 w-4" />}
          />
        </div>
        
        <div className="flex items-center space-x-2">
          <Button
            variant="outline"
            onClick={() => setShowFilters(!showFilters)}
            leftIcon={<FunnelIcon className="h-4 w-4" />}
          >
            Filters
          </Button>
          
          <div className="flex border border-gray-300 rounded-lg">
            <Button
              variant={viewMode === 'grid' ? 'primary' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('grid')}
              className="rounded-r-none border-r-0"
            >
              <Squares2X2Icon className="h-4 w-4" />
            </Button>
            <Button
              variant={viewMode === 'list' ? 'primary' : 'ghost'}
              size="sm"
              onClick={() => setViewMode('list')}
              className="rounded-l-none"
            >
              <ListBulletIcon className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </div>

      {/* Expanded Filters */}
      {showFilters && (
        <Card>
          <CardContent className="p-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Category</label>
                <select
                  value={filters.category || ''}
                  onChange={(e) => handleFilterChange({ category: e.target.value || undefined })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
                >
                  <option value="">All Categories</option>
                  <option value="electronics">Electronics</option>
                  <option value="clothing">Clothing</option>
                  <option value="home">Home & Garden</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                <select
                  value={filters.isActive === undefined ? '' : filters.isActive ? 'active' : 'inactive'}
                  onChange={(e) => {
                    const value = e.target.value;
                    handleFilterChange({ 
                      isActive: value === '' ? undefined : value === 'active'
                    });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
                >
                  <option value="">All Status</option>
                  <option value="active">Active</option>
                  <option value="inactive">Inactive</option>
                </select>
              </div>
              
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Stock Status</label>
                <select
                  value={filters.inStock === undefined ? '' : filters.inStock ? 'instock' : 'outofstock'}
                  onChange={(e) => {
                    const value = e.target.value;
                    handleFilterChange({ 
                      inStock: value === '' ? undefined : value === 'instock'
                    });
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
                >
                  <option value="">All Stock</option>
                  <option value="instock">In Stock</option>
                  <option value="outofstock">Out of Stock</option>
                </select>
              </div>
              
              <div className="flex items-end">
                <Button variant="outline" onClick={clearFilters}>
                  Clear Filters
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Bulk Actions */}
      {selectedProducts.length > 0 && (
        <Card>
          <CardContent className="p-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-4">
                <span className="text-sm text-gray-600">
                  {selectedProducts.length} product{selectedProducts.length > 1 ? 's' : ''} selected
                </span>
                <Button variant="ghost" size="sm" onClick={onClearSelection}>
                  <XMarkIcon className="h-4 w-4 mr-1" />
                  Clear
                </Button>
              </div>
              
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction({ type: 'activate' })}
                >
                  Activate
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleBulkAction({ type: 'deactivate' })}
                >
                  Deactivate
                </Button>
                <Button
                  variant="danger"
                  size="sm"
                  onClick={() => handleBulkAction({ type: 'delete' })}
                >
                  Delete
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Products Display */}
      {loading ? (
        <div className="flex justify-center py-8">
          <LoadingSpinner size="lg" />
        </div>
      ) : products.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-gray-400 mb-4">
            <Squares2X2Icon className="h-12 w-12 mx-auto" />
          </div>
          <h3 className="text-lg font-medium text-gray-900 mb-2">No products found</h3>
          <p className="text-gray-500 mb-4">
            {searchTerm ? 'Try adjusting your search or filters' : 'Get started by adding your first product'}
          </p>
        </div>
      ) : viewMode === 'grid' ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {products.map(product => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      ) : (
        <div className="bg-white shadow rounded-lg overflow-hidden">
          <table className="min-w-full divide-y divide-gray-200">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    ref={(el) => {
                      if (el) el.indeterminate = someSelected;
                    }}
                    onChange={allSelected ? onClearSelection : onSelectAll}
                    className="rounded border-gray-300 text-ocean-600 focus:ring-ocean-500"
                  />
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                  onClick={() => handleSort('name')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Product</span>
                    {sortField === 'name' && (
                      sortOrder === 'asc' ? 
                      <ChevronUpIcon className="h-4 w-4" /> : 
                      <ChevronDownIcon className="h-4 w-4" />
                    )}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                  onClick={() => handleSort('price')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Price</span>
                    {sortField === 'price' && (
                      sortOrder === 'asc' ? 
                      <ChevronUpIcon className="h-4 w-4" /> : 
                      <ChevronDownIcon className="h-4 w-4" />
                    )}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                  onClick={() => handleSort('stockQuantity')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Stock</span>
                    {sortField === 'stockQuantity' && (
                      sortOrder === 'asc' ? 
                      <ChevronUpIcon className="h-4 w-4" /> : 
                      <ChevronDownIcon className="h-4 w-4" />
                    )}
                  </div>
                </th>
                <th
                  className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider cursor-pointer hover:text-gray-700"
                  onClick={() => handleSort('rating')}
                >
                  <div className="flex items-center space-x-1">
                    <span>Rating</span>
                    {sortField === 'rating' && (
                      sortOrder === 'asc' ? 
                      <ChevronUpIcon className="h-4 w-4" /> : 
                      <ChevronDownIcon className="h-4 w-4" />
                    )}
                  </div>
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="bg-white divide-y divide-gray-200">
              {products.map(product => (
                <ProductRow key={product.id} product={product} />
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {pagination.totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <span className="text-sm text-gray-700">Show</span>
            <select
              value={pagination.limit}
              onChange={(e) => onLimitChange(Number(e.target.value))}
              className="px-3 py-1 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-ocean-500 focus:border-ocean-500"
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
              <option value={100}>100</option>
            </select>
            <span className="text-sm text-gray-700">
              per page of {pagination.total} results
            </span>
          </div>
          
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(pagination.page - 1)}
              disabled={pagination.page === 1}
            >
              Previous
            </Button>
            
            <div className="flex items-center space-x-1">
              {Array.from({ length: Math.min(5, pagination.totalPages) }, (_, i) => {
                const pageNum = pagination.page - 2 + i;
                if (pageNum < 1 || pageNum > pagination.totalPages) return null;
                
                return (
                  <Button
                    key={pageNum}
                    variant={pageNum === pagination.page ? 'primary' : 'ghost'}
                    size="sm"
                    onClick={() => onPageChange(pageNum)}
                  >
                    {pageNum}
                  </Button>
                );
              })}
            </div>
            
            <Button
              variant="outline"
              size="sm"
              onClick={() => onPageChange(pagination.page + 1)}
              disabled={pagination.page === pagination.totalPages}
            >
              Next
            </Button>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={!!productToDelete}
        onClose={() => setProductToDelete(null)}
        title="Delete Product"
        size="md"
      >
        <div className="space-y-4">
          <p className="text-gray-600">
            Are you sure you want to delete this product? This action cannot be undone.
          </p>
          
          <div className="flex justify-end space-x-3">
            <Button
              variant="outline"
              onClick={() => setProductToDelete(null)}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={confirmDelete}
            >
              Delete Product
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};

export default ProductList;