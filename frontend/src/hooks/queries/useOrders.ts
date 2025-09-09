import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import orderService from '../../services/orderService';

// Query Keys
export const orderKeys = {
  all: ['orders'] as const,
  lists: () => [...orderKeys.all, 'list'] as const,
  list: (params?: any) => [...orderKeys.lists(), params] as const,
  details: () => [...orderKeys.all, 'detail'] as const,
  detail: (orderId: string) => [...orderKeys.details(), orderId] as const,
  stats: () => [...orderKeys.all, 'stats'] as const,
  history: (params?: any) => [...orderKeys.all, 'history', params] as const,
  search: (query: string) => [...orderKeys.all, 'search', query] as const,
  tracking: (orderId: string) => [...orderKeys.all, 'tracking', orderId] as const,
};

// Fetch user orders with filters and pagination (matches /api/orders)
export const useUserOrders = (params?: {
  page?: number;
  size?: number;
  status?: any; // OrderStatus enum
}) => {
  return useQuery({
    queryKey: orderKeys.list({ type: 'user', ...params }),
    queryFn: () => orderService.getUserOrders(params?.page, params?.size, params?.status),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Fetch store orders with filters and pagination
export const useStoreOrders = (storeId: number, params?: {
  page?: number;
  limit?: number;
  status?: any; // OrderStatus enum
  startDate?: string;
  endDate?: string;
}) => {
  return useQuery({
    queryKey: orderKeys.list({ type: 'store', storeId, ...params }),
    queryFn: () => orderService.getStoreOrders(
      storeId,
      params?.page,
      params?.limit,
      params?.status,
      params?.startDate,
      params?.endDate
    ),
    enabled: !!storeId,
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Fetch all orders (admin) with filters and pagination
export const useAllOrders = (params?: {
  page?: number;
  limit?: number;
  status?: any; // OrderStatus enum
  startDate?: string;
  endDate?: string;
  storeId?: number;
}) => {
  return useQuery({
    queryKey: orderKeys.list({ type: 'all', ...params }),
    queryFn: () => orderService.getAllOrders(
      params?.page,
      params?.limit,
      params?.status,
      params?.startDate,
      params?.endDate,
      params?.storeId
    ),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Fetch single order (matches /api/orders/{orderId})
export const useOrder = (orderId: number) => {
  return useQuery({
    queryKey: orderKeys.detail(orderId.toString()),
    queryFn: () => orderService.getOrder(orderId),
    enabled: !!orderId,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Fetch order statistics (matches /api/orders/statistics)
export const useOrderStats = () => {
  return useQuery({
    queryKey: orderKeys.stats(),
    queryFn: () => orderService.getUserOrderStatistics(),
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Create order mutation (using checkout)
export const useCreateOrder = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (checkoutData: any) => orderService.processCheckout(checkoutData),
    onSuccess: () => {
      // Invalidate order lists
      queryClient.invalidateQueries({ queryKey: orderKeys.lists() });
      queryClient.invalidateQueries({ queryKey: orderKeys.stats() });
      // Also invalidate cart since order creation typically empties the cart
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });
};

// Update order status mutation (for store orders)
export const useUpdateOrderStatus = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ 
      storeId, 
      orderId, 
      statusUpdate 
    }: { 
      storeId: number; 
      orderId: number; 
      statusUpdate: any // OrderStatusUpdateRequest
    }) => orderService.updateOrderStatus(storeId, orderId, statusUpdate),
    onSuccess: (_, { orderId }) => {
      // Invalidate the specific order and lists
      queryClient.invalidateQueries({ queryKey: orderKeys.detail(orderId.toString()) });
      queryClient.invalidateQueries({ queryKey: orderKeys.lists() });
      queryClient.invalidateQueries({ queryKey: orderKeys.stats() });
    },
  });
};

// Cancel order mutation (matches /api/orders/{orderId}/cancel)
export const useCancelOrder = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ orderId, reason }: { orderId: number; reason?: string }) => 
      orderService.cancelOrder(orderId, reason),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: orderKeys.detail(orderId.toString()) });
      queryClient.invalidateQueries({ queryKey: orderKeys.lists() });
      queryClient.invalidateQueries({ queryKey: orderKeys.stats() });
    },
  });
};

// Process refund mutation (for store orders)
export const useProcessRefund = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: ({ 
      storeId, 
      orderId, 
      amount, 
      reason 
    }: { 
      storeId: number; 
      orderId: number; 
      amount: number; 
      reason: string 
    }) => orderService.processRefund(storeId, orderId, amount, reason),
    onSuccess: (_, { orderId }) => {
      queryClient.invalidateQueries({ queryKey: orderKeys.detail(orderId.toString()) });
      queryClient.invalidateQueries({ queryKey: orderKeys.lists() });
      queryClient.invalidateQueries({ queryKey: orderKeys.stats() });
    },
  });
};

// Additional hooks to match backend API endpoints

// Get order by order number (matches /api/orders/number/{orderNumber})
export const useOrderByNumber = (orderNumber: string) => {
  return useQuery({
    queryKey: orderKeys.detail(`number-${orderNumber}`),
    queryFn: () => orderService.getOrderByNumber(orderNumber),
    enabled: !!orderNumber,
    staleTime: 1000 * 60 * 5, // 5 minutes
  });
};

// Get order history with filtering (matches /api/orders/history)
export const useOrderHistory = (params?: {
  page?: number;
  size?: number;
  status?: any;
  year?: number;
  month?: number;
}) => {
  return useQuery({
    queryKey: orderKeys.history(params),
    queryFn: () => orderService.getOrderHistory(params?.page, params?.size, params?.status, params?.year, params?.month),
    staleTime: 1000 * 60 * 2, // 2 minutes
  });
};

// Search orders (matches /api/orders/search)
export const useOrderSearch = (query: string, params?: {
  page?: number;
  size?: number;
}) => {
  return useQuery({
    queryKey: orderKeys.search(query),
    queryFn: () => orderService.searchOrders(query, params?.page, params?.size),
    enabled: !!query && query.length > 2,
    staleTime: 1000 * 60 * 1, // 1 minute
  });
};

// Get order tracking (matches /api/orders/{orderId}/tracking)
export const useOrderTracking = (orderId: number) => {
  return useQuery({
    queryKey: orderKeys.tracking(orderId.toString()),
    queryFn: () => orderService.getOrderTracking(orderId),
    enabled: !!orderId,
    staleTime: 1000 * 60 * 2, // 2 minutes
    refetchInterval: 1000 * 60 * 5, // Auto refresh every 5 minutes for tracking updates
  });
};

// Request invoice mutation (matches /api/orders/{orderId}/invoice)
export const useRequestInvoice = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (orderId: number) => orderService.requestInvoice(orderId),
    onSuccess: (_, orderId) => {
      queryClient.invalidateQueries({ queryKey: orderKeys.detail(orderId.toString()) });
    },
  });
};

// Reorder mutation (matches /api/orders/{orderId}/reorder)
export const useReorderItems = () => {
  const queryClient = useQueryClient();
  
  return useMutation({
    mutationFn: (orderId: number) => orderService.reorderItems(orderId),
    onSuccess: () => {
      // Invalidate cart queries since items were added to cart
      queryClient.invalidateQueries({ queryKey: ['cart'] });
    },
  });
};