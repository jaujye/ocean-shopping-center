import { io, Socket } from 'socket.io-client';
import { ChatWebSocketMessage } from '../types/chat';
import { NotificationWebSocketMessage } from '../types/notifications';
import { ShippingWebSocketMessage } from '../types/shipping';

export type WebSocketMessage = ChatWebSocketMessage | NotificationWebSocketMessage | ShippingWebSocketMessage;

export interface WebSocketConnectionStatus {
  isConnected: boolean;
  isReconnecting: boolean;
  reconnectAttempts: number;
  lastConnected?: Date;
  lastError?: string;
  latency?: number;
}

export interface WebSocketConfig {
  url: string;
  token?: string;
  userId?: string;
  autoConnect?: boolean;
  maxReconnectAttempts?: number;
  reconnectDelay?: number;
}

export type WebSocketEventHandler<T = any> = (data: T) => void;
export type WebSocketErrorHandler = (error: Error) => void;
export type WebSocketStatusHandler = (status: WebSocketConnectionStatus) => void;

class WebSocketService {
  private socket: Socket | null = null;
  private config: WebSocketConfig;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 1000; // Start with 1 second
  private reconnectTimer: NodeJS.Timeout | null = null;
  private lastConnected?: Date;
  private lastError?: string;
  private pingInterval: NodeJS.Timeout | null = null;
  private latency?: number;

  // Event handlers
  private statusHandlers: WebSocketStatusHandler[] = [];
  private messageHandlers: Map<string, WebSocketEventHandler[]> = new Map();
  private errorHandlers: WebSocketErrorHandler[] = [];

  constructor(config: WebSocketConfig) {
    this.config = {
      maxReconnectAttempts: 5,
      reconnectDelay: 1000,
      autoConnect: true,
      ...config,
    };
    this.maxReconnectAttempts = this.config.maxReconnectAttempts!;
    this.reconnectDelay = this.config.reconnectDelay!;

    if (this.config.autoConnect) {
      this.connect();
    }
  }

  /**
   * Connect to WebSocket server
   */
  public connect(): Promise<void> {
    return new Promise((resolve, reject) => {
      try {
        if (this.socket?.connected) {
          resolve();
          return;
        }

        const socketOptions = {
          transports: ['websocket', 'polling'],
          auth: {
            token: this.config.token,
            userId: this.config.userId,
          },
          forceNew: true,
          timeout: 10000,
          reconnection: false, // We handle reconnection manually
        };

        this.socket = io(this.config.url, socketOptions);
        
        this.setupEventListeners();
        
        this.socket.once('connect', () => {
          this.reconnectAttempts = 0;
          this.lastConnected = new Date();
          this.lastError = undefined;
          this.startPingMonitoring();
          this.updateStatus();
          console.log('WebSocket connected successfully');
          resolve();
        });

        this.socket.once('connect_error', (error: Error) => {
          this.lastError = error.message;
          this.updateStatus();
          console.error('WebSocket connection error:', error);
          this.scheduleReconnect();
          reject(error);
        });

      } catch (error) {
        this.lastError = (error as Error).message;
        this.updateStatus();
        reject(error);
      }
    });
  }

  /**
   * Disconnect from WebSocket server
   */
  public disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    if (this.pingInterval) {
      clearInterval(this.pingInterval);
      this.pingInterval = null;
    }

    if (this.socket) {
      this.socket.removeAllListeners();
      this.socket.disconnect();
      this.socket = null;
    }

    this.updateStatus();
    console.log('WebSocket disconnected');
  }

  /**
   * Send message through WebSocket
   */
  public send<T = any>(event: string, data: T): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.socket?.connected) {
        reject(new Error('WebSocket not connected'));
        return;
      }

      try {
        this.socket.emit(event, data, (response: any) => {
          if (response?.error) {
            reject(new Error(response.error));
          } else {
            resolve();
          }
        });
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Subscribe to specific WebSocket events
   */
  public on<T = any>(event: string, handler: WebSocketEventHandler<T>): void {
    if (!this.messageHandlers.has(event)) {
      this.messageHandlers.set(event, []);
    }
    this.messageHandlers.get(event)!.push(handler);

    // Also register with socket if connected
    if (this.socket) {
      this.socket.on(event, handler);
    }
  }

  /**
   * Unsubscribe from WebSocket events
   */
  public off<T = any>(event: string, handler?: WebSocketEventHandler<T>): void {
    if (handler) {
      const handlers = this.messageHandlers.get(event);
      if (handlers) {
        const index = handlers.indexOf(handler);
        if (index > -1) {
          handlers.splice(index, 1);
        }
      }
      if (this.socket) {
        this.socket.off(event, handler);
      }
    } else {
      this.messageHandlers.delete(event);
      if (this.socket) {
        this.socket.removeAllListeners(event);
      }
    }
  }

  /**
   * Subscribe to connection status changes
   */
  public onStatusChange(handler: WebSocketStatusHandler): void {
    this.statusHandlers.push(handler);
  }

  /**
   * Subscribe to errors
   */
  public onError(handler: WebSocketErrorHandler): void {
    this.errorHandlers.push(handler);
  }

  /**
   * Get current connection status
   */
  public getStatus(): WebSocketConnectionStatus {
    return {
      isConnected: this.socket?.connected ?? false,
      isReconnecting: !!this.reconnectTimer,
      reconnectAttempts: this.reconnectAttempts,
      lastConnected: this.lastConnected,
      lastError: this.lastError,
      latency: this.latency,
    };
  }

  /**
   * Join a specific room/channel
   */
  public joinChannel(channel: string): Promise<void> {
    return this.send('join', { channel });
  }

  /**
   * Leave a specific room/channel
   */
  public leaveChannel(channel: string): Promise<void> {
    return this.send('leave', { channel });
  }

  /**
   * Setup event listeners for socket
   */
  private setupEventListeners(): void {
    if (!this.socket) return;

    this.socket.on('disconnect', (reason: string) => {
      console.log('WebSocket disconnected:', reason);
      this.stopPingMonitoring();
      this.updateStatus();
      
      // Automatic reconnection for certain reasons
      if (reason === 'io server disconnect') {
        // Server initiated disconnect, don't auto-reconnect
        return;
      }
      
      this.scheduleReconnect();
    });

    this.socket.on('reconnect_attempt', () => {
      console.log('WebSocket attempting to reconnect...');
      this.updateStatus();
    });

    this.socket.on('error', (error: Error) => {
      console.error('WebSocket error:', error);
      this.lastError = error.message;
      this.updateStatus();
      this.errorHandlers.forEach(handler => handler(error));
    });

    this.socket.on('pong', (latency: number) => {
      this.latency = latency;
      this.updateStatus();
    });

    // Register all previously subscribed message handlers
    this.messageHandlers.forEach((handlers, event) => {
      handlers.forEach(handler => {
        this.socket!.on(event, handler);
      });
    });
  }

  /**
   * Schedule reconnection with exponential backoff
   */
  private scheduleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn('Max reconnection attempts reached');
      this.lastError = 'Max reconnection attempts reached';
      this.updateStatus();
      return;
    }

    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
    }

    const delay = Math.min(
      this.reconnectDelay * Math.pow(2, this.reconnectAttempts),
      30000 // Max 30 seconds
    );

    console.log(`Scheduling reconnect in ${delay}ms (attempt ${this.reconnectAttempts + 1})`);
    
    this.reconnectTimer = setTimeout(() => {
      this.reconnectAttempts++;
      this.reconnectTimer = null;
      this.connect().catch(() => {
        // Reconnection failed, will be scheduled again
      });
    }, delay);

    this.updateStatus();
  }

  /**
   * Start ping monitoring for latency measurement
   */
  private startPingMonitoring(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval);
    }

    this.pingInterval = setInterval(() => {
      if (this.socket?.connected) {
        const start = Date.now();
        this.socket.emit('ping', start);
      }
    }, 30000); // Ping every 30 seconds
  }

  /**
   * Stop ping monitoring
   */
  private stopPingMonitoring(): void {
    if (this.pingInterval) {
      clearInterval(this.pingInterval);
      this.pingInterval = null;
    }
  }

  /**
   * Update status and notify handlers
   */
  private updateStatus(): void {
    const status = this.getStatus();
    this.statusHandlers.forEach(handler => handler(status));
  }
}

// Singleton instance for global use
let websocketService: WebSocketService | null = null;

/**
 * Initialize WebSocket service with configuration
 */
export const initializeWebSocket = (config: WebSocketConfig): WebSocketService => {
  if (websocketService) {
    websocketService.disconnect();
  }
  websocketService = new WebSocketService(config);
  return websocketService;
};

/**
 * Get the current WebSocket service instance
 */
export const getWebSocketService = (): WebSocketService | null => {
  return websocketService;
};

/**
 * Mock WebSocket service for development
 */
export class MockWebSocketService extends WebSocketService {
  private mockConnected = false;
  private mockHandlers: Map<string, WebSocketEventHandler[]> = new Map();

  constructor(config: Partial<WebSocketConfig> = {}) {
    super({ url: 'mock://localhost', ...config });
  }

  public async connect(): Promise<void> {
    await new Promise(resolve => setTimeout(resolve, 500)); // Simulate connection delay
    this.mockConnected = true;
    console.log('Mock WebSocket connected');
    return Promise.resolve();
  }

  public disconnect(): void {
    this.mockConnected = false;
    console.log('Mock WebSocket disconnected');
  }

  public async send<T = any>(event: string, data: T): Promise<void> {
    if (!this.mockConnected) {
      throw new Error('Mock WebSocket not connected');
    }
    console.log('Mock WebSocket send:', event, data);
    return Promise.resolve();
  }

  public on<T = any>(event: string, handler: WebSocketEventHandler<T>): void {
    if (!this.mockHandlers.has(event)) {
      this.mockHandlers.set(event, []);
    }
    this.mockHandlers.get(event)!.push(handler);
  }

  public off<T = any>(event: string, handler?: WebSocketEventHandler<T>): void {
    if (handler) {
      const handlers = this.mockHandlers.get(event);
      if (handlers) {
        const index = handlers.indexOf(handler);
        if (index > -1) {
          handlers.splice(index, 1);
        }
      }
    } else {
      this.mockHandlers.delete(event);
    }
  }

  public getStatus(): WebSocketConnectionStatus {
    return {
      isConnected: this.mockConnected,
      isReconnecting: false,
      reconnectAttempts: 0,
      lastConnected: new Date(),
      latency: 50,
    };
  }

  // Method to simulate receiving messages for testing
  public simulateMessage<T = any>(event: string, data: T): void {
    const handlers = this.mockHandlers.get(event);
    if (handlers) {
      handlers.forEach(handler => handler(data));
    }
  }
}

export default WebSocketService;