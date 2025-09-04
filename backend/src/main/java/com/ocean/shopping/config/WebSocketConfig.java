package com.ocean.shopping.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time features
 */
@Configuration
@EnableWebSocketMessageBroker
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for destinations prefixed with "/topic" and "/queue"
        config.enableSimpleBroker("/topic", "/queue", "/user");
        
        // Set application destination prefix for messages from clients
        config.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for private messages
        config.setUserDestinationPrefix("/user");
        
        log.info("WebSocket message broker configured with prefixes: /topic, /queue, /user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register STOMP endpoints
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
                
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
        
        log.info("WebSocket STOMP endpoints registered at /ws");
    }
}