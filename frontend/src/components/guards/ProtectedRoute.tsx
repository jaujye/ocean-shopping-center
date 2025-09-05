import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../contexts/AuthContext';
import { UserRole } from '../../types';
import LoadingSpinner from '../ui/LoadingSpinner';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles?: UserRole[];
  requireAuth?: boolean;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  allowedRoles,
  requireAuth = true 
}) => {
  const { user, isLoading, isAuthenticated } = useAuth();
  const location = useLocation();

  // Show loading spinner while checking authentication
  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  // If authentication is required but user is not authenticated
  if (requireAuth && !isAuthenticated) {
    return (
      <Navigate 
        to="/auth/login" 
        state={{ from: location.pathname }} 
        replace 
      />
    );
  }

  // If specific roles are required, check if user has the right role
  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    // Redirect to appropriate dashboard based on user role
    const redirectPath = getRoleDashboard(user.role);
    return <Navigate to={redirectPath} replace />;
  }

  return <>{children}</>;
};

// Helper function to get dashboard path based on user role
const getRoleDashboard = (role: UserRole): string => {
  switch (role) {
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

export default ProtectedRoute;