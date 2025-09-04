import React from 'react';
import { Link } from 'react-router-dom';
import { 
  ShoppingBagIcon,
  TruckIcon,
  ShieldCheckIcon,
  GlobeAltIcon 
} from '@heroicons/react/24/outline';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';

const HomePage: React.FC = () => {
  const features = [
    {
      icon: <ShoppingBagIcon className="h-8 w-8 text-ocean-500" />,
      title: 'Wide Product Selection',
      description: 'Discover thousands of products from verified stores across all categories.'
    },
    {
      icon: <TruckIcon className="h-8 w-8 text-teal-500" />,
      title: 'Fast Delivery',
      description: 'Get your orders delivered quickly with our reliable shipping network.'
    },
    {
      icon: <ShieldCheckIcon className="h-8 w-8 text-aqua-500" />,
      title: 'Secure Shopping',
      description: 'Shop with confidence knowing your data and transactions are protected.'
    },
    {
      icon: <GlobeAltIcon className="h-8 w-8 text-ocean-600" />,
      title: 'Global Marketplace',
      description: 'Connect with stores from around the world in one unified platform.'
    },
  ];

  return (
    <div className="min-h-screen">
      {/* Hero Section */}
      <section className="wave-animation py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h1 className="text-4xl md:text-6xl font-bold text-white mb-6">
            Dive into the
            <span className="block text-aqua-100">Ocean of Shopping</span>
          </h1>
          <p className="text-xl text-white/90 mb-8 max-w-2xl mx-auto">
            Discover amazing products from trusted stores worldwide. 
            Whether you're a customer, store owner, or administrator, 
            we've got the perfect experience for you.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button size="lg" variant="secondary">
              <Link to="/products">Start Shopping</Link>
            </Button>
            <Button size="lg" variant="outline" className="border-white text-white hover:bg-white hover:text-ocean-600">
              <Link to="/auth/register">Join as Seller</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-16 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-900 mb-4">
              Why Choose Ocean Shopping?
            </h2>
            <p className="text-lg text-gray-600 max-w-2xl mx-auto">
              Experience the future of e-commerce with our innovative platform 
              designed for customers, store owners, and administrators.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            {features.map((feature, index) => (
              <Card key={index} hover className="text-center">
                <div className="flex justify-center mb-4">
                  {feature.icon}
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-3">
                  {feature.title}
                </h3>
                <p className="text-gray-600">
                  {feature.description}
                </p>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-16 bg-gradient-to-br from-ocean-50 to-teal-50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl font-bold text-gray-900 mb-4">
            Ready to Get Started?
          </h2>
          <p className="text-lg text-gray-600 mb-8 max-w-2xl mx-auto">
            Join thousands of satisfied customers and successful store owners 
            in our growing marketplace community.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <Button size="lg">
              <Link to="/auth/register">Create Account</Link>
            </Button>
            <Button size="lg" variant="outline">
              <Link to="/auth/login">Sign In</Link>
            </Button>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-12">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
            <div className="col-span-1 md:col-span-2">
              <div className="flex items-center space-x-2 mb-4">
                <div className="w-10 h-10 bg-ocean-gradient rounded-lg flex items-center justify-center">
                  <span className="text-white font-bold text-xl">🌊</span>
                </div>
                <span className="text-xl font-bold">Ocean Shopping</span>
              </div>
              <p className="text-gray-400 max-w-md">
                Your premier destination for online shopping. 
                Connecting customers with quality products and trusted sellers worldwide.
              </p>
            </div>
            
            <div>
              <h3 className="font-semibold mb-4">Quick Links</h3>
              <ul className="space-y-2 text-gray-400">
                <li><Link to="/products" className="hover:text-white">Products</Link></li>
                <li><Link to="/stores" className="hover:text-white">Stores</Link></li>
                <li><Link to="/about" className="hover:text-white">About</Link></li>
                <li><Link to="/contact" className="hover:text-white">Contact</Link></li>
              </ul>
            </div>
            
            <div>
              <h3 className="font-semibold mb-4">Support</h3>
              <ul className="space-y-2 text-gray-400">
                <li><Link to="/help" className="hover:text-white">Help Center</Link></li>
                <li><Link to="/shipping" className="hover:text-white">Shipping Info</Link></li>
                <li><Link to="/returns" className="hover:text-white">Returns</Link></li>
                <li><Link to="/privacy" className="hover:text-white">Privacy Policy</Link></li>
              </ul>
            </div>
          </div>
          
          <div className="border-t border-gray-800 mt-8 pt-8 text-center text-gray-400">
            <p>&copy; 2024 Ocean Shopping Center. All rights reserved.</p>
          </div>
        </div>
      </footer>
    </div>
  );
};

export default HomePage;