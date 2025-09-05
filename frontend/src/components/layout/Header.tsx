import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../ui/Button';
import Input from '../ui/Input';
import ThemedIcon from '../ui/ThemedIcon';

interface HeaderProps {
  onMobileMenuToggle?: () => void;
}

const Header: React.FC<HeaderProps> = ({ onMobileMenuToggle }) => {
  const { user, isAuthenticated, logout } = useAuth();
  const [searchQuery, setSearchQuery] = useState('');
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const location = useLocation();
  const navigate = useNavigate();

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      navigate(`/products?search=${encodeURIComponent(searchQuery)}`);
    }
  };

  const handleLogout = async () => {
    await logout();
    setIsUserMenuOpen(false);
    navigate('/');
  };

  const getUserDashboard = () => {
    if (!user) return '/';
    switch (user.role) {
      case 'customer':
        return '/customer/dashboard';
      case 'store_owner':
        return '/store/dashboard';
      case 'admin':
        return '/admin/dashboard';
      default:
        return '/';
    }
  };

  const isPublicPage = !location.pathname.startsWith('/customer') && 
                     !location.pathname.startsWith('/store') && 
                     !location.pathname.startsWith('/admin');

  return (
    <header className="bg-white shadow-md border-b border-gray-200 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Left section - Logo and mobile menu button */}
          <div className="flex items-center">
            {onMobileMenuToggle && (
              <button
                onClick={onMobileMenuToggle}
                className="p-2 rounded-md text-gray-400 hover:text-gray-500 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-ocean-500 lg:hidden"
                aria-label="Open menu"
              >
                <ThemedIcon name="menu" size="lg" color="current" />
              </button>
            )}
            
            <Link to="/" className="flex items-center ml-2 lg:ml-0">
              <div className="flex items-center space-x-2">
                <div className="w-10 h-10 bg-ocean-gradient rounded-lg flex items-center justify-center">
                  <span className="text-white font-bold text-xl">🌊</span>
                </div>
                <span className="text-xl font-bold text-gray-900 hidden sm:block">
                  Ocean Shopping
                </span>
              </div>
            </Link>
          </div>

          {/* Center section - Search (only on public pages) */}
          {isPublicPage && (
            <div className="flex-1 max-w-lg mx-4">
              <form onSubmit={handleSearch}>
                <Input
                  type="text"
                  placeholder="Search products..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  leftIcon={<ThemedIcon name="search" size="md" semantic="muted" />}
                  className="w-full"
                />
              </form>
            </div>
          )}

          {/* Right section - Cart and user menu */}
          <div className="flex items-center space-x-4">
            {/* Shopping Cart (only for customers on public pages) */}
            {isPublicPage && (
              <Link 
                to="/cart"
                className="relative p-2 text-gray-400 hover:text-gray-500 focus:outline-none focus:ring-2 focus:ring-ocean-500 rounded-md"
                aria-label="Shopping cart"
              >
                <ThemedIcon name="shopping-cart" size="lg" color="current" />
                {/* Cart badge - you can connect this to a cart context */}
                <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs rounded-full h-5 w-5 flex items-center justify-center">
                  0
                </span>
              </Link>
            )}

            {/* User menu */}
            {isAuthenticated ? (
              <div className="relative">
                <button
                  onClick={() => setIsUserMenuOpen(!isUserMenuOpen)}
                  className="flex items-center space-x-2 p-2 rounded-md text-gray-700 hover:text-gray-900 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-ocean-500"
                >
                  <div className="w-8 h-8 bg-ocean-500 rounded-full flex items-center justify-center">
                    {user?.avatar ? (
                      <img 
                        src={user.avatar} 
                        alt={user.name}
                        className="w-8 h-8 rounded-full object-cover"
                      />
                    ) : (
                      <ThemedIcon name="user" size="md" color="white" />
                    )}
                  </div>
                  <span className="hidden md:block text-sm font-medium">
                    {user?.name}
                  </span>
                  <ThemedIcon name="chevron-down" size="sm" />
                </button>

                {/* Dropdown menu */}
                {isUserMenuOpen && (
                  <div className="absolute right-0 mt-2 w-48 bg-white rounded-md shadow-lg border border-gray-200 py-1 z-50">
                    <Link
                      to={getUserDashboard()}
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      onClick={() => setIsUserMenuOpen(false)}
                    >
                      Dashboard
                    </Link>
                    <Link
                      to="/profile"
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      onClick={() => setIsUserMenuOpen(false)}
                    >
                      Profile
                    </Link>
                    <Link
                      to="/settings"
                      className="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      onClick={() => setIsUserMenuOpen(false)}
                    >
                      Settings
                    </Link>
                    <hr className="my-1" />
                    <button
                      onClick={handleLogout}
                      className="w-full text-left px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      Sign out
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <div className="flex items-center space-x-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => navigate('/auth/login')}
                >
                  Sign in
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => navigate('/auth/register')}
                >
                  Sign up
                </Button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Overlay for mobile menu */}
      {isUserMenuOpen && (
        <div 
          className="fixed inset-0 z-10" 
          onClick={() => setIsUserMenuOpen(false)}
        />
      )}
    </header>
  );
};

export default Header;