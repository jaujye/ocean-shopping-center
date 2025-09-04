import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { 
  ShoppingBagIcon,
  CurrencyDollarIcon,
  ChartBarIcon,
  UsersIcon,
  PlusIcon,
  EyeIcon
} from '@heroicons/react/24/outline';
import Card, { CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';

const StoreDashboard: React.FC = () => {
  const { user } = useAuth();

  const stats = [
    {
      title: 'Total Products',
      value: '24',
      icon: <ShoppingBagIcon className="h-6 w-6 text-ocean-500" />,
      description: 'Active listings',
      change: '+2 this week'
    },
    {
      title: 'Revenue',
      value: '$2,847',
      icon: <CurrencyDollarIcon className="h-6 w-6 text-green-500" />,
      description: 'This month',
      change: '+12% from last month'
    },
    {
      title: 'Orders',
      value: '18',
      icon: <ChartBarIcon className="h-6 w-6 text-teal-500" />,
      description: 'Pending orders',
      change: '3 need attention'
    },
    {
      title: 'Customers',
      value: '156',
      icon: <UsersIcon className="h-6 w-6 text-aqua-500" />,
      description: 'Total customers',
      change: '+8 this month'
    },
  ];

  const recentOrders = [
    {
      id: 'ORD-101',
      customer: 'John Doe',
      product: 'Wireless Headphones',
      amount: '$89.99',
      status: 'Processing',
      date: '2024-01-15'
    },
    {
      id: 'ORD-102',
      customer: 'Jane Smith',
      product: 'Bluetooth Speaker',
      amount: '$45.50',
      status: 'Shipped',
      date: '2024-01-14'
    },
    {
      id: 'ORD-103',
      customer: 'Mike Johnson',
      product: 'Smart Watch',
      amount: '$199.99',
      status: 'Delivered',
      date: '2024-01-12'
    },
  ];

  const topProducts = [
    {
      name: 'Wireless Headphones',
      sales: 15,
      revenue: '$1,349.85',
      stock: 8
    },
    {
      name: 'Bluetooth Speaker',
      sales: 12,
      revenue: '$546.00',
      stock: 15
    },
    {
      name: 'Smart Watch',
      sales: 8,
      revenue: '$1,599.92',
      stock: 3
    },
  ];

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'delivered':
        return 'text-green-600 bg-green-50';
      case 'shipped':
        return 'text-blue-600 bg-blue-50';
      case 'processing':
        return 'text-yellow-600 bg-yellow-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  const getStockColor = (stock: number) => {
    if (stock <= 5) return 'text-red-600';
    if (stock <= 10) return 'text-yellow-600';
    return 'text-green-600';
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Welcome Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Store Dashboard 🏪
        </h1>
        <p className="text-gray-600 mt-2">
          Manage your store and track your business performance, {user?.name}.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat, index) => (
          <Card key={index} hover>
            <CardContent>
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                  <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                  <p className="text-xs text-gray-500">{stat.description}</p>
                </div>
                <div className="flex-shrink-0">
                  {stat.icon}
                </div>
              </div>
              <div className="mt-2">
                <p className="text-xs text-gray-500">{stat.change}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Recent Orders */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle size="md">Recent Orders</CardTitle>
              <Button variant="ghost" size="sm">
                View All
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentOrders.map((order) => (
                <div key={order.id} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                  <div className="flex-1">
                    <div className="flex items-center justify-between mb-1">
                      <p className="font-medium text-gray-900">{order.id}</p>
                      <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(order.status)}`}>
                        {order.status}
                      </span>
                    </div>
                    <p className="text-sm text-gray-600 mb-1">{order.customer} • {order.product}</p>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">{order.date}</span>
                      <span className="font-semibold text-gray-900">{order.amount}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Top Products */}
        <Card>
          <CardHeader>
            <CardTitle size="md">Top Performing Products</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {topProducts.map((product, index) => (
                <div key={index} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg">
                  <div className="flex-1">
                    <p className="font-medium text-gray-900 mb-1">{product.name}</p>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-600">{product.sales} sold</span>
                      <span className="font-semibold text-gray-900">{product.revenue}</span>
                    </div>
                    <div className="mt-1">
                      <span className={`text-xs font-medium ${getStockColor(product.stock)}`}>
                        {product.stock} in stock
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Quick Actions */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle size="md">Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <Button variant="primary" className="h-20 flex-col">
              <PlusIcon className="h-6 w-6 mb-2" />
              <span>Add Product</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col">
              <ShoppingBagIcon className="h-6 w-6 mb-2" />
              <span>Manage Inventory</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col">
              <ChartBarIcon className="h-6 w-6 mb-2" />
              <span>View Analytics</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col">
              <EyeIcon className="h-6 w-6 mb-2" />
              <span>Store Preview</span>
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* Store Performance Chart */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle size="md">Sales Performance</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8">
            <div className="w-16 h-16 bg-teal-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">📊</span>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Detailed Analytics Coming Soon
            </h3>
            <p className="text-gray-600 mb-4">
              We're building comprehensive analytics to help you understand your store's performance.
            </p>
            <Button variant="outline">
              View Basic Reports
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default StoreDashboard;