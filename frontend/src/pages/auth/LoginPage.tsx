import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { EnvelopeIcon, LockClosedIcon, EyeIcon, EyeSlashIcon } from '@heroicons/react/24/outline';
import { useAuth } from '../../contexts/AuthContext';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import Card, { CardContent, CardHeader, CardTitle } from '../../components/ui/Card';

interface LoginForm {
  email: string;
  password: string;
  rememberMe: boolean;
}

const LoginPage: React.FC = () => {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  
  const [form, setForm] = useState<LoginForm>({
    email: '',
    password: '',
    rememberMe: false,
  });
  
  const [errors, setErrors] = useState<Partial<LoginForm>>({});
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [generalError, setGeneralError] = useState('');

  // Get redirect path from location state or default to home
  const from = (location.state as any)?.from || '/';

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }));
    
    // Clear errors when user starts typing
    if (errors[name as keyof LoginForm]) {
      setErrors(prev => ({
        ...prev,
        [name]: undefined
      }));
    }
    setGeneralError('');
  };

  const validateForm = (): boolean => {
    const newErrors: Partial<LoginForm> = {};

    if (!form.email) {
      newErrors.email = 'Email is required';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = 'Please enter a valid email address';
    }

    if (!form.password) {
      newErrors.password = 'Password is required';
    } else if (form.password.length < 6) {
      newErrors.password = 'Password must be at least 6 characters';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!validateForm()) {
      return;
    }

    setIsLoading(true);
    setGeneralError('');

    try {
      await login({
        email: form.email,
        password: form.password,
        rememberMe: form.rememberMe,
      });
      
      navigate(from, { replace: true });
    } catch (error: any) {
      setGeneralError(
        error?.response?.data?.message || 
        error?.message || 
        'Failed to sign in. Please check your credentials and try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-gradient-to-br from-ocean-50 to-teal-50">
      <div className="max-w-md w-full">
        <Card className="shadow-xl">
          <CardHeader className="text-center">
            <div className="flex justify-center mb-4">
              <div className="w-16 h-16 bg-ocean-gradient rounded-full flex items-center justify-center">
                <span className="text-white font-bold text-2xl">🌊</span>
              </div>
            </div>
            <CardTitle size="xl">Welcome Back</CardTitle>
            <p className="text-gray-600 mt-2">
              Sign in to your Ocean Shopping account
            </p>
          </CardHeader>

          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {generalError && (
                <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg text-sm">
                  {generalError}
                </div>
              )}

              <Input
                label="Email"
                type="email"
                name="email"
                value={form.email}
                onChange={handleChange}
                error={errors.email}
                leftIcon={<EnvelopeIcon />}
                placeholder="Enter your email"
                autoComplete="email"
                required
                fullWidth
              />

              <Input
                label="Password"
                type={showPassword ? 'text' : 'password'}
                name="password"
                value={form.password}
                onChange={handleChange}
                error={errors.password}
                leftIcon={<LockClosedIcon />}
                rightIcon={
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="text-gray-400 hover:text-gray-600 focus:outline-none"
                  >
                    {showPassword ? <EyeSlashIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
                  </button>
                }
                placeholder="Enter your password"
                autoComplete="current-password"
                required
                fullWidth
              />

              <div className="flex items-center justify-between">
                <div className="flex items-center">
                  <input
                    id="rememberMe"
                    name="rememberMe"
                    type="checkbox"
                    checked={form.rememberMe}
                    onChange={handleChange}
                    className="h-4 w-4 text-ocean-600 focus:ring-ocean-500 border-gray-300 rounded"
                  />
                  <label htmlFor="rememberMe" className="ml-2 block text-sm text-gray-900">
                    Remember me
                  </label>
                </div>

                <Link 
                  to="/auth/forgot-password" 
                  className="text-sm text-ocean-600 hover:text-ocean-500"
                >
                  Forgot password?
                </Link>
              </div>

              <Button
                type="submit"
                loading={isLoading}
                loadingText="Signing in..."
                fullWidth
                size="lg"
              >
                Sign in
              </Button>

              <div className="text-center">
                <span className="text-gray-600 text-sm">
                  Don't have an account?{' '}
                  <Link 
                    to="/auth/register" 
                    className="text-ocean-600 hover:text-ocean-500 font-medium"
                  >
                    Sign up
                  </Link>
                </span>
              </div>
            </form>
          </CardContent>
        </Card>

        {/* Demo credentials */}
        <Card className="mt-4 bg-blue-50 border-blue-200">
          <CardContent className="text-center">
            <p className="text-sm text-blue-800 font-medium mb-2">Demo Accounts</p>
            <div className="space-y-1 text-xs text-blue-700">
              <p>Customer: customer@demo.com / password</p>
              <p>Store Owner: store@demo.com / password</p>
              <p>Admin: admin@demo.com / password</p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default LoginPage;