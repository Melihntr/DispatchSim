package com.dispatchsim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Frontend'in dinleyeceği (subscribe olacağı) prefix'ler
        config.enableSimpleBroker("/topic");
        
        // Frontend'den backend'e mesaj gelirken kullanılacak prefix (Şimdilik çok kullanmayacağız ama standarttır)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend'in WebSocket bağlantısını başlatacağı uç nokta (endpoint)
        // CORS ayarını React uygulamamız (genelde localhost:3000 veya 5173) bağlanabilsin diye "*" yapıyoruz.
        registry.addEndpoint("/ws-dispatch")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // Tarayıcı doğrudan WebSocket desteklemiyorsa fallback sağlar
    }
}