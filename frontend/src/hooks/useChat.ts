import { useState, useEffect, useCallback, useRef } from 'react';
import { 
  ChatMessage, 
  ChatConversation, 
  ChatParticipant,
  ChatSendRequest,
  ChatHistoryRequest,
  ChatSearchRequest,
  ChatTypingIndicator,
  ChatConnectionStatus,
  ChatWebSocketMessage
} from '../types/chat';
import { useWebSocket, UseWebSocketOptions } from './useWebSocket';
import chatService from '../services/chat';

export interface UseChatOptions extends UseWebSocketOptions {
  userId?: string;
  autoLoadConversations?: boolean;
  messagePageSize?: number;
  typingTimeout?: number;
}

export interface UseChatReturn {
  // State
  conversations: ChatConversation[];
  activeConversation: ChatConversation | null;
  messages: Record<string, ChatMessage[]>;
  typingUsers: ChatTypingIndicator[];
  connectionStatus: ChatConnectionStatus;
  isLoading: boolean;
  error: string | null;

  // Actions
  loadConversations: () => Promise<void>;
  setActiveConversation: (conversationId: string) => void;
  sendMessage: (request: ChatSendRequest) => Promise<void>;
  startConversation: (participantId: string, type?: 'direct' | 'support') => Promise<string>;
  loadMessageHistory: (conversationId: string, options?: { page?: number; limit?: number }) => Promise<void>;
  markAsRead: (conversationId: string, messageId?: string) => Promise<void>;
  startTyping: (conversationId: string) => void;
  stopTyping: (conversationId: string) => void;
  searchMessages: (request: ChatSearchRequest) => Promise<ChatMessage[]>;
  uploadAttachment: (file: File) => Promise<string>;

  // WebSocket connection
  isConnected: boolean;
  connect: () => Promise<void>;
  disconnect: () => void;
}

/**
 * Custom hook for chat functionality with real-time updates
 */
export const useChat = (options: UseChatOptions = {}): UseChatReturn => {
  const {
    userId = 'user-1', // Default mock user
    autoLoadConversations = true,
    messagePageSize = 50,
    typingTimeout = 3000,
    ...websocketOptions
  } = options;

  // State
  const [conversations, setConversations] = useState<ChatConversation[]>([]);
  const [activeConversation, setActiveConversationState] = useState<ChatConversation | null>(null);
  const [messages, setMessages] = useState<Record<string, ChatMessage[]>>({});
  const [typingUsers, setTypingUsers] = useState<ChatTypingIndicator[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // WebSocket connection
  const websocket = useWebSocket({
    autoConnect: true,
    ...websocketOptions,
  });

  // Refs
  const typingTimersRef = useRef<Map<string, NodeJS.Timeout>>(new Map());
  const messageLoadingRef = useRef<Set<string>>(new Set());

  // Connection status
  const connectionStatus: ChatConnectionStatus = {
    isConnected: websocket.isConnected,
    isReconnecting: websocket.isReconnecting,
    lastConnected: websocket.connectionStatus.lastConnected?.toISOString(),
    reconnectAttempts: websocket.connectionStatus.reconnectAttempts,
    latency: websocket.connectionStatus.latency,
  };

  // Load conversations
  const loadConversations = useCallback(async (): Promise<void> => {
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await chatService.getConversations(userId);
      if (response.success) {
        setConversations(response.data);
      } else {
        setError(response.message || 'Failed to load conversations');
      }
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  // Set active conversation
  const setActiveConversation = useCallback((conversationId: string): void => {
    const conversation = conversations.find(c => c.id === conversationId);
    if (conversation) {
      setActiveConversationState(conversation);
      
      // Load message history if not already loaded
      if (!messages[conversationId]) {
        loadMessageHistory(conversationId);
      }

      // Join WebSocket channel for this conversation
      if (websocket.isConnected) {
        websocket.joinChannel(`chat/${conversationId}`);
      }

      // Mark conversation as read
      markAsRead(conversationId);
    }
  }, [conversations, messages, websocket.isConnected, websocket.joinChannel]);

  // Send message
  const sendMessage = useCallback(async (request: ChatSendRequest): Promise<void> => {
    setError(null);

    try {
      // Optimistic update - add message immediately
      const tempMessage: ChatMessage = {
        id: `temp-${Date.now()}`,
        conversationId: request.conversationId,
        senderId: userId,
        senderName: 'You', // Will be updated from server response
        message: request.message,
        messageType: request.messageType || 'text',
        timestamp: new Date().toISOString(),
        status: 'sending',
        replyTo: request.replyTo,
      };

      setMessages(prev => ({
        ...prev,
        [request.conversationId]: [...(prev[request.conversationId] || []), tempMessage],
      }));

      // Send through service
      const response = await chatService.sendMessage(request);
      
      if (response.success) {
        // Replace temp message with server response
        setMessages(prev => ({
          ...prev,
          [request.conversationId]: prev[request.conversationId].map(msg =>
            msg.id === tempMessage.id ? response.data : msg
          ),
        }));

        // Update conversation's last message
        setConversations(prev =>
          prev.map(conv =>
            conv.id === request.conversationId
              ? { ...conv, lastMessage: response.data, updatedAt: response.data.timestamp }
              : conv
          )
        );

        // Send through WebSocket for real-time delivery
        if (websocket.isConnected) {
          websocket.send('chat_message', {
            type: 'message',
            conversationId: request.conversationId,
            message: response.data,
            timestamp: new Date().toISOString(),
          });
        }
      } else {
        // Mark message as failed
        setMessages(prev => ({
          ...prev,
          [request.conversationId]: prev[request.conversationId].map(msg =>
            msg.id === tempMessage.id ? { ...msg, status: 'failed' } : msg
          ),
        }));
        setError(response.message || 'Failed to send message');
      }
    } catch (err) {
      setError((err as Error).message);
      
      // Mark message as failed
      setMessages(prev => ({
        ...prev,
        [request.conversationId]: prev[request.conversationId].map(msg =>
          msg.status === 'sending' ? { ...msg, status: 'failed' } : msg
        ),
      }));
    }
  }, [userId, websocket.isConnected, websocket.send]);

  // Start new conversation
  const startConversation = useCallback(async (
    participantId: string, 
    type: 'direct' | 'support' = 'direct'
  ): Promise<string> => {
    setError(null);
    
    try {
      const response = await chatService.startConversation(participantId, type);
      
      if (response.success) {
        const newConversation = response.data;
        setConversations(prev => [newConversation, ...prev]);
        setActiveConversationState(newConversation);
        setMessages(prev => ({ ...prev, [newConversation.id]: [] }));
        
        // Join WebSocket channel
        if (websocket.isConnected) {
          websocket.joinChannel(`chat/${newConversation.id}`);
        }
        
        return newConversation.id;
      } else {
        setError(response.message || 'Failed to start conversation');
        throw new Error(response.message || 'Failed to start conversation');
      }
    } catch (err) {
      setError((err as Error).message);
      throw err;
    }
  }, [websocket.isConnected, websocket.joinChannel]);

  // Load message history
  const loadMessageHistory = useCallback(async (
    conversationId: string,
    options: { page?: number; limit?: number } = {}
  ): Promise<void> => {
    if (messageLoadingRef.current.has(conversationId)) return;
    
    messageLoadingRef.current.add(conversationId);
    setError(null);

    try {
      const request: ChatHistoryRequest = {
        conversationId,
        page: options.page || 1,
        limit: options.limit || messagePageSize,
      };

      const response = await chatService.getChatHistory(request);
      
      if (response.success) {
        setMessages(prev => {
          const existingMessages = prev[conversationId] || [];
          const newMessages = response.data;
          
          // Merge messages, avoiding duplicates
          const messageMap = new Map();
          [...existingMessages, ...newMessages].forEach(msg => {
            messageMap.set(msg.id, msg);
          });
          
          return {
            ...prev,
            [conversationId]: Array.from(messageMap.values()).sort((a, b) =>
              new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
            ),
          };
        });
      } else {
        setError(response.message || 'Failed to load message history');
      }
    } catch (err) {
      setError((err as Error).message);
    } finally {
      messageLoadingRef.current.delete(conversationId);
    }
  }, [messagePageSize]);

  // Mark messages as read
  const markAsRead = useCallback(async (
    conversationId: string,
    messageId?: string
  ): Promise<void> => {
    try {
      const response = await chatService.markAsRead(conversationId, messageId);
      
      if (response.success) {
        // Update local state
        setMessages(prev => ({
          ...prev,
          [conversationId]: prev[conversationId]?.map(msg =>
            !messageId || msg.id === messageId
              ? { ...msg, status: 'read', readAt: new Date().toISOString() }
              : msg
          ) || [],
        }));

        setConversations(prev =>
          prev.map(conv =>
            conv.id === conversationId
              ? { ...conv, unreadCount: messageId ? Math.max(0, conv.unreadCount - 1) : 0 }
              : conv
          )
        );

        // Send read receipt through WebSocket
        if (websocket.isConnected) {
          websocket.send('chat_read_receipt', {
            type: 'read_receipt',
            conversationId,
            messageId,
            timestamp: new Date().toISOString(),
          });
        }
      }
    } catch (err) {
      console.error('Failed to mark as read:', err);
    }
  }, [websocket.isConnected, websocket.send]);

  // Start typing indicator
  const startTyping = useCallback((conversationId: string): void => {
    if (!websocket.isConnected) return;

    // Clear existing typing timer
    const existingTimer = typingTimersRef.current.get(conversationId);
    if (existingTimer) {
      clearTimeout(existingTimer);
    }

    // Send typing start event
    websocket.send('chat_typing', {
      type: 'typing_start',
      conversationId,
      userId,
      userName: 'You',
      timestamp: new Date().toISOString(),
    });

    // Set timer to automatically stop typing
    const timer = setTimeout(() => {
      stopTyping(conversationId);
    }, typingTimeout);
    
    typingTimersRef.current.set(conversationId, timer);
  }, [websocket.isConnected, websocket.send, userId, typingTimeout]);

  // Stop typing indicator
  const stopTyping = useCallback((conversationId: string): void => {
    if (!websocket.isConnected) return;

    // Clear timer
    const existingTimer = typingTimersRef.current.get(conversationId);
    if (existingTimer) {
      clearTimeout(existingTimer);
      typingTimersRef.current.delete(conversationId);
    }

    // Send typing stop event
    websocket.send('chat_typing', {
      type: 'typing_stop',
      conversationId,
      userId,
      timestamp: new Date().toISOString(),
    });
  }, [websocket.isConnected, websocket.send, userId]);

  // Search messages
  const searchMessages = useCallback(async (
    request: ChatSearchRequest
  ): Promise<ChatMessage[]> => {
    setError(null);
    
    try {
      const response = await chatService.searchMessages(request);
      
      if (response.success) {
        return response.data;
      } else {
        setError(response.message || 'Failed to search messages');
        return [];
      }
    } catch (err) {
      setError((err as Error).message);
      return [];
    }
  }, []);

  // Upload attachment
  const uploadAttachment = useCallback(async (file: File): Promise<string> => {
    setError(null);
    
    try {
      const response = await chatService.uploadAttachment(file);
      
      if (response.success) {
        return response.data.url;
      } else {
        setError(response.message || 'Failed to upload attachment');
        throw new Error(response.message || 'Failed to upload attachment');
      }
    } catch (err) {
      setError((err as Error).message);
      throw err;
    }
  }, []);

  // WebSocket message handlers
  useEffect(() => {
    if (!websocket.isConnected) return;

    const handleChatMessage = (data: ChatWebSocketMessage) => {
      if (data.type === 'message') {
        const message = data.data as ChatMessage;
        
        setMessages(prev => ({
          ...prev,
          [message.conversationId]: [...(prev[message.conversationId] || []), message],
        }));

        setConversations(prev =>
          prev.map(conv =>
            conv.id === message.conversationId
              ? { ...conv, lastMessage: message, updatedAt: message.timestamp, unreadCount: conv.unreadCount + 1 }
              : conv
          )
        );
      }
    };

    const handleTyping = (data: ChatWebSocketMessage) => {
      if (data.type === 'typing_start') {
        const typingData = data.data as ChatTypingIndicator;
        setTypingUsers(prev => {
          const existing = prev.find(t => t.conversationId === typingData.conversationId && t.userId === typingData.userId);
          if (existing) return prev;
          return [...prev, typingData];
        });
      } else if (data.type === 'typing_stop') {
        const typingData = data.data as ChatTypingIndicator;
        setTypingUsers(prev =>
          prev.filter(t => !(t.conversationId === typingData.conversationId && t.userId === typingData.userId))
        );
      }
    };

    const handleUserStatus = (data: ChatWebSocketMessage) => {
      if (data.type === 'user_status') {
        const { userId: statusUserId, isOnline, lastSeen } = data.data;
        
        setConversations(prev =>
          prev.map(conv => ({
            ...conv,
            participants: conv.participants.map(p =>
              p.id === statusUserId
                ? { ...p, isOnline, lastSeen }
                : p
            ),
          }))
        );
      }
    };

    const unsubscribeMessage = websocket.subscribe('chat_message', handleChatMessage);
    const unsubscribeTyping = websocket.subscribe('chat_typing', handleTyping);
    const unsubscribeStatus = websocket.subscribe('user_status', handleUserStatus);

    return () => {
      unsubscribeMessage();
      unsubscribeTyping();
      unsubscribeStatus();
    };
  }, [websocket.isConnected, websocket.subscribe]);

  // Auto-load conversations on mount
  useEffect(() => {
    if (autoLoadConversations && websocket.isConnected) {
      loadConversations();
    }
  }, [autoLoadConversations, websocket.isConnected, loadConversations]);

  // Cleanup typing timers on unmount
  useEffect(() => {
    return () => {
      typingTimersRef.current.forEach(timer => clearTimeout(timer));
      typingTimersRef.current.clear();
    };
  }, []);

  return {
    // State
    conversations,
    activeConversation,
    messages,
    typingUsers,
    connectionStatus,
    isLoading,
    error,

    // Actions
    loadConversations,
    setActiveConversation,
    sendMessage,
    startConversation,
    loadMessageHistory,
    markAsRead,
    startTyping,
    stopTyping,
    searchMessages,
    uploadAttachment,

    // WebSocket connection
    isConnected: websocket.isConnected,
    connect: websocket.connect,
    disconnect: websocket.disconnect,
  };
};