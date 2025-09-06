import React, { useState, useRef, useEffect } from 'react';
import { ChatMessage, ChatConversation, ChatTypingIndicator } from '../../types/chat';
import { useChat } from '../../hooks/useChat';
import Button from '../ui/Button';
import Modal from '../ui/Modal';
import LoadingSpinner from '../ui/LoadingSpinner';
import { cn } from '../../utils/cn';

interface ChatWidgetProps {
  className?: string;
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  theme?: 'light' | 'dark';
  autoOpen?: boolean;
  defaultConversationId?: string;
}

/**
 * Chat widget component with real-time messaging functionality
 */
export const ChatWidget: React.FC<ChatWidgetProps> = ({
  className,
  position = 'bottom-right',
  theme = 'light',
  autoOpen = false,
  defaultConversationId,
}) => {
  const [isOpen, setIsOpen] = useState(autoOpen);
  const [messageText, setMessageText] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const chat = useChat({
    autoLoadConversations: true,
    useMockService: true,
  });

  // Auto-select default conversation
  useEffect(() => {
    if (defaultConversationId && chat.conversations.length > 0 && !chat.activeConversation) {
      chat.setActiveConversation(defaultConversationId);
    }
  }, [defaultConversationId, chat.conversations.length, chat.activeConversation, chat.setActiveConversation]);

  // Auto-select first conversation if none selected
  useEffect(() => {
    if (chat.conversations.length > 0 && !chat.activeConversation) {
      chat.setActiveConversation(chat.conversations[0].id);
    }
  }, [chat.conversations.length, chat.activeConversation, chat.setActiveConversation]);

  // Auto-scroll to bottom when new messages arrive
  useEffect(() => {
    if (messagesEndRef.current) {
      messagesEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [chat.messages, chat.activeConversation?.id]);

  // Focus input when widget opens
  useEffect(() => {
    if (isOpen && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isOpen]);

  const handleSendMessage = async () => {
    if (!messageText.trim() || !chat.activeConversation) return;

    const message = messageText.trim();
    setMessageText('');
    
    // Stop typing indicator
    if (isTyping) {
      chat.stopTyping(chat.activeConversation.id);
      setIsTyping(false);
    }

    try {
      await chat.sendMessage({
        conversationId: chat.activeConversation.id,
        message,
        messageType: 'text',
      });
    } catch (error) {
      console.error('Failed to send message:', error);
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setMessageText(value);

    if (!chat.activeConversation) return;

    // Handle typing indicator
    if (value.trim() && !isTyping) {
      setIsTyping(true);
      chat.startTyping(chat.activeConversation.id);
    }

    // Reset typing timeout
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
    }

    typingTimeoutRef.current = setTimeout(() => {
      if (isTyping) {
        setIsTyping(false);
        chat.stopTyping(chat.activeConversation.id);
      }
    }, 1000);
  };

  const handleKeyPress = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const formatTimestamp = (timestamp: string) => {
    return new Date(timestamp).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const renderTypingIndicator = () => {
    const typingInConversation = chat.typingUsers.filter(
      user => user.conversationId === chat.activeConversation?.id
    );

    if (typingInConversation.length === 0) return null;

    return (
      <div className="flex items-center gap-2 px-4 py-2 text-sm text-gray-500">
        <div className="flex gap-1">
          <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
          <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
          <div className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
        </div>
        <span>
          {typingInConversation.length === 1
            ? `${typingInConversation[0].userName} is typing...`
            : `${typingInConversation.length} people are typing...`}
        </span>
      </div>
    );
  };

  const renderMessage = (message: ChatMessage) => {
    const isOwn = message.senderId === 'user-1'; // Mock current user ID
    const isSystem = message.messageType === 'system';

    if (isSystem) {
      return (
        <div key={message.id} className="flex justify-center py-2">
          <div className="bg-gray-100 text-gray-600 text-xs px-3 py-1 rounded-full">
            {message.message}
          </div>
        </div>
      );
    }

    return (
      <div
        key={message.id}
        className={cn(
          'flex gap-3 py-2 px-4',
          isOwn ? 'flex-row-reverse' : 'flex-row'
        )}
      >
        {!isOwn && (
          <img
            src={message.senderAvatar || `https://ui-avatars.com/api/?name=${message.senderName}`}
            alt={message.senderName}
            className="w-8 h-8 rounded-full flex-shrink-0"
          />
        )}
        
        <div className={cn('flex flex-col gap-1', isOwn ? 'items-end' : 'items-start')}>
          {!isOwn && (
            <span className="text-xs text-gray-600 font-medium">
              {message.senderName}
            </span>
          )}
          
          <div
            className={cn(
              'max-w-xs lg:max-w-md px-3 py-2 rounded-lg text-sm',
              isOwn
                ? 'bg-blue-500 text-white rounded-br-sm'
                : 'bg-gray-100 text-gray-900 rounded-bl-sm'
            )}
          >
            {message.message}
          </div>
          
          <div className="flex items-center gap-2 text-xs text-gray-500">
            <span>{formatTimestamp(message.timestamp)}</span>
            {isOwn && (
              <span className="flex items-center gap-1">
                {message.status === 'sending' && (
                  <div className="w-3 h-3">
                    <LoadingSpinner size="xs" />
                  </div>
                )}
                {message.status === 'sent' && '✓'}
                {message.status === 'delivered' && '✓✓'}
                {message.status === 'read' && (
                  <span className="text-blue-500">✓✓</span>
                )}
                {message.status === 'failed' && (
                  <span className="text-red-500">⚠</span>
                )}
              </span>
            )}
          </div>
        </div>
      </div>
    );
  };

  const positionClasses = {
    'bottom-right': 'bottom-4 right-4',
    'bottom-left': 'bottom-4 left-4',
    'top-right': 'top-4 right-4',
    'top-left': 'top-4 left-4',
  };

  const themeClasses = {
    light: 'bg-white border-gray-200 text-gray-900',
    dark: 'bg-gray-800 border-gray-700 text-white',
  };

  if (!isOpen) {
    return (
      <div className={cn('fixed z-50', positionClasses[position], className)}>
        <Button
          onClick={() => setIsOpen(true)}
          className="w-14 h-14 rounded-full shadow-lg hover:shadow-xl transition-shadow"
          size="lg"
        >
          <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
            <path fillRule="evenodd" d="M18 10c0 3.866-3.582 7-8 7a8.841 8.841 0 01-4.083-.98L2 17l1.338-3.123C2.493 12.767 2 11.434 2 10c0-3.866 3.582-7 8-7s8 3.134 8 7zM7 9H5v2h2V9zm8 0h-2v2h2V9zM9 9h2v2H9V9z" clipRule="evenodd" />
          </svg>
          {chat.badge?.unread > 0 && (
            <span className="absolute -top-2 -right-2 bg-red-500 text-white text-xs font-bold rounded-full w-6 h-6 flex items-center justify-center">
              {chat.badge.unread > 99 ? '99+' : chat.badge.unread}
            </span>
          )}
        </Button>
      </div>
    );
  }

  const currentMessages = chat.activeConversation ? chat.messages[chat.activeConversation.id] || [] : [];

  return (
    <div className={cn('fixed z-50', positionClasses[position], className)}>
      <div
        className={cn(
          'w-80 h-96 rounded-lg border shadow-xl flex flex-col',
          themeClasses[theme]
        )}
      >
        {/* Header */}
        <div className="flex items-center justify-between p-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            {chat.activeConversation?.avatar && (
              <img
                src={chat.activeConversation.avatar}
                alt={chat.activeConversation.title}
                className="w-8 h-8 rounded-full"
              />
            )}
            <div>
              <h3 className="font-medium text-sm">
                {chat.activeConversation?.title || 'Chat'}
              </h3>
              <div className="flex items-center gap-2">
                <div
                  className={cn(
                    'w-2 h-2 rounded-full',
                    chat.isConnected ? 'bg-green-500' : 'bg-red-500'
                  )}
                />
                <span className="text-xs text-gray-500">
                  {chat.isConnected ? 'Connected' : 'Disconnected'}
                </span>
              </div>
            </div>
          </div>
          
          <div className="flex items-center gap-2">
            {/* Conversation selector */}
            {chat.conversations.length > 1 && (
              <select
                value={chat.activeConversation?.id || ''}
                onChange={(e) => chat.setActiveConversation(e.target.value)}
                className="text-xs border border-gray-300 rounded px-2 py-1"
              >
                {chat.conversations.map((conv) => (
                  <option key={conv.id} value={conv.id}>
                    {conv.title}
                  </option>
                ))}
              </select>
            )}
            
            <Button
              onClick={() => setIsOpen(false)}
              variant="ghost"
              size="sm"
              className="p-1"
            >
              ✕
            </Button>
          </div>
        </div>

        {/* Messages */}
        <div className="flex-1 overflow-y-auto">
          {chat.isLoading && currentMessages.length === 0 ? (
            <div className="flex items-center justify-center h-full">
              <LoadingSpinner />
            </div>
          ) : currentMessages.length === 0 ? (
            <div className="flex items-center justify-center h-full text-gray-500 text-sm">
              No messages yet. Start a conversation!
            </div>
          ) : (
            <div className="py-2">
              {currentMessages.map(renderMessage)}
              {renderTypingIndicator()}
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>

        {/* Input */}
        <div className="p-4 border-t border-gray-200">
          <div className="flex gap-2">
            <input
              ref={inputRef}
              type="text"
              value={messageText}
              onChange={handleInputChange}
              onKeyPress={handleKeyPress}
              placeholder="Type a message..."
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={!chat.isConnected || !chat.activeConversation}
            />
            <Button
              onClick={handleSendMessage}
              disabled={!messageText.trim() || !chat.isConnected || !chat.activeConversation}
              size="sm"
              className="px-3"
            >
              Send
            </Button>
          </div>
          
          {chat.error && (
            <div className="mt-2 text-xs text-red-600">
              {chat.error}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default ChatWidget;