import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { 
  ApiResponse, 
  AuthResponse, 
  LoginCredentials, 
  RegisterData, 
  User,
  Product,
  Store,
  Order,
  PaginatedResponse
} from '../types';

class ApiClient {
  private client: AxiosInstance;
  private token: string | null = null;

  constructor() {
    this.client = axios.create({
      baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8000/api',
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    // Request interceptor to add auth token
    this.client.interceptors.request.use(
      (config) => {
        if (this.token) {
          config.headers.Authorization = `Bearer ${this.token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for error handling
    this.client.interceptors.response.use(
      (response: AxiosResponse) => response,
      async (error) => {
        const originalRequest = error.config;

        if (error.response?.status === 401 && !originalRequest._retry) {
          originalRequest._retry = true;
          
          try {
            await this.refreshToken();
            return this.client(originalRequest);
          } catch (refreshError) {
            this.logout();
            window.location.href = '/auth/login';
            return Promise.reject(refreshError);
          }
        }

        return Promise.reject(error);
      }
    );
  }

  // Auth methods
  setToken(token: string) {
    this.token = token;
    localStorage.setItem('access_token', token);
  }

  getToken(): string | null {
    if (!this.token) {
      this.token = localStorage.getItem('access_token');
    }
    return this.token;
  }

  clearToken() {
    this.token = null;
    localStorage.removeItem('access_token');
    localStorage.removeItem('refresh_token');
  }

  async login(credentials: LoginCredentials): Promise<AuthResponse> {
    const response = await this.client.post<ApiResponse<AuthResponse>>('/auth/login', credentials);
    const authData = response.data.data;
    
    this.setToken(authData.token);
    localStorage.setItem('refresh_token', authData.refreshToken);
    
    return authData;
  }

  async register(data: RegisterData): Promise<AuthResponse> {
    const response = await this.client.post<ApiResponse<AuthResponse>>('/auth/register', data);
    const authData = response.data.data;
    
    this.setToken(authData.token);
    localStorage.setItem('refresh_token', authData.refreshToken);
    
    return authData;
  }

  async refreshToken(): Promise<string> {
    const refreshToken = localStorage.getItem('refresh_token');
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await this.client.post<ApiResponse<{ token: string }>>('/auth/refresh', {
      refreshToken
    });
    
    const newToken = response.data.data.token;
    this.setToken(newToken);
    
    return newToken;
  }

  async logout(): Promise<void> {
    try {
      await this.client.post('/auth/logout');
    } catch (error) {
      // Continue with logout even if API call fails
      console.warn('Logout API call failed:', error);
    } finally {
      this.clearToken();
    }
  }

  async getCurrentUser(): Promise<User> {
    const response = await this.client.get<ApiResponse<User>>('/auth/me');
    return response.data.data;
  }

  // Product methods
  async getProducts(params?: {
    page?: number;
    limit?: number;
    search?: string;
    category?: string;
    storeId?: string;
    minPrice?: number;
    maxPrice?: number;
    sortBy?: string;
    sortOrder?: 'asc' | 'desc';
  }): Promise<PaginatedResponse<Product>> {
    const response = await this.client.get<ApiResponse<PaginatedResponse<Product>>>('/products', {
      params
    });
    return response.data.data;
  }

  async getProduct(id: string): Promise<Product> {
    const response = await this.client.get<ApiResponse<Product>>(`/products/${id}`);
    return response.data.data;
  }

  async createProduct(product: Omit<Product, 'id' | 'createdAt' | 'updatedAt'>): Promise<Product> {
    const response = await this.client.post<ApiResponse<Product>>('/products', product);
    return response.data.data;
  }

  async updateProduct(id: string, product: Partial<Product>): Promise<Product> {
    const response = await this.client.patch<ApiResponse<Product>>(`/products/${id}`, product);
    return response.data.data;
  }

  async deleteProduct(id: string): Promise<void> {
    await this.client.delete(`/products/${id}`);
  }

  // Store methods
  async getStores(params?: {
    page?: number;
    limit?: number;
    search?: string;
    category?: string;
    verified?: boolean;
  }): Promise<PaginatedResponse<Store>> {
    const response = await this.client.get<ApiResponse<PaginatedResponse<Store>>>('/stores', {
      params
    });
    return response.data.data;
  }

  async getStore(id: string): Promise<Store> {
    const response = await this.client.get<ApiResponse<Store>>(`/stores/${id}`);
    return response.data.data;
  }

  async createStore(store: Omit<Store, 'id' | 'createdAt' | 'updatedAt'>): Promise<Store> {
    const response = await this.client.post<ApiResponse<Store>>('/stores', store);
    return response.data.data;
  }

  async updateStore(id: string, store: Partial<Store>): Promise<Store> {
    const response = await this.client.patch<ApiResponse<Store>>(`/stores/${id}`, store);
    return response.data.data;
  }

  // Order methods
  async getOrders(params?: {
    page?: number;
    limit?: number;
    status?: string;
    customerId?: string;
    storeId?: string;
  }): Promise<PaginatedResponse<Order>> {
    const response = await this.client.get<ApiResponse<PaginatedResponse<Order>>>('/orders', {
      params
    });
    return response.data.data;
  }

  async getOrder(id: string): Promise<Order> {
    const response = await this.client.get<ApiResponse<Order>>(`/orders/${id}`);
    return response.data.data;
  }

  async createOrder(order: Omit<Order, 'id' | 'createdAt' | 'updatedAt'>): Promise<Order> {
    const response = await this.client.post<ApiResponse<Order>>('/orders', order);
    return response.data.data;
  }

  async updateOrderStatus(id: string, status: string): Promise<Order> {
    const response = await this.client.patch<ApiResponse<Order>>(`/orders/${id}/status`, { status });
    return response.data.data;
  }

  // File upload method
  async uploadFile(file: File, path: string = 'general'): Promise<string> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('path', path);

    const response = await this.client.post<ApiResponse<{ url: string }>>('/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });

    return response.data.data.url;
  }

  // Generic request method for custom endpoints
  async request<T = any>(config: AxiosRequestConfig): Promise<T> {
    const response = await this.client.request<ApiResponse<T>>(config);
    return response.data.data;
  }
}

// Create and export a singleton instance
export const apiClient = new ApiClient();

// Initialize token from localStorage on app start
const storedToken = localStorage.getItem('access_token');
if (storedToken) {
  apiClient.setToken(storedToken);
}

export default apiClient;