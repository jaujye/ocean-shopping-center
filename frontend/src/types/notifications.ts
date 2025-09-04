// Notification system types for real-time updates

export interface SystemNotification {
  id: string;
  type: NotificationType;
  category: NotificationCategory;
  title: string;
  message: string;
  data?: Record<string, any>; // Additional data payload
  userId: string;
  priority: NotificationPriority;
  status: NotificationStatus;
  readAt?: string;
  actionUrl?: string;
  actionLabel?: string;
  expiresAt?: string;
  createdAt: string;
  updatedAt: string;
}

export type NotificationType = 
  | 'order_status'      // Order updates
  | 'payment'           // Payment-related
  | 'shipping'          // Shipping and tracking
  | 'chat'              // New messages
  | 'promotion'         // Deals and offers
  | 'system'            // System announcements
  | 'inventory'         // Stock alerts
  | 'review'            // Review requests/responses
  | 'security'          // Security alerts
  | 'reminder';         // Reminders and deadlines

export type NotificationCategory = 
  | 'transactional'     // Order/payment updates
  | 'marketing'         // Promotions and offers
  | 'social'            // Chat and reviews
  | 'system'            // System messages
  | 'security';         // Security alerts

export type NotificationPriority = 'low' | 'medium' | 'high' | 'urgent';

export type NotificationStatus = 'unread' | 'read' | 'dismissed' | 'archived';

export interface NotificationPreferences {
  userId: string;
  emailNotifications: {
    orderUpdates: boolean;
    promotions: boolean;
    chatMessages: boolean;
    systemAnnouncements: boolean;
    securityAlerts: boolean;
  };
  pushNotifications: {
    orderUpdates: boolean;
    promotions: boolean;
    chatMessages: boolean;
    systemAnnouncements: boolean;
    securityAlerts: boolean;
  };
  inAppNotifications: {
    orderUpdates: boolean;
    promotions: boolean;
    chatMessages: boolean;
    systemAnnouncements: boolean;
    securityAlerts: boolean;
  };
  quietHours: {
    enabled: boolean;
    startTime: string; // HH:mm format
    endTime: string;   // HH:mm format
    timezone: string;
  };
  updatedAt: string;
}

export interface NotificationBadge {
  total: number;
  unread: number;
  byType: Record<NotificationType, number>;
  byPriority: Record<NotificationPriority, number>;
}

export interface NotificationTemplate {
  id: string;
  type: NotificationType;
  category: NotificationCategory;
  title: string;
  messageTemplate: string;
  priority: NotificationPriority;
  actionUrl?: string;
  actionLabel?: string;
  isActive: boolean;
  variables: string[]; // Template variables like {{userName}}, {{orderNumber}}
}

// WebSocket message types for notifications
export interface NotificationWebSocketMessage {
  type: 'new_notification' | 'notification_read' | 'notification_updated' | 'batch_update';
  data: SystemNotification | SystemNotification[] | NotificationBadge;
  timestamp: string;
}

// Real-time notification delivery
export interface NotificationDelivery {
  notificationId: string;
  userId: string;
  channels: ('push' | 'email' | 'inapp' | 'sms')[];
  deliveredAt: string;
  readAt?: string;
  clickedAt?: string;
  status: 'pending' | 'delivered' | 'failed' | 'expired';
}

// Toast notification for UI display
export interface ToastNotification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  duration?: number; // Auto-dismiss time in ms
  persistent?: boolean; // Don't auto-dismiss
  actions?: NotificationAction[];
  position?: 'top-right' | 'top-left' | 'bottom-right' | 'bottom-left' | 'top-center' | 'bottom-center';
}

export interface NotificationAction {
  id: string;
  label: string;
  action: () => void;
  style?: 'primary' | 'secondary' | 'danger';
  loading?: boolean;
}

// Notification contexts and state
export interface NotificationContextType {
  notifications: SystemNotification[];
  badge: NotificationBadge;
  preferences: NotificationPreferences;
  connectionStatus: {
    isConnected: boolean;
    lastUpdate?: string;
  };
  toasts: ToastNotification[];
  
  // Actions
  markAsRead: (notificationId: string) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  deleteNotification: (notificationId: string) => Promise<void>;
  updatePreferences: (preferences: Partial<NotificationPreferences>) => Promise<void>;
  loadNotifications: (page?: number, limit?: number) => Promise<void>;
  
  // Toast actions
  showToast: (toast: Omit<ToastNotification, 'id'>) => void;
  dismissToast: (toastId: string) => void;
  clearAllToasts: () => void;
}

// Mock data types for development
export interface MockNotificationData {
  notifications: SystemNotification[];
  preferences: NotificationPreferences;
  templates: NotificationTemplate[];
  badge: NotificationBadge;
}