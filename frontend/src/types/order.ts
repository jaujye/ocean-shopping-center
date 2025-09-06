// Re-export all order-related types from orderService for convenience
export type {
  OrderStatus,
  Address,
  CheckoutRequest,
  OrderItem,
  OrderSummary,
  Order,
  CheckoutResponse,
  PaginatedResponse,
  OrderStatusUpdateRequest
} from '../services/orderService';