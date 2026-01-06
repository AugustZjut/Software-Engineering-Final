package com.second.hand.trading.server.websocket;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final String USER_KEY = "userId";

    @Resource
    private ChatSessionRegistry chatSessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = resolveUserId(session);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("未登录"));
            return;
        }
        chatSessionRegistry.register(userId, session);
        log.debug("Chat websocket established for user {}", userId);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (message == null || !StringUtils.hasText(message.getPayload())) {
            return;
        }
        String payload = message.getPayload().trim();
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.debug("Chat websocket transport error", exception);
        cleanup(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.debug("Chat websocket closed: {}", status);
        cleanup(session);
    }

    private void cleanup(WebSocketSession session) {
        Long userId = resolveUserId(session);
        chatSessionRegistry.remove(userId, session);
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException ignored) {
            // ignore
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttributes().get(USER_KEY);
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof String str) {
            try {
                return Long.valueOf(str);
            } catch (NumberFormatException ex) {
                log.debug("Invalid userId attribute: {}", str);
            }
        }
        return null;
    }
}
