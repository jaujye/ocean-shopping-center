import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { 
  ShoppingCartIcon,
  HeartIcon,
  TruckIcon,
  UserCircleIcon 
} from '@heroicons/react/24/outline';
import Card, { CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';

const CustomerDashboard: React.FC = () => {
  const { user } = useAuth();

  const stats = [
    {
      title: 'Active Orders',
      value: '3',
      icon: <TruckIcon className="h-6 w-6 text-ocean-500" />,
      description: 'Orders in progress'
    },
    {
      title: 'Wishlist Items',
      value: '12',
      icon: <HeartIcon className="h-6 w-6 text-pink-500" />,
      description: 'Saved for later'
    },
    {
      title: 'Total Orders',
      value: '47',
      icon: <ShoppingCartIcon className="h-6 w-6 text-teal-500" />,
      description: 'All time purchases'
    },
    {
      title: 'Profile Score',
      value: '92%',
      icon: <UserCircleIcon className="h-6 w-6 text-aqua-500" />,
      description: 'Profile completion'
    },
  ];

  const recentOrders = [
    {
      id: 'ORD-001',
      items: 'Wireless Headphones, Phone Case',
      total: '$89.99',
      status: 'Delivered',
      date: '2024-01-15'
    },
    {
      id: 'ORD-002',
      items: 'Bluetooth Speaker',
      total: '$45.50',
      status: 'Shipped',
      date: '2024-01-12'
    },
    {
      id: 'ORD-003',
      items: 'Smart Watch, Charging Cable',
      total: '$199.99',
      status: 'Processing',
      date: '2024-01-10'
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

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Welcome Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Welcome back, {user?.name}! 👋
        </h1>
        <p className="text-gray-600 mt-2">
          Here's what's happening with your account today.
        </p>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat, index) => (
          <Card key={index} hover>
            <CardContent className="flex items-center">
              <div className="flex-shrink-0">
                {stat.icon}
              </div>
              <div className="ml-4">
                <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                <p className="text-sm font-medium text-gray-600">{stat.title}</p>
                <p className="text-xs text-gray-500">{stat.description}</p>
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
                    <p className="text-sm text-gray-600 mb-1">{order.items}</p>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-gray-500">{order.date}</span>
                      <span className="font-semibold text-gray-900">{order.total}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Quick Actions */}
        <Card>
          <CardHeader>
            <CardTitle size="md">Quick Actions</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 gap-4">
              <Button variant="outline" className="h-20 flex-col">
                <ShoppingCartIcon className="h-6 w-6 mb-2" />
                <span>Browse Products</span>
              </Button>
              <Button variant="outline" className="h-20 flex-col">
                <HeartIcon className="h-6 w-6 mb-2" />
                <span>My Wishlist</span>
              </Button>
              <Button variant="outline" className="h-20 flex-col">
                <TruckIcon className="h-6 w-6 mb-2" />
                <span>Track Orders</span>
              </Button>
              <Button variant="outline" className="h-20 flex-col">
                <UserCircleIcon className="h-6 w-6 mb-2" />
                <span>Edit Profile</span>
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Recommendations Section */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle size="md">Recommended for You</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-center py-8">
            <div className="w-16 h-16 bg-ocean-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <span className="text-2xl">🎯</span>
            </div>
            <h3 className="text-lg font-medium text-gray-900 mb-2">
              Personalized Recommendations Coming Soon
            </h3>
            <p className="text-gray-600 mb-4">
              We're working on personalized product recommendations based on your shopping history.
            </p>
            <Button>
              Explore Products
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default CustomerDashboard;