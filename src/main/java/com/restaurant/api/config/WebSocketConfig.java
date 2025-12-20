package com.restaurant.api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

/**
 * WebSocketConfig – Phase 5.1
 * ------------------------------------------------------------------
 * Cấu hình WebSocket + STOMP cho hệ thống
 *
 * - Endpoint client kết nối: /ws
 * - Prefix client gửi lên server: /app
 * - Topic server broadcast xuống client: /topic/**
 *
 * Lưu ý:
 * - Dùng SimpleBroker (chưa dùng RabbitMQ / Kafka)
 * - Không ảnh hưởng REST API hiện tại
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Interceptor xử lý JWT khi WebSocket CONNECT
     */
    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    /**
     * Đăng ký endpoint WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * Cấu hình message broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Client gửi message lên server
        registry.setApplicationDestinationPrefixes("/app");

        // Server broadcast message xuống client (PREFIX)
        registry.enableSimpleBroker("/topic");
    }

    /**
     * Gắn interceptor để xử lý JWT cho WebSocket
     * (sẽ hoàn thiện ở STEP 2)
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Gắn interceptor để xử lý JWT cho toàn bộ message inbound
        // (Trong interceptor ta chỉ xử lý lúc CONNECT)
        registration.interceptors(webSocketAuthChannelInterceptor);
    }
}
