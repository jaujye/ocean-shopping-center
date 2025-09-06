import React, { createContext, useContext, useState, useCallback, ReactNode } from 'react';

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number;
}

interface NotificationContextType {
  notifications: Notification[];
  showNotification: (type: Notification['type'], message: string, duration?: number) => void;
  removeNotification: (id: string) => void;
  clearNotifications: () => void;
}

const NotificationContext = createContext<NotificationContextType | undefined>(undefined);

export const useNotification = () => {
  const context = useContext(NotificationContext);
  if (!context) {
    throw new Error('useNotification must be used within a NotificationProvider');
  }
  return context;
};

interface NotificationProviderProps {
  children: ReactNode;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const [notifications, setNotifications] = useState<Notification[]>([]);

  const removeNotification = useCallback((id: string) => {
    setNotifications(prev => prev.filter(n => n.id !== id));
  }, []);

  const showNotification = useCallback((
    type: Notification['type'],
    message: string,
    duration: number = 5000
  ) => {
    const id = Date.now().toString();
    const notification: Notification = {
      id,
      type,
      message,
      duration
    };

    setNotifications(prev => [...prev, notification]);

    if (duration > 0) {
      setTimeout(() => {
        removeNotification(id);
      }, duration);
    }
  }, [removeNotification]);

  const clearNotifications = useCallback(() => {
    setNotifications([]);
  }, []);

  const value: NotificationContextType = {
    notifications,
    showNotification,
    removeNotification,
    clearNotifications
  };

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <NotificationDisplay notifications={notifications} onRemove={removeNotification} />
    </NotificationContext.Provider>
  );
};

interface NotificationDisplayProps {
  notifications: Notification[];
  onRemove: (id: string) => void;
}

const NotificationDisplay: React.FC<NotificationDisplayProps> = ({ notifications, onRemove }) => {
  const getNotificationStyle = (type: Notification['type']) => {
    const baseStyle = 'px-4 py-3 rounded-lg shadow-lg flex items-center justify-between mb-2 animate-slide-in';
    
    switch (type) {
      case 'success':
        return `${baseStyle} bg-green-500 text-white`;
      case 'error':
        return `${baseStyle} bg-red-500 text-white`;
      case 'warning':
        return `${baseStyle} bg-yellow-500 text-white`;
      case 'info':
        return `${baseStyle} bg-blue-500 text-white`;
      default:
        return `${baseStyle} bg-gray-500 text-white`;
    }
  };

  const getIcon = (type: Notification['type']) => {
    switch (type) {
      case 'success':
        return '✓';
      case 'error':
        return '✕';
      case 'warning':
        return '⚠';
      case 'info':
        return 'ℹ';
      default:
        return '';
    }
  };

  if (notifications.length === 0) return null;

  return (
    <div className="fixed top-4 right-4 z-50 max-w-sm">
      {notifications.map(notification => (
        <div
          key={notification.id}
          className={getNotificationStyle(notification.type)}
        >
          <div className="flex items-center">
            <span className="mr-2 text-xl">{getIcon(notification.type)}</span>
            <span className="flex-1">{notification.message}</span>
          </div>
          <button
            onClick={() => onRemove(notification.id)}
            className="ml-4 text-white hover:text-gray-200 transition-colors"
            aria-label="Close notification"
          >
            ✕
          </button>
        </div>
      ))}
    </div>
  );
};