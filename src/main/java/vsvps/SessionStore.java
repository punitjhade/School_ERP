package vsvps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionStore {
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public Session create(String userId, String loginId, String role, String name,
                           String studentId, String teacherId, List<String> wardIds) {
        String sid = UUID.randomUUID().toString();
        Session s = new Session(sid, userId, loginId, role, name, studentId, teacherId, wardIds);
        sessions.put(sid, s);
        return s;
    }

    public Session get(String sessionId) {
        if (sessionId == null) return null;
        return sessions.get(sessionId);
    }

    public void destroy(String sessionId) {
        if (sessionId != null) sessions.remove(sessionId);
    }
}
