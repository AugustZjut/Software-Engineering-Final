package com.second.hand.trading.server.websocket;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ChatWebSocketConfig implements WebSocketConfigurer {

    @Resource
    private ChatWebSocketHandler chatWebSocketHandler;

    @Resource
    private ChatAuthHandshakeInterceptor chatAuthHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocketHandler, "/ws/chat")
                .addInterceptors(chatAuthHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
