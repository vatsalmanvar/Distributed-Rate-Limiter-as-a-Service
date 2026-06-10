package com.ratelimiter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.model.RateLimitEvent;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class RateLimitEventBroadcaster {

    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitEventBroadcaster.class);

    private final ObjectMapper objectMapper;
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public RateLimitEventBroadcaster(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(WebSocketSession session) {
        sessions.add(session);
    }

    public void unregister(WebSocketSession session) {
        sessions.remove(session);
    }

    public void publish(RateLimitEvent event) {
        if (sessions.isEmpty()) {
            return;
        }
        TextMessage message = toMessage(event);
        for (WebSocketSession session : sessions) {
            if (!session.isOpen()) {
                unregister(session);
                continue;
            }
            if (!matchesTenant(session, event.tenantId())) {
                continue;
            }
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                LOGGER.warn("Unable to send analytics websocket event to session {}", session.getId(), e);
                unregister(session);
            }
        }
    }

    private TextMessage toMessage(RateLimitEvent event) {
        try {
            return new TextMessage(objectMapper.writeValueAsString(event));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize analytics websocket event", e);
        }
    }

    private boolean matchesTenant(WebSocketSession session, UUID tenantId) {
        Object value = session.getAttributes().get("tenantId");
        return value == null || tenantId.equals(value);
    }
}
