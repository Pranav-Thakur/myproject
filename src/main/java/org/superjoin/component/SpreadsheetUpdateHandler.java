package org.superjoin.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.superjoin.dto.ChangeImpact;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SpreadsheetUpdateHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(SpreadsheetUpdateHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    private String convertToJson(ChangeImpact impact) {
        try {
            return objectMapper.writeValueAsString(impact);
        } catch (JsonProcessingException e) {
            log.error("JSON conversion failed", e);
            return "{}";
        }
    }
    public void broadcastUpdate(ChangeImpact impact) {
        String message = convertToJson(impact);

        sessions.parallelStream().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                log.error("Failed to send update to session", e);
            }
        });
    }
}
