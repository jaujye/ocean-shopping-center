package com.ocean.shopping.service;

import com.ocean.shopping.dto.chat.ChatMessageRequest;
import com.ocean.shopping.dto.chat.ChatMessageResponse;
import com.ocean.shopping.dto.chat.ConversationResponse;
import com.ocean.shopping.model.entity.ChatMessage;
import com.ocean.shopping.model.entity.User;
import com.ocean.shopping.model.entity.enums.UserRole;
import com.ocean.shopping.model.entity.enums.UserStatus;
import com.ocean.shopping.repository.ChatMessageRepository;
import com.ocean.shopping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ChatService
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService Tests")
class ChatServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatService chatService;

    private User sender;
    private User receiver;
    private UUID conversationId;
    private ChatMessageRequest messageRequest;

    @BeforeEach
    void setUp() {
        // Create test users
        sender = User.builder()
                .id(UUID.randomUUID())
                .email("sender@example.com")
                .firstName("John")
                .lastName("Sender")
                .role(UserRole.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        receiver = User.builder()
                .id(UUID.randomUUID())
                .email("receiver@example.com")
                .firstName("Jane")
                .lastName("Receiver")
                .role(UserRole.STORE_OWNER)
                .status(UserStatus.ACTIVE)
                .build();

        conversationId = UUID.randomUUID();

        // Create test message request
        messageRequest = ChatMessageRequest.builder()
                .content("Hello, I have a question about your product")
                .receiverId(receiver.getId())
                .conversationId(conversationId)
                .messageType(ChatMessageRequest.MessageType.TEXT)
                .build();
    }

    @Test
    @DisplayName("Should send message successfully")
    void shouldSendMessageSuccessfully() {
        // Given
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        ChatMessage savedMessage = createTestChatMessage();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        // When
        ChatMessageResponse result = chatService.sendMessage(sender.getId(), messageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(messageRequest.getContent());
        assertThat(result.getSender().getId()).isEqualTo(sender.getId());
        assertThat(result.getReceiver().getId()).isEqualTo(receiver.getId());
        assertThat(result.getConversationId()).isEqualTo(conversationId);

        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class)); // Initial save + mark as delivered
        verify(messagingTemplate).convertAndSendToUser(
                eq(receiver.getId().toString()),
                eq("/queue/chat"),
                any(ChatMessageResponse.class)
        );
    }

    @Test
    @DisplayName("Should generate conversation ID when not provided")
    void shouldGenerateConversationIdWhenNotProvided() {
        // Given
        messageRequest.setConversationId(null);
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        ChatMessage savedMessage = createTestChatMessage();
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        // When
        ChatMessageResponse result = chatService.sendMessage(sender.getId(), messageRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getConversationId()).isNotNull();
        verify(chatMessageRepository, times(2)).save(any(ChatMessage.class));
    }

    @Test
    @DisplayName("Should throw exception when sender not found")
    void shouldThrowExceptionWhenSenderNotFound() {
        // Given
        when(userRepository.findById(sender.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(sender.getId(), messageRequest))
                .isInstanceOf(com.ocean.shopping.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Sender not found");
    }

    @Test
    @DisplayName("Should throw exception when receiver not found")
    void shouldThrowExceptionWhenReceiverNotFound() {
        // Given
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(sender.getId(), messageRequest))
                .isInstanceOf(com.ocean.shopping.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Receiver not found");
    }

    @Test
    @DisplayName("Should throw exception for empty message content")
    void shouldThrowExceptionForEmptyMessageContent() {
        // Given
        messageRequest.setContent("   "); // Empty/whitespace content
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(userRepository.findById(receiver.getId())).thenReturn(Optional.of(receiver));

        // When & Then
        assertThatThrownBy(() -> chatService.sendMessage(sender.getId(), messageRequest))
                .isInstanceOf(com.ocean.shopping.exception.BadRequestException.class)
                .hasMessageContaining("Message content cannot be empty");
    }

    @Test
    @DisplayName("Should get conversation history successfully")
    void shouldGetConversationHistorySuccessfully() {
        // Given
        Pageable pageable = PageRequest.of(0, 20);
        List<ChatMessage> messages = Arrays.asList(
                createTestChatMessage(),
                createTestChatMessage()
        );
        Page<ChatMessage> messagePage = new PageImpl<>(messages, pageable, messages.size());

        when(chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId, pageable))
                .thenReturn(messagePage);

        // Mock security check
        when(chatMessageRepository.findConversationParticipants(conversationId))
                .thenReturn(Arrays.asList(sender, receiver));

        // When
        Page<ChatMessageResponse> result = chatService.getConversationHistory(sender.getId(), conversationId, pageable);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(chatMessageRepository).findByConversationIdOrderByCreatedAtAsc(conversationId, pageable);
    }

    @Test
    @DisplayName("Should get user conversations successfully")
    void shouldGetUserConversationsSuccessfully() {
        // Given
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(chatMessageRepository.findConversationsByUser(sender)).thenReturn(Arrays.asList(conversationId));
        when(chatMessageRepository.findConversationParticipants(conversationId))
                .thenReturn(Arrays.asList(sender, receiver));

        ChatMessage latestMessage = createTestChatMessage();
        when(chatMessageRepository.findLatestMessageInConversation(eq(conversationId), any(PageRequest.class)))
                .thenReturn(Arrays.asList(latestMessage));
        when(chatMessageRepository.countUnreadMessagesInConversation(conversationId, sender))
                .thenReturn(0L);

        // When
        List<ConversationResponse> result = chatService.getUserConversations(sender.getId());

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);

        ConversationResponse conversation = result.get(0);
        assertThat(conversation.getConversationId()).isEqualTo(conversationId);
        assertThat(conversation.getParticipants()).hasSize(2);
        assertThat(conversation.getLastMessage()).isNotNull();
        assertThat(conversation.getUnreadCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should mark message as read successfully")
    void shouldMarkMessageAsReadSuccessfully() {
        // Given
        ChatMessage message = createTestChatMessage();
        message.setReceiver(sender); // sender is receiving the message
        message.setIsRead(false);

        when(chatMessageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        // Mock security check
        when(chatMessageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        // When
        chatService.markMessageAsRead(sender.getId(), message.getId());

        // Then
        assertThat(message.getIsRead()).isTrue();
        assertThat(message.getReadAt()).isNotNull();

        verify(chatMessageRepository).save(message);
        verify(messagingTemplate).convertAndSendToUser(
                eq(message.getSender().getId().toString()),
                eq("/queue/chat"),
                any(Map.class)
        );
    }

    @Test
    @DisplayName("Should mark conversation as read successfully")
    void shouldMarkConversationAsReadSuccessfully() {
        // Given
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));

        List<ChatMessage> unreadMessages = Arrays.asList(
                createUnreadMessage(),
                createUnreadMessage()
        );

        when(chatMessageRepository.findUnreadMessagesByReceiver(sender)).thenReturn(unreadMessages);

        // Mock security check
        when(chatMessageRepository.findConversationParticipants(conversationId))
                .thenReturn(Arrays.asList(sender, receiver));

        // When
        chatService.markConversationAsRead(sender.getId(), conversationId);

        // Then
        verify(chatMessageRepository).saveAll(anyList());
        
        // Verify all messages were marked as read
        unreadMessages.forEach(message -> {
            assertThat(message.getIsRead()).isTrue();
            assertThat(message.getReadAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("Should get unread message count successfully")
    void shouldGetUnreadMessageCountSuccessfully() {
        // Given
        when(userRepository.findById(sender.getId())).thenReturn(Optional.of(sender));
        when(chatMessageRepository.countUnreadMessagesByReceiver(sender)).thenReturn(5L);

        // When
        long result = chatService.getUnreadMessageCount(sender.getId());

        // Then
        assertThat(result).isEqualTo(5L);
        verify(chatMessageRepository).countUnreadMessagesByReceiver(sender);
    }

    @Test
    @DisplayName("Should create system message successfully")
    void shouldCreateSystemMessageSuccessfully() {
        // Given
        String systemContent = "User joined the conversation";
        when(chatMessageRepository.findConversationParticipants(conversationId))
                .thenReturn(Arrays.asList(sender, receiver));

        ChatMessage systemMessage = ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .content(systemContent)
                .messageType(ChatMessage.MessageType.SYSTEM)
                .sender(sender)
                .receiver(receiver)
                .deliveryStatus(ChatMessage.DeliveryStatus.DELIVERED)
                .isRead(false)
                .createdAt(ZonedDateTime.now())
                .build();

        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(systemMessage);

        // When
        ChatMessageResponse result = chatService.createSystemMessage(conversationId, systemContent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo(systemContent);
        assertThat(result.getMessageType()).isEqualTo(ChatMessageRequest.MessageType.SYSTEM);

        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(messagingTemplate, times(2)).convertAndSendToUser(
                anyString(),
                eq("/queue/chat"),
                any(ChatMessageResponse.class)
        );
    }

    @Test
    @DisplayName("Should delete message (soft delete) successfully")
    void shouldDeleteMessageSuccessfully() {
        // Given
        ChatMessage message = createTestChatMessage();
        message.setSender(sender); // sender owns the message

        when(chatMessageRepository.findById(message.getId())).thenReturn(Optional.of(message));

        // When
        chatService.deleteMessage(sender.getId(), message.getId());

        // Then
        assertThat(message.getContent()).isEqualTo("[Message deleted]");
        assertThat(message.getMessageType()).isEqualTo(ChatMessage.MessageType.SYSTEM);
        assertThat(message.getAttachmentUrl()).isNull();
        assertThat(message.getAttachmentName()).isNull();

        verify(chatMessageRepository).save(message);
    }

    @Test
    @DisplayName("Should validate security - user is participant in conversation")
    void shouldValidateSecurityUserIsParticipant() {
        // Given
        when(chatMessageRepository.findConversationParticipants(conversationId))
                .thenReturn(Arrays.asList(sender, receiver));

        // When
        boolean result = chatService.isParticipantInConversation(sender.getId(), conversationId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Should validate security - user is not participant in conversation")
    void shouldValidateSecurityUserIsNotParticipant() {
        // Given
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        when(chatMessageRepository.findConversationParticipants(conversationId))
                .thenReturn(Arrays.asList(sender, receiver));

        // When
        boolean result = chatService.isParticipantInConversation(otherUser.getId(), conversationId);

        // Then
        assertThat(result).isFalse();
    }

    // Helper methods
    private ChatMessage createTestChatMessage() {
        return ChatMessage.builder()
                .id(UUID.randomUUID())
                .conversationId(conversationId)
                .content(messageRequest.getContent())
                .messageType(ChatMessage.MessageType.TEXT)
                .sender(sender)
                .receiver(receiver)
                .deliveryStatus(ChatMessage.DeliveryStatus.SENT)
                .isRead(false)
                .createdAt(ZonedDateTime.now())
                .build();
    }

    private ChatMessage createUnreadMessage() {
        ChatMessage message = createTestChatMessage();
        message.setConversationId(conversationId);
        message.setIsRead(false);
        message.setReceiver(sender); // sender is receiving these messages
        return message;
    }
}