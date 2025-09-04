import React from 'react';
import { useAuth } from '../../contexts/AuthContext';
import { 
  UsersIcon,
  BuildingStorefrontIcon,
  ShoppingBagIcon,
  CurrencyDollarIcon,
  ExclamationTriangleIcon,
  CheckCircleIcon,
  ClockIcon,
  ChartBarIcon
} from '@heroicons/react/24/outline';
import Card, { CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import Button from '../../components/ui/Button';

const AdminDashboard: React.FC = () => {
  const { user } = useAuth();

  const stats = [
    {
      title: 'Total Users',
      value: '1,247',
      icon: <UsersIcon className="h-6 w-6 text-ocean-500" />,
      description: 'Active users',
      change: '+45 this week'
    },
    {
      title: 'Active Stores',
      value: '89',
      icon: <BuildingStorefrontIcon className="h-6 w-6 text-teal-500" />,
      description: 'Verified stores',
      change: '+7 this month'
    },
    {
      title: 'Total Products',
      value: '2,156',
      icon: <ShoppingBagIcon className="h-6 w-6 text-aqua-500" />,
      description: 'Listed products',
      change: '+124 this week'
    },
    {
      title: 'Platform Revenue',
      value: '$24,847',
      icon: <CurrencyDollarIcon className="h-6 w-6 text-green-500" />,
      description: 'This month',
      change: '+18% from last month'
    },
  ];

  const pendingActions = [
    {
      type: 'store_verification',
      title: 'Store Verification Requests',
      count: 5,
      icon: <BuildingStorefrontIcon className="h-5 w-5 text-yellow-500" />,
      description: '5 stores awaiting verification'
    },
    {
      type: 'reported_products',
      title: 'Reported Products',
      count: 3,
      icon: <ExclamationTriangleIcon className="h-5 w-5 text-red-500" />,
      description: '3 products reported by users'
    },
    {
      type: 'user_appeals',
      title: 'User Appeals',
      count: 2,
      icon: <UsersIcon className="h-5 w-5 text-blue-500" />,
      description: '2 users appealing suspensions'
    },
  ];

  const recentActivities = [
    {
      action: 'User Registration',
      details: 'John Doe joined as customer',
      timestamp: '5 minutes ago',
      status: 'success'
    },
    {
      action: 'Store Application',
      details: 'TechGear Store submitted verification',
      timestamp: '1 hour ago',
      status: 'pending'
    },
    {
      action: 'Product Report',
      details: 'Counterfeit product reported',
      timestamp: '2 hours ago',
      status: 'warning'
    },
    {
      action: 'Payment Processed',
      details: '$1,250 commission received',
      timestamp: '3 hours ago',
      status: 'success'
    },
  ];

  const systemHealth = [
    {
      service: 'API Server',
      status: 'healthy',
      uptime: '99.9%',
      lastCheck: '30 seconds ago'
    },
    {
      service: 'Database',
      status: 'healthy',
      uptime: '100%',
      lastCheck: '30 seconds ago'
    },
    {
      service: 'Payment Gateway',
      status: 'healthy',
      uptime: '99.8%',
      lastCheck: '1 minute ago'
    },
    {
      service: 'Email Service',
      status: 'warning',
      uptime: '98.5%',
      lastCheck: '2 minutes ago'
    },
  ];

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'success':
        return <CheckCircleIcon className="h-4 w-4 text-green-500" />;
      case 'warning':
        return <ExclamationTriangleIcon className="h-4 w-4 text-yellow-500" />;
      case 'pending':
        return <ClockIcon className="h-4 w-4 text-blue-500" />;
      default:
        return <ClockIcon className="h-4 w-4 text-gray-500" />;
    }
  };

  const getServiceStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'text-green-600 bg-green-50';
      case 'warning':
        return 'text-yellow-600 bg-yellow-50';
      case 'error':
        return 'text-red-600 bg-red-50';
      default:
        return 'text-gray-600 bg-gray-50';
    }
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Welcome Section */}
      <div className="mb-8">
        <h1 className="text-3xl font-bold text-gray-900">
          Admin Dashboard 🛡️
        </h1>
        <p className="text-gray-600 mt-2">
          Monitor and manage the Ocean Shopping platform, {user?.name}.
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
        {/* Pending Actions */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <CardTitle size="md">Pending Actions</CardTitle>
              <span className="bg-red-100 text-red-800 text-xs font-medium px-2.5 py-0.5 rounded-full">
                {pendingActions.reduce((sum, action) => sum + action.count, 0)} total
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {pendingActions.map((action, index) => (
                <div key={index} className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:bg-gray-50 cursor-pointer">
                  <div className="flex items-center">
                    <div className="flex-shrink-0 mr-3">
                      {action.icon}
                    </div>
                    <div>
                      <p className="font-medium text-gray-900">{action.title}</p>
                      <p className="text-sm text-gray-600">{action.description}</p>
                    </div>
                  </div>
                  <div className="flex items-center">
                    <span className="bg-yellow-100 text-yellow-800 text-sm font-medium px-2.5 py-0.5 rounded-full">
                      {action.count}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Recent Activities */}
        <Card>
          <CardHeader>
            <CardTitle size="md">Recent Activities</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="space-y-4">
              {recentActivities.map((activity, index) => (
                <div key={index} className="flex items-start space-x-3">
                  <div className="flex-shrink-0 mt-1">
                    {getStatusIcon(activity.status)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm font-medium text-gray-900">{activity.action}</p>
                    <p className="text-sm text-gray-600">{activity.details}</p>
                    <p className="text-xs text-gray-500 mt-1">{activity.timestamp}</p>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* System Health */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle size="md">System Health</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {systemHealth.map((service, index) => (
              <div key={index} className="p-4 border border-gray-200 rounded-lg">
                <div className="flex items-center justify-between mb-2">
                  <p className="font-medium text-gray-900">{service.service}</p>
                  <span className={`px-2 py-1 rounded-full text-xs font-medium ${getServiceStatusColor(service.status)}`}>
                    {service.status}
                  </span>
                </div>
                <p className="text-sm text-gray-600">Uptime: {service.uptime}</p>
                <p className="text-xs text-gray-500">{service.lastCheck}</p>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Quick Actions */}
      <Card className="mt-8">
        <CardHeader>
          <CardTitle size="md">Quick Actions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
            <Button variant="primary" className="h-20 flex-col">
              <UsersIcon className="h-6 w-6 mb-2" />
              <span>Manage Users</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col">
              <BuildingStorefrontIcon className="h-6 w-6 mb-2" />
              <span>Review Stores</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col">
              <ChartBarIcon className="h-6 w-6 mb-2" />
              <span>View Reports</span>
            </Button>
            <Button variant="outline" className="h-20 flex-col">
              <ExclamationTriangleIcon className="h-6 w-6 mb-2" />
              <span>System Alerts</span>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
};

export default AdminDashboard;