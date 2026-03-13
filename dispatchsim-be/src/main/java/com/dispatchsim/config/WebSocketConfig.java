package com.dispatchsim.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Uygulamanın gerçek zamanlı iletişim altyapısını (WebSocket) yapılandıran sınıftır.
 * <p>
 * Bu konfigürasyon, STOMP protokolünü kullanarak sunucudan istemciye (frontend)
 * metrik güncellemeleri ve görev durum değişikliklerinin anlık olarak
 * iletilmesini sağlar.
 * </p>
 *
 * @author Melihntr
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Mesaj aracısını (Message Broker) yapılandırır.
     * <ul>
     * <li>{@code /topic}: İstemcilerin (React) sunucudan gelen yayınları dinleyeceği önek.</li>
     * <li>{@code /app}: İstemcilerin sunucuya mesaj gönderirken kullanacağı uygulama öneki.</li>
     * </ul>
     * * @param config Mesaj aracısı kayıt defteri
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Frontend'in dinleyeceği (subscribe olacağı) prefix'ler
        config.enableSimpleBroker("/topic");

        // Frontend'den backend'e mesaj gelirken kullanılacak prefix
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * İstemcilerin WebSocket bağlantısını kuracağı uç noktayı (Endpoint) tanımlar.
     * <ul>
     * <li>Endpoint: {@code /ws-dispatch}</li>
     * <li>CORS: Geliştirme kolaylığı için tüm kaynaklara izin verilmiştir.</li>
     * <li>Fallback: WebSocket desteklemeyen tarayıcılar için SockJS desteği eklenmiştir.</li>
     * </ul>
     * * @param registry STOMP uç nokta kayıt defteri
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Frontend'in bağlantıyı başlatacağı URL ve tarayıcı uyumluluk ayarları
        registry.addEndpoint("/ws-dispatch")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}