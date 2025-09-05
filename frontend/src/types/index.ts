// User types
export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
  avatar?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export type UserRole = 'customer' | 'store_owner' | 'admin';

export interface Customer extends User {
  role: 'customer';
  shippingAddresses: Address[];
  preferredPaymentMethod?: PaymentMethod;
  orderHistory: Order[];
}

export interface StoreOwner extends User {
  role: 'store_owner';
  storeId: string;
  businessLicense?: string;
  verified: boolean;
}

export interface Admin extends User {
  role: 'admin';
  permissions: AdminPermission[];
}

export type AdminPermission = 
  | 'manage_users' 
  | 'manage_stores' 
  | 'manage_products' 
  | 'view_analytics' 
  | 'manage_system';

// Authentication types
export interface LoginCredentials {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterData {
  name: string;
  email: string;
  password: string;
  confirmPassword: string;
  role: UserRole;
}

export interface AuthResponse {
  user: User;
  token: string;
  refreshToken: string;
}

export interface AuthContextType {
  user: User | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (credentials: LoginCredentials) => Promise<void>;
  register: (data: RegisterData) => Promise<void>;
  logout: () => void;
  refreshToken: () => Promise<void>;
}

// Product types
export interface Product {
  id: string;
  name: string;
  description: string;
  price: number;
  originalPrice?: number;
  images: string[];
  category: string;
  subcategory?: string;
  storeId: string;
  storeName: string;
  inStock: boolean;
  stockQuantity: number;
  rating: number;
  reviewCount: number;
  tags: string[];
  specifications?: Record<string, string>;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

// Store types
export interface Store {
  id: string;
  name: string;
  description: string;
  logo?: string;
  banner?: string;
  ownerId: string;
  category: string;
  rating: number;
  reviewCount: number;
  isVerified: boolean;
  isActive: boolean;
  address: Address;
  contact: {
    phone: string;
    email: string;
    website?: string;
  };
  businessHours: BusinessHours[];
  createdAt: string;
  updatedAt: string;
}

export interface BusinessHours {
  day: string;
  open: string;
  close: string;
  isClosed: boolean;
}

// Order types
export interface Order {
  id: string;
  customerId: string;
  items: OrderItem[];
  status: OrderStatus;
  totalAmount: number;
  subtotal: number;
  taxAmount: number;
  shippingFee: number;
  discountAmount: number;
  shippingAddress: Address;
  billingAddress: Address;
  paymentMethod: PaymentMethod;
  trackingNumber?: string;
  estimatedDelivery?: string;
  createdAt: string;
  updatedAt: string;
}

export interface OrderItem {
  id: string;
  productId: string;
  productName: string;
  productImage: string;
  quantity: number;
  price: number;
  total: number;
}

export type OrderStatus = 
  | 'pending' 
  | 'confirmed' 
  | 'processing' 
  | 'shipped' 
  | 'delivered' 
  | 'cancelled' 
  | 'refunded';

// Address types
export interface Address {
  id?: string;
  name: string;
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
  isDefault?: boolean;
}

// Payment types
export interface PaymentMethod {
  id: string;
  type: 'credit_card' | 'debit_card' | 'paypal' | 'bank_transfer';
  lastFourDigits?: string;
  expiryMonth?: number;
  expiryYear?: number;
  cardholderName?: string;
  isDefault: boolean;
}

// Cart types
export interface CartItem {
  id: string;
  productId: string;
  product: Product;
  quantity: number;
  selectedOptions?: Record<string, string>;
}

export interface Cart {
  items: CartItem[];
  subtotal: number;
  taxAmount: number;
  shippingFee: number;
  total: number;
}

// API response types
export interface ApiResponse<T = any> {
  success: boolean;
  data: T;
  message?: string;
  errors?: string[];
}

export interface PaginatedResponse<T> {
  data: T[];
  pagination: {
    page: number;
    limit: number;
    total: number;
    totalPages: number;
  };
}

// Common component types
export interface SelectOption {
  value: string;
  label: string;
  disabled?: boolean;
}

export interface TableColumn<T = any> {
  key: keyof T | string;
  title: string;
  sortable?: boolean;
  render?: (value: any, record: T, index: number) => React.ReactNode;
}

// Notification types
export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number;
  actions?: NotificationAction[];
}

export interface NotificationAction {
  label: string;
  action: () => void;
}

// Theme types
export interface ThemeContextType {
  isDark: boolean;
  toggleTheme: () => void;
}

// WebSocket message types
export interface WebSocketMessage {
  type: string;
  data: any;
  timestamp: string;
}