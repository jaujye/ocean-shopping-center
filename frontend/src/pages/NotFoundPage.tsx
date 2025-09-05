import React from 'react';
import { Link } from 'react-router-dom';
import Button from '../components/ui/Button';
import Card from '../components/ui/Card';

const NotFoundPage: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-gradient-to-br from-ocean-50 to-teal-50">
      <div className="max-w-md w-full text-center">
        <Card className="shadow-xl">
          <div className="p-8">
            <div className="mb-6">
              <div className="w-24 h-24 bg-ocean-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-4xl">🌊</span>
              </div>
              <h1 className="text-9xl font-bold text-ocean-500 mb-4">404</h1>
            </div>
            
            <h2 className="text-2xl font-bold text-gray-900 mb-4">
              Lost in the Ocean?
            </h2>
            
            <p className="text-gray-600 mb-8">
              The page you're looking for has drifted away like a message in a bottle. 
              Let's help you navigate back to shore.
            </p>
            
            <div className="space-y-4">
              <Button asChild size="lg" className="w-full">
                <Link to="/">
                  Return to Home
                </Link>
              </Button>
              
              <Button asChild variant="outline" size="lg" className="w-full">
                <Link to="/products">
                  Browse Products
                </Link>
              </Button>
            </div>
          </div>
        </Card>

        <div className="mt-8 text-center">
          <p className="text-sm text-gray-500">
            Need help? <Link to="/contact" className="text-ocean-600 hover:text-ocean-500">Contact our support team</Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default NotFoundPage;