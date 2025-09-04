import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { 
  UserIcon, 
  EnvelopeIcon, 
  LockClosedIcon, 
  EyeIcon, 
  EyeSlashIcon,
  BuildingStorefrontIcon,
  ShieldCheckIcon
} from '@heroicons/react/24/outline';
import { useAuth } from '../../contexts/AuthContext';
import { UserRole } from '../../types';
import Button from '../../components/ui/Button';
import Input from '../../components/ui/Input';
import Card, { CardContent, CardHeader, CardTitle } from '../../components/ui/Card';

interface RegisterForm {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
  role: UserRole;
}

const RegisterPage: React.FC = () => {
  const { register } = useAuth();
  const navigate = useNavigate();
  
  const [form, setForm] = useState<RegisterForm>({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    role: 'customer',
  });
  
  const [errors, setErrors] = useState<Partial<RegisterForm>>({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [generalError, setGeneralError] = useState('');

  const roleOptions = [
    {
      value: 'customer' as UserRole,
      label: 'Customer',
      description: 'Browse and purchase products from various stores',
      icon: <UserIcon className="h-6 w-6" />,
    },
    {
      value: 'store_owner' as UserRole,
      label: 'Store Owner',
      description: 'Manage your own store and sell products',
      icon: <BuildingStorefrontIcon className="h-6 w-6" />,
    },
    {
      value: 'admin' as UserRole,
      label: 'Administrator',
      description: 'Manage the platform and oversee operations',
      icon: <ShieldCheckIcon className="h-6 w-6" />,
    },
  ];

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setForm(prev => ({
      ...prev,
      [name]: value
    }));
    
    // Clear errors when user starts typing
    if (errors[name as keyof RegisterForm]) {
      setErrors(prev => ({
        ...prev,
        [name]: undefined
      }));
    }
    setGeneralError('');
  };

  const handleRoleChange = (role: UserRole) => {
    setForm(prev => ({
      ...prev,
      role
    }));
  };

  const validateForm = (): boolean => {
    const newErrors: Partial<RegisterForm> = {};

    if (!form.name.trim()) {
      newErrors.name = 'Name is required';
    } else if (form.name.trim().length < 2) {
      newErrors.name = 'Name must be at least 2 characters';
    }

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

    if (!form.confirmPassword) {
      newErrors.confirmPassword = 'Please confirm your password';
    } else if (form.password !== form.confirmPassword) {
      newErrors.confirmPassword = 'Passwords do not match';
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
      await register({
        name: form.name.trim(),
        email: form.email,
        password: form.password,
        confirmPassword: form.confirmPassword,
        role: form.role,
      });
      
      navigate('/');
    } catch (error: any) {
      setGeneralError(
        error?.response?.data?.message || 
        error?.message || 
        'Failed to create account. Please try again.'
      );
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8 bg-gradient-to-br from-ocean-50 to-teal-50">
      <div className="max-w-2xl w-full">
        <Card className="shadow-xl">
          <CardHeader className="text-center">
            <div className="flex justify-center mb-4">
              <div className="w-16 h-16 bg-ocean-gradient rounded-full flex items-center justify-center">
                <span className="text-white font-bold text-2xl">🌊</span>
              </div>
            </div>
            <CardTitle size="xl">Join Ocean Shopping</CardTitle>
            <p className="text-gray-600 mt-2">
              Create your account and start your shopping journey
            </p>
          </CardHeader>

          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {generalError && (
                <div className="bg-red-50 border border-red-200 text-red-600 px-4 py-3 rounded-lg text-sm">
                  {generalError}
                </div>
              )}

              {/* Role Selection */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-3">
                  I want to join as
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                  {roleOptions.map((option) => (
                    <button
                      key={option.value}
                      type="button"
                      onClick={() => handleRoleChange(option.value)}
                      className={`p-4 border rounded-lg text-left transition-all ${
                        form.role === option.value
                          ? 'border-ocean-500 bg-ocean-50 ring-2 ring-ocean-500'
                          : 'border-gray-200 hover:border-gray-300'
                      }`}
                    >
                      <div className="flex items-center mb-2">
                        <div className={`${form.role === option.value ? 'text-ocean-500' : 'text-gray-400'}`}>
                          {option.icon}
                        </div>
                        <span className={`ml-2 font-medium ${form.role === option.value ? 'text-ocean-700' : 'text-gray-900'}`}>
                          {option.label}
                        </span>
                      </div>
                      <p className="text-xs text-gray-500">
                        {option.description}
                      </p>
                    </button>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Full Name"
                  type="text"
                  name="name"
                  value={form.name}
                  onChange={handleChange}
                  error={errors.name}
                  leftIcon={<UserIcon />}
                  placeholder="Enter your full name"
                  autoComplete="name"
                  required
                  fullWidth
                />

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
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
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
                  placeholder="Create a password"
                  autoComplete="new-password"
                  required
                  fullWidth
                />

                <Input
                  label="Confirm Password"
                  type={showConfirmPassword ? 'text' : 'password'}
                  name="confirmPassword"
                  value={form.confirmPassword}
                  onChange={handleChange}
                  error={errors.confirmPassword}
                  leftIcon={<LockClosedIcon />}
                  rightIcon={
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="text-gray-400 hover:text-gray-600 focus:outline-none"
                    >
                      {showConfirmPassword ? <EyeSlashIcon className="h-5 w-5" /> : <EyeIcon className="h-5 w-5" />}
                    </button>
                  }
                  placeholder="Confirm your password"
                  autoComplete="new-password"
                  required
                  fullWidth
                />
              </div>

              <div className="text-xs text-gray-500">
                By creating an account, you agree to our{' '}
                <Link to="/terms" className="text-ocean-600 hover:text-ocean-500">
                  Terms of Service
                </Link>{' '}
                and{' '}
                <Link to="/privacy" className="text-ocean-600 hover:text-ocean-500">
                  Privacy Policy
                </Link>
                .
              </div>

              <Button
                type="submit"
                loading={isLoading}
                loadingText="Creating account..."
                fullWidth
                size="lg"
              >
                Create Account
              </Button>

              <div className="text-center">
                <span className="text-gray-600 text-sm">
                  Already have an account?{' '}
                  <Link 
                    to="/auth/login" 
                    className="text-ocean-600 hover:text-ocean-500 font-medium"
                  >
                    Sign in
                  </Link>
                </span>
              </div>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default RegisterPage;