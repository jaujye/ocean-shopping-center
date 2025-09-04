import React, { useState, useCallback } from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { 
  PlusIcon, 
  ChartBarIcon,
  ExclamationTriangleIcon,
  InformationCircleIcon,
  ArrowDownTrayIcon,
  ArrowUpTrayIcon,
} from '@heroicons/react/24/outline';
import Button from '../../components/ui/Button';
import Card, { CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Modal from '../../components/ui/Modal';
import LoadingSpinner from '../../components/ui/LoadingSpinner';
import ProductList from '../../components/products/ProductList';
import ProductForm from '../../components/products/ProductForm';
import { useProducts } from '../../hooks/useProducts';
import { 
  Product, 
  ProductFormData, 
  ProductFilters,
  BulkAction,
} from '../../types/product';

type ViewMode = 'list' | 'create' | 'edit' | 'stats';

const ProductManagement: React.FC = () => {
  const { user } = useAuth();
  const [viewMode, setViewMode] = useState<ViewMode>('list');
  const [editingProduct, setEditingProduct] = useState<Product | null>(null);
  const [showNotification, setShowNotification] = useState<{
    type: 'success' | 'error' | 'warning';
    message: string;
  } | null>(null);

  // Use the products hook
  const {
    products,
    loading,
    error,
    pagination,
    stats,
    selectedProducts,
    fetchProducts,
    createProduct,
    updateProduct,
    deleteProduct,
    duplicateProduct,
    toggleProductSelection,
    selectAllProducts,
    clearSelection,
    performBulkAction,
    setPage,
    setLimit,
  } = useProducts();

  // Show notification helper
  const showNotificationMessage = useCallback((type: 'success' | 'error' | 'warning', message: string) => {
    setShowNotification({ type, message });
    setTimeout(() => setShowNotification(null), 5000);
  }, []);

  // Handle product creation
  const handleProductCreate = async (data: ProductFormData & { images: string[] }) => {
    try {
      await createProduct(data);
      setViewMode('list');
      showNotificationMessage('success', 'Product created successfully!');
    } catch (error) {
      showNotificationMessage('error', error instanceof Error ? error.message : 'Failed to create product');
    }
  };

  // Handle product update
  const handleProductUpdate = async (data: ProductFormData & { images: string[] }) => {
    if (!editingProduct) return;
    
    try {
      await updateProduct(editingProduct.id, data);
      setEditingProduct(null);
      setViewMode('list');
      showNotificationMessage('success', 'Product updated successfully!');
    } catch (error) {
      showNotificationMessage('error', error instanceof Error ? error.message : 'Failed to update product');
    }
  };

  // Handle product editing
  const handleProductEdit = useCallback((product: Product) => {
    setEditingProduct(product);
    setViewMode('edit');
  }, []);

  // Handle product deletion
  const handleProductDelete = async (productId: string) => {
    try {
      await deleteProduct(productId);
      showNotificationMessage('success', 'Product deleted successfully!');
    } catch (error) {
      showNotificationMessage('error', error instanceof Error ? error.message : 'Failed to delete product');
    }
  };

  // Handle product duplication
  const handleProductDuplicate = async (productId: string) => {
    try {
      const modifications = {
        name: `${products.find(p => p.id === productId)?.name} (Copy)`,
      };
      await duplicateProduct(productId, modifications);
      showNotificationMessage('success', 'Product duplicated successfully!');
    } catch (error) {
      showNotificationMessage('error', error instanceof Error ? error.message : 'Failed to duplicate product');
    }
  };

  // Handle filter changes
  const handleFilterChange = useCallback((filters: ProductFilters) => {
    fetchProducts(filters);
  }, [fetchProducts]);

  // Handle bulk actions
  const handleBulkAction = async (action: BulkAction) => {
    try {
      await performBulkAction(action);
      
      const actionMessages = {
        activate: 'Products activated successfully!',
        deactivate: 'Products deactivated successfully!',
        delete: 'Products deleted successfully!',
        updateCategory: 'Product categories updated successfully!',
        updatePrice: 'Product prices updated successfully!',
      };
      
      showNotificationMessage('success', actionMessages[action.type] || 'Bulk action completed successfully!');
    } catch (error) {
      showNotificationMessage('error', error instanceof Error ? error.message : 'Failed to perform bulk action');
    }
  };

  // Cancel form editing
  const handleCancelEdit = () => {
    setEditingProduct(null);
    setViewMode('list');
  };

  // Get stock alerts
  const getStockAlerts = () => {
    const lowStockProducts = products.filter(p => p.stockQuantity <= 10 && p.stockQuantity > 0);
    const outOfStockProducts = products.filter(p => p.stockQuantity === 0);
    
    return {
      lowStock: lowStockProducts.length,
      outOfStock: outOfStockProducts.length,
      total: lowStockProducts.length + outOfStockProducts.length,
    };
  };

  const stockAlerts = getStockAlerts();

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between mb-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">
            Product Management
          </h1>
          <p className="text-gray-600 mt-2">
            Manage your store's product catalog and inventory
          </p>
        </div>
        
        <div className="mt-4 sm:mt-0 flex space-x-3">
          <Button
            variant="outline"
            leftIcon={<ChartBarIcon className="h-4 w-4" />}
            onClick={() => setViewMode(viewMode === 'stats' ? 'list' : 'stats')}
          >
            {viewMode === 'stats' ? 'Hide Stats' : 'View Stats'}
          </Button>
          
          <Button
            variant="primary"
            leftIcon={<PlusIcon className="h-4 w-4" />}
            onClick={() => setViewMode('create')}
          >
            Add Product
          </Button>
        </div>
      </div>

      {/* Notification */}
      {showNotification && (
        <div className={`fixed top-4 right-4 z-50 p-4 rounded-lg shadow-lg max-w-sm ${
          showNotification.type === 'success' ? 'bg-green-50 border border-green-200' :
          showNotification.type === 'error' ? 'bg-red-50 border border-red-200' :
          'bg-yellow-50 border border-yellow-200'
        }`}>
          <div className="flex items-center">
            {showNotification.type === 'success' ? (
              <InformationCircleIcon className="h-5 w-5 text-green-500 mr-2" />
            ) : showNotification.type === 'error' ? (
              <ExclamationTriangleIcon className="h-5 w-5 text-red-500 mr-2" />
            ) : (
              <ExclamationTriangleIcon className="h-5 w-5 text-yellow-500 mr-2" />
            )}
            <span className={`text-sm font-medium ${
              showNotification.type === 'success' ? 'text-green-800' :
              showNotification.type === 'error' ? 'text-red-800' :
              'text-yellow-800'
            }`}>
              {showNotification.message}
            </span>
          </div>
        </div>
      )}

      {/* Stats Overview */}
      {(viewMode === 'stats' || stockAlerts.total > 0) && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          {stats && (
            <>
              <Card>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-2xl font-bold text-gray-900">{stats.totalProducts}</p>
                      <p className="text-sm font-medium text-gray-600">Total Products</p>
                      <p className="text-xs text-gray-500">{stats.activeProducts} active</p>
                    </div>
                    <div className="text-ocean-500">
                      <ChartBarIcon className="h-8 w-8" />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-2xl font-bold text-gray-900">${stats.totalValue.toLocaleString()}</p>
                      <p className="text-sm font-medium text-gray-600">Inventory Value</p>
                      <p className="text-xs text-gray-500">Total stock value</p>
                    </div>
                    <div className="text-green-500">
                      <ChartBarIcon className="h-8 w-8" />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-2xl font-bold text-yellow-600">{stats.lowStock}</p>
                      <p className="text-sm font-medium text-gray-600">Low Stock</p>
                      <p className="text-xs text-gray-500">≤10 items remaining</p>
                    </div>
                    <div className="text-yellow-500">
                      <ExclamationTriangleIcon className="h-8 w-8" />
                    </div>
                  </div>
                </CardContent>
              </Card>

              <Card>
                <CardContent>
                  <div className="flex items-center justify-between">
                    <div>
                      <p className="text-2xl font-bold text-red-600">{stats.outOfStock}</p>
                      <p className="text-sm font-medium text-gray-600">Out of Stock</p>
                      <p className="text-xs text-gray-500">0 items remaining</p>
                    </div>
                    <div className="text-red-500">
                      <ExclamationTriangleIcon className="h-8 w-8" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            </>
          )}
        </div>
      )}

      {/* Stock Alerts */}
      {stockAlerts.total > 0 && viewMode === 'list' && (
        <Card className="mb-6 border-yellow-200 bg-yellow-50">
          <CardContent className="p-4">
            <div className="flex items-center">
              <ExclamationTriangleIcon className="h-5 w-5 text-yellow-600 mr-2" />
              <div className="flex-1">
                <p className="text-sm font-medium text-yellow-800">
                  Stock Alert: {stockAlerts.outOfStock} products out of stock, {stockAlerts.lowStock} running low
                </p>
                <p className="text-xs text-yellow-700">
                  Consider restocking these items to avoid lost sales
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Error Display */}
      {error && (
        <Card className="mb-6 border-red-200 bg-red-50">
          <CardContent className="p-4">
            <div className="flex items-center">
              <ExclamationTriangleIcon className="h-5 w-5 text-red-600 mr-2" />
              <div className="text-sm text-red-700">{error}</div>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Main Content */}
      {viewMode === 'list' && (
        <ProductList
          products={products}
          loading={loading}
          selectedProducts={selectedProducts}
          pagination={pagination}
          onProductSelect={toggleProductSelection}
          onSelectAll={selectAllProducts}
          onClearSelection={clearSelection}
          onFilterChange={handleFilterChange}
          onPageChange={setPage}
          onLimitChange={setLimit}
          onProductEdit={handleProductEdit}
          onProductDelete={handleProductDelete}
          onProductDuplicate={handleProductDuplicate}
          onBulkAction={handleBulkAction}
        />
      )}

      {viewMode === 'create' && (
        <Card>
          <CardHeader>
            <CardTitle size="lg">Add New Product</CardTitle>
          </CardHeader>
          <CardContent>
            <ProductForm
              onSubmit={handleProductCreate}
              onCancel={() => setViewMode('list')}
              loading={loading}
            />
          </CardContent>
        </Card>
      )}

      {viewMode === 'edit' && editingProduct && (
        <Card>
          <CardHeader>
            <CardTitle size="lg">Edit Product</CardTitle>
          </CardHeader>
          <CardContent>
            <ProductForm
              product={editingProduct}
              onSubmit={handleProductUpdate}
              onCancel={handleCancelEdit}
              loading={loading}
            />
          </CardContent>
        </Card>
      )}

      {viewMode === 'stats' && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <Card>
            <CardHeader>
              <CardTitle size="md">Product Performance</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="text-center py-8">
                <div className="w-16 h-16 bg-ocean-100 rounded-full flex items-center justify-center mx-auto mb-4">
                  <ChartBarIcon className="h-8 w-8 text-ocean-600" />
                </div>
                <h3 className="text-lg font-medium text-gray-900 mb-2">
                  Detailed Analytics Coming Soon
                </h3>
                <p className="text-gray-600 mb-4">
                  Track your best-selling products, conversion rates, and revenue analytics.
                </p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle size="md">Inventory Management</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <span className="text-sm font-medium">Export Products</span>
                  <Button variant="outline" size="sm" leftIcon={<ArrowDownTrayIcon className="h-4 w-4" />}>
                    CSV
                  </Button>
                </div>
                
                <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <span className="text-sm font-medium">Import Products</span>
                  <Button variant="outline" size="sm" leftIcon={<ArrowUpTrayIcon className="h-4 w-4" />}>
                    Upload
                  </Button>
                </div>
                
                <div className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <span className="text-sm font-medium">Bulk Price Update</span>
                  <Button variant="outline" size="sm">
                    Update
                  </Button>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
};

export default ProductManagement;