import React, { useState, useRef, useEffect } from 'react';
import { SystemNotification, NotificationPriority, NotificationType } from '../../types/notifications';
import { useNotifications } from '../../hooks/useNotifications';
import { Button } from '../ui/Button';
import { LoadingSpinner } from '../ui/LoadingSpinner';
import { cn } from '../../utils/cn';

interface NotificationBellProps {
  className?: string;
  showBadge?: boolean;
  maxNotifications?: number;
  autoMarkAsRead?: boolean;
  theme?: 'light' | 'dark';
  position?: 'left' | 'right';
}

/**
 * Notification bell component with dropdown showing recent notifications
 */
export const NotificationBell: React.FC<NotificationBellProps> = ({
  className,
  showBadge = true,
  maxNotifications = 10,
  autoMarkAsRead = false,
  theme = 'light',
  position = 'right',
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);

  const notifications = useNotifications({
    autoLoadNotifications: true,
    useMockService: true,
  });

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        dropdownRef.current &&
        buttonRef.current &&
        !dropdownRef.current.contains(event.target as Node) &&
        !buttonRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen]);

  // Auto-mark as read when opening
  useEffect(() => {
    if (isOpen && autoMarkAsRead && notifications.badge.unread > 0) {
      notifications.markAllAsRead();
    }
  }, [isOpen, autoMarkAsRead, notifications.badge.unread, notifications.markAllAsRead]);

  const handleNotificationClick = async (notification: SystemNotification) => {
    // Mark as read if unread
    if (notification.status === 'unread') {
      await notifications.markAsRead(notification.id);
    }

    // Navigate to action URL if available
    if (notification.actionUrl) {
      window.location.href = notification.actionUrl;
    }

    setIsOpen(false);
  };

  const handleMarkAllAsRead = (e: React.MouseEvent) => {
    e.stopPropagation();
    notifications.markAllAsRead();
  };

  const handleClearAll = (e: React.MouseEvent) => {
    e.stopPropagation();
    // In real implementation, this would clear all notifications
    console.log('Clear all notifications');
  };

  const formatRelativeTime = (timestamp: string) => {
    const now = new Date().getTime();
    const time = new Date(timestamp).getTime();
    const diff = now - time;

    const minutes = Math.floor(diff / (1000 * 60));
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const days = Math.floor(diff / (1000 * 60 * 60 * 24));

    if (minutes < 1) return 'Just now';
    if (minutes < 60) return `${minutes}m ago`;
    if (hours < 24) return `${hours}h ago`;
    if (days < 7) return `${days}d ago`;
    return new Date(timestamp).toLocaleDateString();
  };

  const getNotificationIcon = (type: NotificationType) => {
    const icons = {
      order_status: '📦',
      payment: '💳',
      shipping: '🚚',
      chat: '💬',
      promotion: '🎉',
      system: '⚙️',
      inventory: '📊',
      review: '⭐',
      security: '🔒',
      reminder: '⏰',
    };
    return icons[type] || '📢';
  };

  const getPriorityColor = (priority: NotificationPriority) => {
    const colors = {
      low: 'text-gray-600',
      medium: 'text-blue-600',
      high: 'text-orange-600',
      urgent: 'text-red-600',
    };
    return colors[priority];
  };

  const getPriorityBadge = (priority: NotificationPriority) => {
    if (priority === 'low') return null;
    
    const badges = {
      medium: 'bg-blue-100 text-blue-800',
      high: 'bg-orange-100 text-orange-800',
      urgent: 'bg-red-100 text-red-800',
    };

    return (
      <span className={cn('text-xs px-2 py-0.5 rounded-full font-medium', badges[priority])}>
        {priority.toUpperCase()}
      </span>
    );
  };

  const themeClasses = {
    light: 'bg-white border-gray-200 text-gray-900',
    dark: 'bg-gray-800 border-gray-700 text-white',
  };

  const positionClasses = {
    left: 'left-0 origin-top-left',
    right: 'right-0 origin-top-right',
  };

  const recentNotifications = notifications.notifications.slice(0, maxNotifications);

  return (
    <div className={cn('relative', className)}>
      {/* Bell Button */}
      <Button
        ref={buttonRef}
        onClick={() => setIsOpen(!isOpen)}
        variant="ghost"
        size="sm"
        className={cn(
          'relative p-2 transition-colors',
          isOpen ? 'bg-gray-100 text-gray-900' : 'hover:bg-gray-50'
        )}
        aria-label="Notifications"
      >
        <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 20 20">
          <path d="M10 2a6 6 0 00-6 6v3.586l-.707.707A1 1 0 004 14h12a1 1 0 00.707-1.707L16 11.586V8a6 6 0 00-6-6zM10 18a3 3 0 01-3-3h6a3 3 0 01-3 3z" />
        </svg>
        
        {/* Connection status indicator */}
        <div
          className={cn(
            'absolute -top-1 -left-1 w-3 h-3 rounded-full',
            notifications.isConnected ? 'bg-green-500' : 'bg-red-500'
          )}
        />

        {/* Badge */}
        {showBadge && notifications.badge.unread > 0 && (
          <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs font-bold rounded-full w-5 h-5 flex items-center justify-center">
            {notifications.badge.unread > 99 ? '99+' : notifications.badge.unread}
          </span>
        )}
      </Button>

      {/* Dropdown */}
      {isOpen && (
        <div
          ref={dropdownRef}
          className={cn(
            'absolute z-50 mt-2 w-80 max-w-sm rounded-lg shadow-lg border',
            positionClasses[position],
            themeClasses[theme]
          )}
        >
          {/* Header */}
          <div className="p-4 border-b border-gray-200">
            <div className="flex items-center justify-between">
              <h3 className="font-semibold text-lg">Notifications</h3>
              <div className="flex items-center gap-2">
                <span
                  className={cn(
                    'w-2 h-2 rounded-full',
                    notifications.isConnected ? 'bg-green-500' : 'bg-red-500'
                  )}
                />
                <span className="text-xs text-gray-500">
                  {notifications.isConnected ? 'Live' : 'Offline'}
                </span>
              </div>
            </div>
            
            {/* Action buttons */}
            {recentNotifications.length > 0 && (
              <div className="flex gap-2 mt-2">
                {notifications.badge.unread > 0 && (
                  <Button
                    onClick={handleMarkAllAsRead}
                    variant="ghost"
                    size="xs"
                    className="text-xs"
                  >
                    Mark all read
                  </Button>
                )}
                <Button
                  onClick={handleClearAll}
                  variant="ghost"
                  size="xs"
                  className="text-xs text-red-600 hover:text-red-700"
                >
                  Clear all
                </Button>
              </div>
            )}
          </div>

          {/* Notifications List */}
          <div className="max-h-96 overflow-y-auto">
            {notifications.isLoading && recentNotifications.length === 0 ? (
              <div className="flex items-center justify-center p-8">
                <LoadingSpinner />
              </div>
            ) : recentNotifications.length === 0 ? (
              <div className="p-8 text-center text-gray-500">
                <div className="text-4xl mb-2">🔔</div>
                <p className="text-sm">No notifications yet</p>
                <p className="text-xs mt-1">You'll see new notifications here</p>
              </div>
            ) : (
              <div className="py-2">
                {recentNotifications.map((notification) => (
                  <div
                    key={notification.id}
                    onClick={() => handleNotificationClick(notification)}
                    className={cn(
                      'p-4 border-b border-gray-100 cursor-pointer transition-colors hover:bg-gray-50',
                      notification.status === 'unread' ? 'bg-blue-50 border-l-4 border-l-blue-500' : '',
                      theme === 'dark' && 'hover:bg-gray-700 border-gray-600'
                    )}
                  >
                    <div className="flex gap-3">
                      <div className="flex-shrink-0 text-lg">
                        {getNotificationIcon(notification.type)}
                      </div>
                      
                      <div className="flex-1 min-w-0">
                        <div className="flex items-start justify-between gap-2">
                          <h4 className={cn(
                            'text-sm font-medium truncate',
                            notification.status === 'unread' ? 'text-gray-900' : 'text-gray-700'
                          )}>
                            {notification.title}
                          </h4>
                          
                          <div className="flex items-center gap-1 flex-shrink-0">
                            {getPriorityBadge(notification.priority)}
                            <span className="text-xs text-gray-500">
                              {formatRelativeTime(notification.createdAt)}
                            </span>
                          </div>
                        </div>
                        
                        <p className={cn(
                          'text-xs mt-1 line-clamp-2',
                          notification.status === 'unread' ? 'text-gray-800' : 'text-gray-600'
                        )}>
                          {notification.message}
                        </p>
                        
                        {notification.actionLabel && (
                          <div className="mt-2">
                            <span className="text-xs text-blue-600 font-medium">
                              {notification.actionLabel} →
                            </span>
                          </div>
                        )}

                        {/* Expiration warning */}
                        {notification.expiresAt && new Date(notification.expiresAt) < new Date(Date.now() + 24 * 60 * 60 * 1000) && (
                          <div className="mt-1 text-xs text-orange-600">
                            ⏰ Expires {formatRelativeTime(notification.expiresAt)}
                          </div>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer */}
          {recentNotifications.length > 0 && (
            <div className="p-3 border-t border-gray-200 text-center">
              <Button
                variant="ghost"
                size="sm"
                className="text-xs text-blue-600 hover:text-blue-700"
                onClick={() => {
                  // Navigate to full notifications page
                  window.location.href = '/notifications';
                  setIsOpen(false);
                }}
              >
                View all notifications
              </Button>
            </div>
          )}

          {/* Error message */}
          {notifications.error && (
            <div className="p-3 border-t border-red-200 bg-red-50 text-red-700 text-xs">
              {notifications.error}
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default NotificationBell;