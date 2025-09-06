import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { CartProvider } from './contexts/CartContext';
import { NotificationProvider } from './contexts/NotificationContext';
import { IconThemeProvider } from './contexts/IconThemeContext';
import ProtectedRoute from './components/guards/ProtectedRoute';
import Header from './components/layout/Header';

// Page components (we'll create these)
import HomePage from './pages/HomePage';
import LoginPage from './pages/auth/LoginPage';
import RegisterPage from './pages/auth/RegisterPage';
import CustomerDashboard from './pages/customer/Dashboard';
import StoreDashboard from './pages/store/Dashboard';
import AdminDashboard from './pages/admin/Dashboard';
import NotFoundPage from './pages/NotFoundPage';

function App() {
  return (
    <Router>
      <AuthProvider>
        <IconThemeProvider>
          <NotificationProvider>
            <CartProvider>
              <div className="min-h-screen bg-slate-50">
                <Header />
              
              <main className="flex-1">
                <Routes>
              {/* Public routes */}
              <Route path="/" element={<HomePage />} />
              <Route path="/auth/login" element={<LoginPage />} />
              <Route path="/auth/register" element={<RegisterPage />} />
              
              {/* Customer routes */}
              <Route
                path="/customer/*"
                element={
                  <ProtectedRoute allowedRoles={['customer']}>
                    <Routes>
                      <Route path="dashboard" element={<CustomerDashboard />} />
                      <Route path="" element={<Navigate to="dashboard" replace />} />
                    </Routes>
                  </ProtectedRoute>
                }
              />
              
              {/* Store owner routes */}
              <Route
                path="/store/*"
                element={
                  <ProtectedRoute allowedRoles={['store_owner']}>
                    <Routes>
                      <Route path="dashboard" element={<StoreDashboard />} />
                      <Route path="" element={<Navigate to="dashboard" replace />} />
                    </Routes>
                  </ProtectedRoute>
                }
              />
              
              {/* Admin routes */}
              <Route
                path="/admin/*"
                element={
                  <ProtectedRoute allowedRoles={['admin']}>
                    <Routes>
                      <Route path="dashboard" element={<AdminDashboard />} />
                      <Route path="" element={<Navigate to="dashboard" replace />} />
                    </Routes>
                  </ProtectedRoute>
                }
              />
              
              {/* 404 page */}
              <Route path="*" element={<NotFoundPage />} />
            </Routes>
          </main>
        </div>
            </CartProvider>
          </NotificationProvider>
        </IconThemeProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
