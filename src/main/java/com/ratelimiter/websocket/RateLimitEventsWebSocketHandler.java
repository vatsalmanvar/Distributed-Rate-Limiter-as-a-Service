package com.ratelimiter.websocket;

import com.ratelimiter.service.RateLimitEventBroadcaster;
import java.net.URI;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RateLimitEventsWebSocketHandler extends TextWebSocketHandler {

    private final RateLimitEventBroadcaster broadcaster;

    public RateLimitEventsWebSocketHandler(RateLimitEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID tenantId = tenantId(session.getUri());
        if (tenantId != null) {
            session.getAttributes().put("tenantId", tenantId);
        }
        broadcaster.register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregister(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        broadcaster.unregister(session);
    }

    private UUID tenantId(URI uri) {
        if (uri == null) {
            return null;
        }
        String value = UriComponentsBuilder.fromUri(uri).build().getQueryParams().getFirst("tenantId");
        return value == null || value.isBlank() ? null : UUID.fromString(value);
    }
}
