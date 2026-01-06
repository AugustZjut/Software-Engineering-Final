package com.second.hand.trading.server.websocket;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class ChatAuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final String USER_KEY = "userId";
    private static final String COOKIE_NAME = "shUserId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            response.setStatusCode(HttpStatus.FORBIDDEN);
            return false;
        }
        HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
        String userIdValue = resolveUserId(httpServletRequest);
        if (!StringUtils.hasText(userIdValue)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Long userId = Long.valueOf(userIdValue);
            attributes.put(USER_KEY, userId);
            return true;
        } catch (NumberFormatException ex) {
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private String resolveUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie != null && COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return request.getParameter(USER_KEY);
    }
}
