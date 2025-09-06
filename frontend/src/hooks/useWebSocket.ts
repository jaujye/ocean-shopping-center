import { useEffect, useState, useCallback, useRef } from 'react';
import { 
  initializeWebSocket, 
  getWebSocketService, 
  WebSocketConnectionStatus, 
  WebSocketConfig,
  WebSocketEventHandler,
  WebSocketErrorHandler,
  WebSocketStatusHandler,
  MockWebSocketService
} from '../services/websocket';

interface WebSocketServiceInterface {
  connect(): Promise<void>;
  disconnect(): void;
  send<T>(event: string, data: T): Promise<void>;
  on<T>(event: string, handler: WebSocketEventHandler<T>): void;
  off<T>(event: string, handler: WebSocketEventHandler<T>): void;
  onStatusChange(handler: WebSocketStatusHandler): void;
  onError(handler: WebSocketErrorHandler): void;
  getStatus(): WebSocketConnectionStatus;
  joinChannel(channel: string): Promise<void>;
  leaveChannel(channel: string): Promise<void>;
}

export interface UseWebSocketReturn {
  connectionStatus: WebSocketConnectionStatus;
  isConnected: boolean;
  isReconnecting: boolean;
  connect: () => Promise<void>;
  disconnect: () => void;
  send: <T>(event: string, data: T) => Promise<void>;
  subscribe: <T>(event: string, handler: WebSocketEventHandler<T>) => () => void;
  joinChannel: (channel: string) => Promise<void>;
  leaveChannel: (channel: string) => Promise<void>;
  error: string | null;
}

export interface UseWebSocketOptions {
  url?: string;
  token?: string;
  userId?: string;
  autoConnect?: boolean;
  maxReconnectAttempts?: number;
  reconnectDelay?: number;
  useMockService?: boolean;
}

/**
 * Custom hook for WebSocket connection management
 */
export const useWebSocket = (options: UseWebSocketOptions = {}): UseWebSocketReturn => {
  const {
    url = process.env.REACT_APP_WEBSOCKET_URL || 'ws://localhost:8080',
    token,
    userId,
    autoConnect = true,
    maxReconnectAttempts = 5,
    reconnectDelay = 1000,
    useMockService = process.env.NODE_ENV === 'development',
  } = options;

  const [connectionStatus, setConnectionStatus] = useState<WebSocketConnectionStatus>({
    isConnected: false,
    isReconnecting: false,
    reconnectAttempts: 0,
  });
  const [error, setError] = useState<string | null>(null);
  
  const serviceRef = useRef<WebSocketServiceInterface | null>(null);
  const handlersRef = useRef<Map<string, WebSocketEventHandler[]>>(new Map());
  const isInitializedRef = useRef(false);

  // Initialize WebSocket service
  const initialize = useCallback(() => {
    if (isInitializedRef.current) return;

    const config: WebSocketConfig = {
      url,
      token,
      userId,
      autoConnect,
      maxReconnectAttempts,
      reconnectDelay,
    };

    try {
      if (useMockService) {
        serviceRef.current = new MockWebSocketService(config);
      } else {
        serviceRef.current = initializeWebSocket(config);
      }

      // Set up status change handler
      const statusHandler: WebSocketStatusHandler = (status) => {
        setConnectionStatus(status);
        if (status.lastError) {
          setError(status.lastError);
        } else {
          setError(null);
        }
      };

      // Set up error handler
      const errorHandler: WebSocketErrorHandler = (err) => {
        setError(err.message);
      };

      serviceRef.current.onStatusChange(statusHandler);
      serviceRef.current.onError(errorHandler);

      // Re-register all existing handlers
      handlersRef.current.forEach((handlers, event) => {
        handlers.forEach(handler => {
          serviceRef.current?.on(event, handler);
        });
      });

      isInitializedRef.current = true;
    } catch (err) {
      setError((err as Error).message);
    }
  }, [url, token, userId, autoConnect, maxReconnectAttempts, reconnectDelay, useMockService]);

  // Connect to WebSocket
  const connect = useCallback(async (): Promise<void> => {
    if (!serviceRef.current) {
      initialize();
    }
    
    if (!serviceRef.current) {
      throw new Error('WebSocket service initialization failed');
    }
    
    try {
      await serviceRef.current.connect();
      setError(null);
    } catch (err) {
      setError((err as Error).message);
      throw err;
    }
  }, [initialize]);

  // Disconnect from WebSocket
  const disconnect = useCallback((): void => {
    if (serviceRef.current) {
      serviceRef.current.disconnect();
    }
    setConnectionStatus({
      isConnected: false,
      isReconnecting: false,
      reconnectAttempts: 0,
    });
  }, []);

  // Send message through WebSocket
  const send = useCallback(async <T>(event: string, data: T): Promise<void> => {
    if (!serviceRef.current) {
      throw new Error('WebSocket service not initialized');
    }
    
    try {
      await serviceRef.current.send(event, data);
    } catch (err) {
      setError((err as Error).message);
      throw err;
    }
  }, []);

  // Subscribe to WebSocket events
  const subscribe = useCallback(<T>(
    event: string, 
    handler: WebSocketEventHandler<T>
  ): (() => void) => {
    // Store handler for re-registration after reconnection
    if (!handlersRef.current.has(event)) {
      handlersRef.current.set(event, []);
    }
    handlersRef.current.get(event)!.push(handler);

    // Register with current service if available
    if (serviceRef.current) {
      serviceRef.current.on(event, handler);
    }

    // Return unsubscribe function
    return () => {
      const handlers = handlersRef.current.get(event);
      if (handlers) {
        const index = handlers.indexOf(handler);
        if (index > -1) {
          handlers.splice(index, 1);
        }
        if (handlers.length === 0) {
          handlersRef.current.delete(event);
        }
      }
      
      if (serviceRef.current) {
        serviceRef.current.off(event, handler);
      }
    };
  }, []);

  // Join a channel/room
  const joinChannel = useCallback(async (channel: string): Promise<void> => {
    if (!serviceRef.current) {
      throw new Error('WebSocket service not initialized');
    }
    
    try {
      await serviceRef.current.joinChannel(channel);
    } catch (err) {
      setError((err as Error).message);
      throw err;
    }
  }, []);

  // Leave a channel/room
  const leaveChannel = useCallback(async (channel: string): Promise<void> => {
    if (!serviceRef.current) {
      throw new Error('WebSocket service not initialized');
    }
    
    try {
      await serviceRef.current.leaveChannel(channel);
    } catch (err) {
      setError((err as Error).message);
      throw err;
    }
  }, []);

  // Initialize on mount
  useEffect(() => {
    initialize();
    
    // Cleanup on unmount
    return () => {
      if (serviceRef.current) {
        serviceRef.current.disconnect();
      }
      isInitializedRef.current = false;
    };
  }, [initialize]);

  // Update connection status periodically
  useEffect(() => {
    let isMounted = true;
    
    const interval = setInterval(() => {
      if (isMounted && serviceRef.current) {
        const status = serviceRef.current.getStatus();
        if (isMounted) {
          setConnectionStatus(status);
        }
      }
    }, 1000);

    return () => {
      isMounted = false;
      clearInterval(interval);
    };
  }, []);

  return {
    connectionStatus,
    isConnected: connectionStatus.isConnected,
    isReconnecting: connectionStatus.isReconnecting,
    connect,
    disconnect,
    send,
    subscribe,
    joinChannel,
    leaveChannel,
    error,
  };
};

/**
 * Hook for managing WebSocket channels
 */
export const useWebSocketChannel = (
  channel: string,
  options: UseWebSocketOptions = {}
) => {
  const websocket = useWebSocket(options);
  const [isJoined, setIsJoined] = useState(false);

  // Join channel when connected
  useEffect(() => {
    if (websocket.isConnected && channel && !isJoined) {
      websocket.joinChannel(channel)
        .then(() => setIsJoined(true))
        .catch(err => console.error('Failed to join channel:', err));
    }
  }, [websocket.isConnected, channel, websocket.joinChannel, isJoined]);

  // Leave channel on unmount
  useEffect(() => {
    return () => {
      if (isJoined && channel) {
        websocket.leaveChannel(channel).catch(err => 
          console.error('Failed to leave channel:', err)
        );
      }
    };
  }, [channel, websocket.leaveChannel, isJoined]);

  return {
    ...websocket,
    isJoined,
    channel,
  };
};

/**
 * Hook for subscribing to specific WebSocket events with automatic cleanup
 */
export const useWebSocketSubscription = <T>(
  event: string,
  handler: WebSocketEventHandler<T>,
  options: UseWebSocketOptions = {},
  dependencies: React.DependencyList = []
) => {
  const websocket = useWebSocket(options);

  useEffect(() => {
    if (!websocket.isConnected || !handler) return;

    const unsubscribe = websocket.subscribe(event, handler);
    return unsubscribe;
  }, [websocket.isConnected, event, websocket.subscribe, ...dependencies]);

  return websocket;
};

/**
 * Hook for periodic connection health checks
 */
export const useWebSocketHealth = (options: UseWebSocketOptions = {}) => {
  const websocket = useWebSocket(options);
  const [health, setHealth] = useState({
    isHealthy: false,
    avgLatency: 0,
    uptime: 0,
    lastCheck: new Date(),
  });

  useEffect(() => {
    const interval = setInterval(() => {
      if (websocket.isConnected) {
        const { latency, lastConnected } = websocket.connectionStatus;
        const now = new Date();
        const uptime = lastConnected ? now.getTime() - lastConnected.getTime() : 0;
        
        setHealth({
          isHealthy: latency ? latency < 1000 : false, // Healthy if latency < 1s
          avgLatency: latency || 0,
          uptime,
          lastCheck: now,
        });
      } else {
        setHealth(prev => ({
          ...prev,
          isHealthy: false,
          lastCheck: new Date(),
        }));
      }
    }, 5000); // Check every 5 seconds

    return () => clearInterval(interval);
  }, [websocket.isConnected, websocket.connectionStatus]);

  return {
    ...websocket,
    health,
  };
};