package com.second.hand.trading.server.websocket;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatSessionRegistry {

    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        sessions.computeIfAbsent(userId, key -> new CopyOnWriteArraySet<>()).add(session);
    }

    public void remove(Long userId, WebSocketSession session) {
        if (userId == null || session == null) {
            return;
        }
        CopyOnWriteArraySet<WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) {
            return;
        }
        userSessions.remove(session);
        if (userSessions.isEmpty()) {
            sessions.remove(userId);
        }
    }

    public void broadcastToUsers(Collection<Long> userIds, String payload) {
        if (CollectionUtils.isEmpty(userIds) || !StringUtils.hasText(payload)) {
            return;
        }
        TextMessage message = new TextMessage(payload);
        for (Long userId : userIds) {
            if (userId == null) {
                continue;
            }
            Set<WebSocketSession> wsSessions = sessions.get(userId);
            if (CollectionUtils.isEmpty(wsSessions)) {
                continue;
            }
            for (WebSocketSession session : wsSessions) {
                if (session == null) {
                    continue;
                }
                if (!session.isOpen()) {
                    remove(userId, session);
                    continue;
                }
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    remove(userId, session);
                    try {
                        session.close();
                    } catch (IOException ignored) {
                        // ignore close exception
                    }
                }
            }
        }
    }
}
