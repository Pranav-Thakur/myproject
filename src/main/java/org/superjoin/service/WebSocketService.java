package org.superjoin.service;

import org.springframework.stereotype.Service;
import org.superjoin.dto.ChangeImpact;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WebSocketService {

    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

    public void registerSession(WebSocketSession session) {
        sessions.add(session);
    }

    public void removeSession(WebSocketSession session) {
        sessions.remove(session);
    }

    public void broadcastUpdate(ChangeImpact impact) {
        String message = convertToJson(impact);

        sessions.parallelStream().forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (Exception e) {
                e.printStackTrace(); // Or use a proper logger
            }
        });
    }

    private String convertToJson(ChangeImpact impact) {
        // You can use Jackson ObjectMapper or similar libraries here
        return "{ \"changedCell\": \"" + impact.getChangedCell() + "\", " +
                "\"oldValue\": \"" + impact.getOldValue() + "\", " +
                "\"newValue\": \"" + impact.getNewValue() + "\", " +
                "\"affectedCells\": " + impact.getAffectedCells().toString() + " }";
    }
}
