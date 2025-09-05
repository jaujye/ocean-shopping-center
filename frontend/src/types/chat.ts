// Chat system types for real-time communication

export interface ChatMessage {
  id: string;
  conversationId: string;
  senderId: string;
  senderName: string;
  senderAvatar?: string;
  message: string;
  messageType: 'text' | 'image' | 'file' | 'system';
  attachments?: ChatAttachment[];
  timestamp: string;
  readAt?: string;
  editedAt?: string;
  replyTo?: string; // ID of message being replied to
  status: 'sending' | 'sent' | 'delivered' | 'read' | 'failed';
}

export interface ChatAttachment {
  id: string;
  name: string;
  type: 'image' | 'document' | 'video' | 'audio';
  url: string;
  size: number;
  mimeType: string;
}

export interface ChatConversation {
  id: string;
  type: 'direct' | 'support' | 'group';
  participants: ChatParticipant[];
  lastMessage?: ChatMessage;
  unreadCount: number;
  title: string;
  avatar?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ChatParticipant {
  id: string;
  name: string;
  avatar?: string;
  role: 'customer' | 'store_owner' | 'admin' | 'support';
  isOnline: boolean;
  lastSeen?: string;
  typingAt?: string;
}

export interface ChatTypingIndicator {
  conversationId: string;
  userId: string;
  userName: string;
  timestamp: string;
}

export interface ChatConnectionStatus {
  isConnected: boolean;
  isReconnecting: boolean;
  lastConnected?: string;
  reconnectAttempts: number;
  latency?: number;
}

// WebSocket message types for chat
export interface ChatWebSocketMessage {
  type: 'message' | 'typing_start' | 'typing_stop' | 'read_receipt' | 'user_status';
  conversationId: string;
  data: any;
  timestamp: string;
}

export interface ChatSendRequest {
  conversationId: string;
  message: string;
  messageType?: 'text' | 'image' | 'file';
  attachments?: File[];
  replyTo?: string;
}

export interface ChatHistoryRequest {
  conversationId: string;
  page?: number;
  limit?: number;
  before?: string; // timestamp
}

export interface ChatSearchRequest {
  query: string;
  conversationId?: string;
  messageType?: 'text' | 'image' | 'file';
  dateFrom?: string;
  dateTo?: string;
}

// Chat contexts and state
export interface ChatContextType {
  conversations: ChatConversation[];
  activeConversation: ChatConversation | null;
  messages: Record<string, ChatMessage[]>;
  connectionStatus: ChatConnectionStatus;
  typingUsers: ChatTypingIndicator[];
  
  // Actions
  sendMessage: (request: ChatSendRequest) => Promise<void>;
  startConversation: (participantId: string, type?: 'direct' | 'support') => Promise<string>;
  setActiveConversation: (conversationId: string) => void;
  loadMessageHistory: (conversationId: string, options?: { page?: number; limit?: number }) => Promise<void>;
  markAsRead: (conversationId: string, messageId?: string) => void;
  startTyping: (conversationId: string) => void;
  stopTyping: (conversationId: string) => void;
  searchMessages: (request: ChatSearchRequest) => Promise<ChatMessage[]>;
}

// Mock data types for development
export interface MockChatData {
  conversations: ChatConversation[];
  messages: Record<string, ChatMessage[]>;
  users: ChatParticipant[];
}