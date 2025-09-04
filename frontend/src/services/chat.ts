import { 
  ChatMessage, 
  ChatConversation, 
  ChatParticipant, 
  ChatSendRequest, 
  ChatHistoryRequest, 
  ChatSearchRequest,
  MockChatData,
  ChatAttachment
} from '../types/chat';
import { ApiResponse } from '../types';

// Mock data for development
const MOCK_PARTICIPANTS: ChatParticipant[] = [
  {
    id: 'user-1',
    name: 'John Doe',
    avatar: 'https://ui-avatars.com/api/?name=John+Doe',
    role: 'customer',
    isOnline: true,
    lastSeen: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5 minutes ago
  },
  {
    id: 'user-2', 
    name: 'Ocean Store Support',
    avatar: 'https://ui-avatars.com/api/?name=Ocean+Support',
    role: 'support',
    isOnline: true,
  },
  {
    id: 'user-3',
    name: 'Store Owner',
    avatar: 'https://ui-avatars.com/api/?name=Store+Owner',
    role: 'store_owner',
    isOnline: false,
    lastSeen: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(), // 2 hours ago
  },
  {
    id: 'user-4',
    name: 'Admin User',
    avatar: 'https://ui-avatars.com/api/?name=Admin+User',
    role: 'admin',
    isOnline: true,
  },
];

const MOCK_MESSAGES: Record<string, ChatMessage[]> = {
  'conv-1': [
    {
      id: 'msg-1',
      conversationId: 'conv-1',
      senderId: 'user-2',
      senderName: 'Ocean Store Support',
      senderAvatar: 'https://ui-avatars.com/api/?name=Ocean+Support',
      message: 'Hello! How can I help you today?',
      messageType: 'text',
      timestamp: new Date(Date.now() - 60 * 60 * 1000).toISOString(), // 1 hour ago
      status: 'read',
    },
    {
      id: 'msg-2',
      conversationId: 'conv-1',
      senderId: 'user-1',
      senderName: 'John Doe',
      senderAvatar: 'https://ui-avatars.com/api/?name=John+Doe',
      message: 'I have a question about my recent order. Can you help me track it?',
      messageType: 'text',
      timestamp: new Date(Date.now() - 58 * 60 * 1000).toISOString(), // 58 minutes ago
      status: 'read',
    },
    {
      id: 'msg-3',
      conversationId: 'conv-1',
      senderId: 'user-2',
      senderName: 'Ocean Store Support',
      senderAvatar: 'https://ui-avatars.com/api/?name=Ocean+Support',
      message: 'Of course! Could you please provide me with your order number?',
      messageType: 'text',
      timestamp: new Date(Date.now() - 55 * 60 * 1000).toISOString(), // 55 minutes ago
      status: 'read',
    },
    {
      id: 'msg-4',
      conversationId: 'conv-1',
      senderId: 'user-1',
      senderName: 'John Doe',
      senderAvatar: 'https://ui-avatars.com/api/?name=John+Doe',
      message: 'The order number is #12345. I ordered it 3 days ago but haven\'t received any tracking information.',
      messageType: 'text',
      timestamp: new Date(Date.now() - 5 * 60 * 1000).toISOString(), // 5 minutes ago
      status: 'delivered',
    },
  ],
  'conv-2': [
    {
      id: 'msg-5',
      conversationId: 'conv-2',
      senderId: 'user-3',
      senderName: 'Store Owner',
      senderAvatar: 'https://ui-avatars.com/api/?name=Store+Owner',
      message: 'Thank you for your inquiry about bulk orders. We can definitely accommodate that.',
      messageType: 'text',
      timestamp: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(), // 3 hours ago
      status: 'read',
    },
    {
      id: 'msg-6',
      conversationId: 'conv-2',
      senderId: 'user-1',
      senderName: 'John Doe',
      senderAvatar: 'https://ui-avatars.com/api/?name=John+Doe',
      message: 'Great! What kind of discounts are available for orders over 100 units?',
      messageType: 'text',
      timestamp: new Date(Date.now() - 2.5 * 60 * 60 * 1000).toISOString(), // 2.5 hours ago
      status: 'read',
    },
  ],
};

const MOCK_CONVERSATIONS: ChatConversation[] = [
  {
    id: 'conv-1',
    type: 'support',
    participants: [MOCK_PARTICIPANTS[0], MOCK_PARTICIPANTS[1]],
    lastMessage: MOCK_MESSAGES['conv-1'][MOCK_MESSAGES['conv-1'].length - 1],
    unreadCount: 1,
    title: 'Order Support',
    avatar: 'https://ui-avatars.com/api/?name=Ocean+Support',
    isActive: true,
    createdAt: new Date(Date.now() - 2 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 5 * 60 * 1000).toISOString(),
  },
  {
    id: 'conv-2',
    type: 'direct',
    participants: [MOCK_PARTICIPANTS[0], MOCK_PARTICIPANTS[2]],
    lastMessage: MOCK_MESSAGES['conv-2'][MOCK_MESSAGES['conv-2'].length - 1],
    unreadCount: 0,
    title: 'Store Owner Chat',
    avatar: 'https://ui-avatars.com/api/?name=Store+Owner',
    isActive: true,
    createdAt: new Date(Date.now() - 5 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 2.5 * 60 * 60 * 1000).toISOString(),
  },
];

class ChatService {
  private baseUrl: string;
  private useMockData: boolean;

  constructor(baseUrl: string = '/api', useMockData: boolean = true) {
    this.baseUrl = baseUrl;
    this.useMockData = useMockData;
  }

  /**
   * Get all conversations for the current user
   */
  async getConversations(userId: string): Promise<ApiResponse<ChatConversation[]>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 500));
      return {
        success: true,
        data: MOCK_CONVERSATIONS.filter(conv => 
          conv.participants.some(p => p.id === userId)
        ),
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/chat/conversations`, {
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
    });
    
    if (!response.ok) {
      throw new Error('Failed to fetch conversations');
    }

    return response.json();
  }

  /**
   * Get chat history for a specific conversation
   */
  async getChatHistory(request: ChatHistoryRequest): Promise<ApiResponse<ChatMessage[]>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 300));
      
      const messages = MOCK_MESSAGES[request.conversationId] || [];
      let filteredMessages = [...messages];

      // Apply pagination
      const page = request.page || 1;
      const limit = request.limit || 50;
      const start = (page - 1) * limit;
      const end = start + limit;
      
      if (request.before) {
        const beforeIndex = messages.findIndex(m => m.timestamp === request.before);
        if (beforeIndex > -1) {
          filteredMessages = messages.slice(0, beforeIndex);
        }
      }

      filteredMessages = filteredMessages.slice(start, end);

      return {
        success: true,
        data: filteredMessages.sort((a, b) => 
          new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        ),
      };
    }

    // Real API implementation
    const params = new URLSearchParams({
      page: (request.page || 1).toString(),
      limit: (request.limit || 50).toString(),
    });
    
    if (request.before) {
      params.append('before', request.before);
    }

    const response = await fetch(
      `${this.baseUrl}/chat/history/${request.conversationId}?${params}`,
      {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      }
    );
    
    if (!response.ok) {
      throw new Error('Failed to fetch chat history');
    }

    return response.json();
  }

  /**
   * Send a new message
   */
  async sendMessage(request: ChatSendRequest): Promise<ApiResponse<ChatMessage>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 200));
      
      const newMessage: ChatMessage = {
        id: `msg-${Date.now()}`,
        conversationId: request.conversationId,
        senderId: 'user-1', // Mock current user
        senderName: 'John Doe',
        senderAvatar: 'https://ui-avatars.com/api/?name=John+Doe',
        message: request.message,
        messageType: request.messageType || 'text',
        timestamp: new Date().toISOString(),
        status: 'sent',
        replyTo: request.replyTo,
      };

      // Add to mock data
      if (!MOCK_MESSAGES[request.conversationId]) {
        MOCK_MESSAGES[request.conversationId] = [];
      }
      MOCK_MESSAGES[request.conversationId].push(newMessage);

      // Update conversation's last message
      const conversation = MOCK_CONVERSATIONS.find(c => c.id === request.conversationId);
      if (conversation) {
        conversation.lastMessage = newMessage;
        conversation.updatedAt = new Date().toISOString();
      }

      return {
        success: true,
        data: newMessage,
      };
    }

    // Real API implementation
    const formData = new FormData();
    formData.append('message', request.message);
    formData.append('messageType', request.messageType || 'text');
    
    if (request.replyTo) {
      formData.append('replyTo', request.replyTo);
    }

    if (request.attachments) {
      request.attachments.forEach((file, index) => {
        formData.append(`attachment_${index}`, file);
      });
    }

    const response = await fetch(`${this.baseUrl}/chat/send`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: formData,
    });
    
    if (!response.ok) {
      throw new Error('Failed to send message');
    }

    return response.json();
  }

  /**
   * Start a new conversation
   */
  async startConversation(participantId: string, type: 'direct' | 'support' = 'direct'): Promise<ApiResponse<ChatConversation>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 300));

      const participant = MOCK_PARTICIPANTS.find(p => p.id === participantId);
      if (!participant) {
        throw new Error('Participant not found');
      }

      const newConversation: ChatConversation = {
        id: `conv-${Date.now()}`,
        type,
        participants: [MOCK_PARTICIPANTS[0], participant], // Current user + selected participant
        unreadCount: 0,
        title: type === 'support' ? 'Support Chat' : participant.name,
        avatar: participant.avatar,
        isActive: true,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };

      MOCK_CONVERSATIONS.push(newConversation);
      MOCK_MESSAGES[newConversation.id] = [];

      return {
        success: true,
        data: newConversation,
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/chat/conversation`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ participantId, type }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to start conversation');
    }

    return response.json();
  }

  /**
   * Mark messages as read
   */
  async markAsRead(conversationId: string, messageId?: string): Promise<ApiResponse<void>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const messages = MOCK_MESSAGES[conversationId] || [];
      const conversation = MOCK_CONVERSATIONS.find(c => c.id === conversationId);
      
      if (messageId) {
        const message = messages.find(m => m.id === messageId);
        if (message) {
          message.status = 'read';
          message.readAt = new Date().toISOString();
        }
      } else {
        // Mark all messages as read
        messages.forEach(message => {
          message.status = 'read';
          message.readAt = new Date().toISOString();
        });
        
        if (conversation) {
          conversation.unreadCount = 0;
        }
      }

      return { success: true, data: undefined };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/chat/read`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify({ conversationId, messageId }),
    });
    
    if (!response.ok) {
      throw new Error('Failed to mark as read');
    }

    return response.json();
  }

  /**
   * Search messages
   */
  async searchMessages(request: ChatSearchRequest): Promise<ApiResponse<ChatMessage[]>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 400));
      
      let allMessages: ChatMessage[] = [];
      
      if (request.conversationId) {
        allMessages = MOCK_MESSAGES[request.conversationId] || [];
      } else {
        // Search across all conversations
        allMessages = Object.values(MOCK_MESSAGES).flat();
      }

      const filteredMessages = allMessages.filter(message => {
        const matchesQuery = message.message.toLowerCase().includes(request.query.toLowerCase());
        const matchesType = !request.messageType || message.messageType === request.messageType;
        const matchesDateFrom = !request.dateFrom || new Date(message.timestamp) >= new Date(request.dateFrom);
        const matchesDateTo = !request.dateTo || new Date(message.timestamp) <= new Date(request.dateTo);
        
        return matchesQuery && matchesType && matchesDateFrom && matchesDateTo;
      });

      return {
        success: true,
        data: filteredMessages.slice(0, 100), // Limit results
      };
    }

    // Real API implementation
    const response = await fetch(`${this.baseUrl}/chat/search`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      throw new Error('Failed to search messages');
    }

    return response.json();
  }

  /**
   * Upload attachment
   */
  async uploadAttachment(file: File): Promise<ApiResponse<ChatAttachment>> {
    if (this.useMockData) {
      // Simulate API delay
      await new Promise(resolve => setTimeout(resolve, 1000));
      
      const attachment: ChatAttachment = {
        id: `att-${Date.now()}`,
        name: file.name,
        type: file.type.startsWith('image/') ? 'image' : 'document',
        url: URL.createObjectURL(file), // Create temporary URL for preview
        size: file.size,
        mimeType: file.type,
      };

      return {
        success: true,
        data: attachment,
      };
    }

    // Real API implementation
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`${this.baseUrl}/chat/upload`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${localStorage.getItem('token')}`,
      },
      body: formData,
    });
    
    if (!response.ok) {
      throw new Error('Failed to upload attachment');
    }

    return response.json();
  }

  /**
   * Get mock data for testing
   */
  getMockData(): MockChatData {
    return {
      conversations: MOCK_CONVERSATIONS,
      messages: MOCK_MESSAGES,
      users: MOCK_PARTICIPANTS,
    };
  }

  /**
   * Set mock data mode
   */
  setMockMode(useMock: boolean): void {
    this.useMockData = useMock;
  }

  /**
   * Simulate receiving a message (for testing)
   */
  simulateIncomingMessage(conversationId: string, senderId: string, message: string): ChatMessage {
    const sender = MOCK_PARTICIPANTS.find(p => p.id === senderId);
    if (!sender) {
      throw new Error('Sender not found');
    }

    const newMessage: ChatMessage = {
      id: `msg-${Date.now()}`,
      conversationId,
      senderId,
      senderName: sender.name,
      senderAvatar: sender.avatar,
      message,
      messageType: 'text',
      timestamp: new Date().toISOString(),
      status: 'delivered',
    };

    // Add to mock data
    if (!MOCK_MESSAGES[conversationId]) {
      MOCK_MESSAGES[conversationId] = [];
    }
    MOCK_MESSAGES[conversationId].push(newMessage);

    // Update conversation
    const conversation = MOCK_CONVERSATIONS.find(c => c.id === conversationId);
    if (conversation) {
      conversation.lastMessage = newMessage;
      conversation.updatedAt = new Date().toISOString();
      conversation.unreadCount++;
    }

    return newMessage;
  }
}

// Create singleton instance
const chatService = new ChatService();

export default chatService;