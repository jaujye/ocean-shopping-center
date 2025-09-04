import { useState, useEffect, useCallback, useRef } from 'react';
import {
  SystemNotification,
  NotificationPreferences,
  NotificationBadge,
  ToastNotification,
  NotificationAction,
  NotificationType,
  NotificationPriority,
  NotificationWebSocketMessage,
  MockNotificationData
} from '../types/notifications';
import { useWebSocket, UseWebSocketOptions } from './useWebSocket';

// Mock notification service (replace with real API service later)
class MockNotificationService {
  private mockNotifications: SystemNotification[] = [
    {
      id: 'notif-1',
      type: 'order_status',
      category: 'transactional',
      title: 'Order Shipped',
      message: 'Your order #12345 has been shipped and is on its way!',
      userId: 'user-1',
      priority: 'high',
      status: 'unread',
      actionUrl: '/orders/12345',
      actionLabel: 'Track Order',
      createdAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(), // 30 minutes ago
      updatedAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
    },
    {
      id: 'notif-2',
      type: 'chat',
      category: 'social',
      title: 'New Message',
      message: 'You have a new message from Ocean Store Support',
      userId: 'user-1',
      priority: 'medium',
      status: 'unread',
      actionUrl: '/chat/conv-1',
      actionLabel: 'View Message',
      createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(), // 1 hour ago
      updatedAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'notif-3',
      type: 'promotion',
      category: 'marketing',
      title: 'Special Offer',
      message: 'Get 20% off your next purchase! Limited time offer.',
      userId: 'user-1',
      priority: 'low',
      status: 'unread',
      actionUrl: '/promotions/summer-sale',
      actionLabel: 'Shop Now',
      expiresAt: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(), // Expires in 24 hours
      createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
      updatedAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'notif-4',
      type: 'system',
      category: 'system',
      title: 'Scheduled Maintenance',
      message: 'Our system will undergo maintenance tonight from 2 AM to 4 AM EST.',
      userId: 'user-1',
      priority: 'medium',
      status: 'read',
      readAt: new Date(Date.now() - 30 * 60 * 1000).toISOString(),
      createdAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(), // 4 hours ago
      updatedAt: new Date(Date.now() - 4 * 60 * 60 * 1000).toISOString(),
    },
    {
      id: 'notif-5',
      type: 'security',
      category: 'security',
      title: 'New Login Detected',
      message: 'We detected a new login to your account from a new device.',
      userId: 'user-1',
      priority: 'high',
      status: 'read',
      actionUrl: '/security/activity',
      actionLabel: 'Review Activity',
      readAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
      createdAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(), // 6 hours ago
      updatedAt: new Date(Date.now() - 6 * 60 * 60 * 1000).toISOString(),
    },
  ];

  private mockPreferences: NotificationPreferences = {
    userId: 'user-1',
    emailNotifications: {
      orderUpdates: true,
      promotions: false,
      chatMessages: true,
      systemAnnouncements: true,
      securityAlerts: true,
    },
    pushNotifications: {
      orderUpdates: true,
      promotions: true,
      chatMessages: true,
      systemAnnouncements: false,
      securityAlerts: true,
    },
    inAppNotifications: {
      orderUpdates: true,
      promotions: true,
      chatMessages: true,
      systemAnnouncements: true,
      securityAlerts: true,
    },
    quietHours: {
      enabled: true,
      startTime: '22:00',
      endTime: '08:00',
      timezone: 'America/New_York',
    },
    updatedAt: new Date().toISOString(),
  };

  async getNotifications(userId: string, page = 1, limit = 20): Promise<SystemNotification[]> {
    await new Promise(resolve => setTimeout(resolve, 300)); // Simulate delay
    return this.mockNotifications.filter(n => n.userId === userId).slice((page - 1) * limit, page * limit);
  }

  async markAsRead(notificationId: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 200));
    const notification = this.mockNotifications.find(n => n.id === notificationId);
    if (notification) {
      notification.status = 'read';
      notification.readAt = new Date().toISOString();
    }
  }

  async markAllAsRead(userId: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 400));
    this.mockNotifications
      .filter(n => n.userId === userId && n.status === 'unread')
      .forEach(n => {
        n.status = 'read';
        n.readAt = new Date().toISOString();
      });
  }

  async deleteNotification(notificationId: string): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 200));
    const index = this.mockNotifications.findIndex(n => n.id === notificationId);
    if (index > -1) {
      this.mockNotifications.splice(index, 1);
    }
  }

  async getPreferences(userId: string): Promise<NotificationPreferences> {
    await new Promise(resolve => setTimeout(resolve, 200));
    return this.mockPreferences;
  }

  async updatePreferences(userId: string, preferences: Partial<NotificationPreferences>): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 300));
    Object.assign(this.mockPreferences, preferences, {
      userId,
      updatedAt: new Date().toISOString(),
    });
  }

  getBadge(userId: string): NotificationBadge {
    const userNotifications = this.mockNotifications.filter(n => n.userId === userId);
    const unread = userNotifications.filter(n => n.status === 'unread');

    const byType = userNotifications.reduce((acc, n) => {
      acc[n.type] = (acc[n.type] || 0) + (n.status === 'unread' ? 1 : 0);
      return acc;
    }, {} as Record<NotificationType, number>);

    const byPriority = userNotifications.reduce((acc, n) => {
      acc[n.priority] = (acc[n.priority] || 0) + (n.status === 'unread' ? 1 : 0);
      return acc;
    }, {} as Record<NotificationPriority, number>);

    return {
      total: userNotifications.length,
      unread: unread.length,
      byType,
      byPriority,
    };
  }

  // Simulate adding a new notification for testing
  addNotification(notification: Omit<SystemNotification, 'id' | 'createdAt' | 'updatedAt'>): SystemNotification {
    const newNotification: SystemNotification = {
      ...notification,
      id: `notif-${Date.now()}`,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };
    this.mockNotifications.unshift(newNotification);
    return newNotification;
  }
}

const mockService = new MockNotificationService();

export interface UseNotificationsOptions extends UseWebSocketOptions {
  userId?: string;
  autoLoadNotifications?: boolean;
  maxToasts?: number;
  toastDefaultDuration?: number;
}

export interface UseNotificationsReturn {
  // State
  notifications: SystemNotification[];
  badge: NotificationBadge;
  preferences: NotificationPreferences;
  toasts: ToastNotification[];
  isLoading: boolean;
  error: string | null;
  isConnected: boolean;

  // Actions
  loadNotifications: (page?: number, limit?: number) => Promise<void>;
  markAsRead: (notificationId: string) => Promise<void>;
  markAllAsRead: () => Promise<void>;
  deleteNotification: (notificationId: string) => Promise<void>;
  updatePreferences: (preferences: Partial<NotificationPreferences>) => Promise<void>;

  // Toast actions
  showToast: (toast: Omit<ToastNotification, 'id'>) => void;
  dismissToast: (toastId: string) => void;
  clearAllToasts: () => void;

  // WebSocket connection
  connect: () => Promise<void>;
  disconnect: () => void;
}

/**
 * Custom hook for notification management with real-time updates
 */
export const useNotifications = (options: UseNotificationsOptions = {}): UseNotificationsReturn => {
  const {
    userId = 'user-1',
    autoLoadNotifications = true,
    maxToasts = 5,
    toastDefaultDuration = 5000,
    ...websocketOptions
  } = options;

  // State
  const [notifications, setNotifications] = useState<SystemNotification[]>([]);
  const [badge, setBadge] = useState<NotificationBadge>({
    total: 0,
    unread: 0,
    byType: {} as Record<NotificationType, number>,
    byPriority: {} as Record<NotificationPriority, number>,
  });
  const [preferences, setPreferences] = useState<NotificationPreferences>({
    userId,
    emailNotifications: {
      orderUpdates: true,
      promotions: false,
      chatMessages: true,
      systemAnnouncements: true,
      securityAlerts: true,
    },
    pushNotifications: {
      orderUpdates: true,
      promotions: true,
      chatMessages: true,
      systemAnnouncements: false,
      securityAlerts: true,
    },
    inAppNotifications: {
      orderUpdates: true,
      promotions: true,
      chatMessages: true,
      systemAnnouncements: true,
      securityAlerts: true,
    },
    quietHours: {
      enabled: false,
      startTime: '22:00',
      endTime: '08:00',
      timezone: 'America/New_York',
    },
    updatedAt: new Date().toISOString(),
  });
  const [toasts, setToasts] = useState<ToastNotification[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // WebSocket connection
  const websocket = useWebSocket({
    autoConnect: true,
    ...websocketOptions,
  });

  // Refs
  const toastTimeoutRefs = useRef<Map<string, NodeJS.Timeout>>(new Map());

  // Update badge
  const updateBadge = useCallback(() => {
    setBadge(mockService.getBadge(userId));
  }, [userId]);

  // Load notifications
  const loadNotifications = useCallback(async (page = 1, limit = 20): Promise<void> => {
    setIsLoading(true);
    setError(null);

    try {
      const newNotifications = await mockService.getNotifications(userId, page, limit);
      
      if (page === 1) {
        setNotifications(newNotifications);
      } else {
        setNotifications(prev => [...prev, ...newNotifications]);
      }
      
      updateBadge();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setIsLoading(false);
    }
  }, [userId, updateBadge]);

  // Load preferences
  const loadPreferences = useCallback(async (): Promise<void> => {
    try {
      const prefs = await mockService.getPreferences(userId);
      setPreferences(prefs);
    } catch (err) {
      console.error('Failed to load preferences:', err);
    }
  }, [userId]);

  // Mark as read
  const markAsRead = useCallback(async (notificationId: string): Promise<void> => {
    setError(null);

    try {
      await mockService.markAsRead(notificationId);
      
      setNotifications(prev =>
        prev.map(n =>
          n.id === notificationId
            ? { ...n, status: 'read', readAt: new Date().toISOString() }
            : n
        )
      );
      
      updateBadge();

      // Send read receipt through WebSocket
      if (websocket.isConnected) {
        websocket.send('notification_read', {
          type: 'notification_read',
          data: { notificationId, userId, timestamp: new Date().toISOString() },
          timestamp: new Date().toISOString(),
        });
      }
    } catch (err) {
      setError((err as Error).message);
    }
  }, [updateBadge, websocket.isConnected, websocket.send, userId]);

  // Mark all as read
  const markAllAsRead = useCallback(async (): Promise<void> => {
    setError(null);

    try {
      await mockService.markAllAsRead(userId);
      
      setNotifications(prev =>
        prev.map(n => ({
          ...n,
          status: n.status === 'unread' ? 'read' : n.status,
          readAt: n.status === 'unread' ? new Date().toISOString() : n.readAt,
        }))
      );
      
      updateBadge();

      // Send batch read through WebSocket
      if (websocket.isConnected) {
        websocket.send('notifications_read_all', {
          type: 'batch_update',
          data: { userId, action: 'mark_all_read', timestamp: new Date().toISOString() },
          timestamp: new Date().toISOString(),
        });
      }
    } catch (err) {
      setError((err as Error).message);
    }
  }, [userId, updateBadge, websocket.isConnected, websocket.send]);

  // Delete notification
  const deleteNotification = useCallback(async (notificationId: string): Promise<void> => {
    setError(null);

    try {
      await mockService.deleteNotification(notificationId);
      
      setNotifications(prev => prev.filter(n => n.id !== notificationId));
      updateBadge();
    } catch (err) {
      setError((err as Error).message);
    }
  }, [updateBadge]);

  // Update preferences
  const updatePreferences = useCallback(async (
    newPreferences: Partial<NotificationPreferences>
  ): Promise<void> => {
    setError(null);

    try {
      await mockService.updatePreferences(userId, newPreferences);
      setPreferences(prev => ({ ...prev, ...newPreferences, updatedAt: new Date().toISOString() }));
    } catch (err) {
      setError((err as Error).message);
    }
  }, [userId]);

  // Show toast notification
  const showToast = useCallback((toastData: Omit<ToastNotification, 'id'>): void => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const toast: ToastNotification = {
      id,
      duration: toastDefaultDuration,
      position: 'top-right',
      ...toastData,
    };

    setToasts(prev => {
      const newToasts = [toast, ...prev].slice(0, maxToasts);
      return newToasts;
    });

    // Auto-dismiss if not persistent
    if (!toast.persistent && toast.duration) {
      const timeout = setTimeout(() => {
        dismissToast(id);
      }, toast.duration);
      
      toastTimeoutRefs.current.set(id, timeout);
    }
  }, [toastDefaultDuration, maxToasts]);

  // Dismiss toast
  const dismissToast = useCallback((toastId: string): void => {
    setToasts(prev => prev.filter(t => t.id !== toastId));
    
    const timeout = toastTimeoutRefs.current.get(toastId);
    if (timeout) {
      clearTimeout(timeout);
      toastTimeoutRefs.current.delete(toastId);
    }
  }, []);

  // Clear all toasts
  const clearAllToasts = useCallback((): void => {
    setToasts([]);
    toastTimeoutRefs.current.forEach(timeout => clearTimeout(timeout));
    toastTimeoutRefs.current.clear();
  }, []);

  // WebSocket message handlers
  useEffect(() => {
    if (!websocket.isConnected) return;

    const handleNewNotification = (data: NotificationWebSocketMessage) => {
      if (data.type === 'new_notification') {
        const notification = data.data as SystemNotification;
        
        setNotifications(prev => [notification, ...prev]);
        updateBadge();

        // Show toast if enabled in preferences
        const shouldShowToast = preferences.inAppNotifications[
          notification.type as keyof typeof preferences.inAppNotifications
        ];

        if (shouldShowToast) {
          showToast({
            type: notification.priority === 'urgent' ? 'error' : 
                  notification.priority === 'high' ? 'warning' : 'info',
            title: notification.title,
            message: notification.message,
            actions: notification.actionUrl ? [
              {
                id: 'action',
                label: notification.actionLabel || 'View',
                action: () => {
                  window.location.href = notification.actionUrl!;
                },
              },
            ] : undefined,
          });
        }
      }
    };

    const handleNotificationUpdate = (data: NotificationWebSocketMessage) => {
      if (data.type === 'notification_updated') {
        const updatedNotification = data.data as SystemNotification;
        
        setNotifications(prev =>
          prev.map(n =>
            n.id === updatedNotification.id ? updatedNotification : n
          )
        );
        
        updateBadge();
      }
    };

    const handleBatchUpdate = (data: NotificationWebSocketMessage) => {
      if (data.type === 'batch_update') {
        const batchData = data.data as NotificationBadge;
        setBadge(batchData);
      }
    };

    const unsubscribeNew = websocket.subscribe('new_notification', handleNewNotification);
    const unsubscribeUpdate = websocket.subscribe('notification_updated', handleNotificationUpdate);
    const unsubscribeBatch = websocket.subscribe('batch_notification_update', handleBatchUpdate);

    return () => {
      unsubscribeNew();
      unsubscribeUpdate();
      unsubscribeBatch();
    };
  }, [websocket.isConnected, websocket.subscribe, preferences.inAppNotifications, updateBadge, showToast]);

  // Auto-load on mount
  useEffect(() => {
    if (autoLoadNotifications) {
      loadNotifications();
      loadPreferences();
    }
  }, [autoLoadNotifications, loadNotifications, loadPreferences]);

  // Join notification channel when connected
  useEffect(() => {
    if (websocket.isConnected) {
      websocket.joinChannel(`notifications/${userId}`);
    }
  }, [websocket.isConnected, websocket.joinChannel, userId]);

  // Cleanup toast timeouts on unmount
  useEffect(() => {
    return () => {
      toastTimeoutRefs.current.forEach(timeout => clearTimeout(timeout));
      toastTimeoutRefs.current.clear();
    };
  }, []);

  return {
    // State
    notifications,
    badge,
    preferences,
    toasts,
    isLoading,
    error,
    isConnected: websocket.isConnected,

    // Actions
    loadNotifications,
    markAsRead,
    markAllAsRead,
    deleteNotification,
    updatePreferences,

    // Toast actions
    showToast,
    dismissToast,
    clearAllToasts,

    // WebSocket connection
    connect: websocket.connect,
    disconnect: websocket.disconnect,
  };
};

/**
 * Hook for showing system toasts
 */
export const useToast = () => {
  const [toasts, setToasts] = useState<ToastNotification[]>([]);
  const toastTimeoutRefs = useRef<Map<string, NodeJS.Timeout>>(new Map());

  const showToast = useCallback((toastData: Omit<ToastNotification, 'id'>): void => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
    const toast: ToastNotification = {
      id,
      duration: 5000,
      position: 'top-right',
      ...toastData,
    };

    setToasts(prev => [toast, ...prev].slice(0, 5));

    if (!toast.persistent && toast.duration) {
      const timeout = setTimeout(() => {
        setToasts(prev => prev.filter(t => t.id !== id));
      }, toast.duration);
      
      toastTimeoutRefs.current.set(id, timeout);
    }
  }, []);

  const dismissToast = useCallback((toastId: string): void => {
    setToasts(prev => prev.filter(t => t.id !== toastId));
    
    const timeout = toastTimeoutRefs.current.get(toastId);
    if (timeout) {
      clearTimeout(timeout);
      toastTimeoutRefs.current.delete(toastId);
    }
  }, []);

  useEffect(() => {
    return () => {
      toastTimeoutRefs.current.forEach(timeout => clearTimeout(timeout));
      toastTimeoutRefs.current.clear();
    };
  }, []);

  return {
    toasts,
    showToast,
    dismissToast,
    showSuccess: (message: string, title?: string) => showToast({ type: 'success', title: title || 'Success', message }),
    showError: (message: string, title?: string) => showToast({ type: 'error', title: title || 'Error', message }),
    showWarning: (message: string, title?: string) => showToast({ type: 'warning', title: title || 'Warning', message }),
    showInfo: (message: string, title?: string) => showToast({ type: 'info', title: title || 'Info', message }),
  };
};