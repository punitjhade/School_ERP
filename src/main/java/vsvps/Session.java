package vsvps;

import java.util.List;

/** A logged-in user's session data. */
public class Session {
    public final String sessionId;
    public final String userId;
    public final String loginId;
    public final String role;
    public final String name;
    public final String studentId;   // for role=student
    public final String teacherId;   // for role=teacher (optional)
    public final List<String> wardIds; // for role=parent

    public Session(String sessionId, String userId, String loginId, String role, String name,
                    String studentId, String teacherId, List<String> wardIds) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.loginId = loginId;
        this.role = role;
        this.name = name;
        this.studentId = studentId;
        this.teacherId = teacherId;
        this.wardIds = wardIds;
    }

    /** The student record this session should view (self for student, first ward for parent). */
    public String primaryStudentId() {
        if ("student".equals(role)) return studentId;
        if ("parent".equals(role) && !wardIds.isEmpty()) return wardIds.get(0);
        return null;
    }
}
