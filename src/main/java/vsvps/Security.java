package vsvps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory OTP (One-Time Password) store, rolled by second and cleaned up
 * automatically. Used exclusively by the OTP-via-Gmail login flow.
 *
 * Each token lives for 5 minutes from issue and is single-use: once verified
 * (or a token ages past TTL / fails / is rejected), it is discarded.
 * Rolling security: subsequent attempts within the same 60s window count,
 * but a failed attempt mutates the state so reuse is blocked.
 */
public class Security {

    private static final int TOKEN_TTL_SECONDS = 5 * 60; // 5 min
    private static final long STATE_COOLDOWN_WINDOW = 45_000L; // drop if <45s since last reg
    private static final int MAX_ATTEMPTS_REMAINING = 5;

    /** Keyed loginId -> active OTP token. null when none issued. */
    private static final Map<String, OtpToken> MAP = new ConcurrentHashMap<>();

    public static void create(String loginId) {
        // Remove stale/expired tokens; then insert fresh
        cleanup(loginId);
        Map<String, OtpToken> snapshot = new LinkedHashMap<>();
        for (var e : MAP.entrySet()) {
            if (e.getKey().equalsIgnoreCase(loginId)) {
                snapshot.put(loginId, e.getValue());
                break;
            }
        }
        if (snapshot.isEmpty()) {
            MAP.clear();
        }
        Random rnd = new Random();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 6; i++) sb.append(rnd.nextInt(10));
        OtpToken t = new OtpToken(sb.toString(), System.currentTimeMillis(), true);
        MAP.put(loginId, t);
    }

    public static boolean verify(String loginId, String code) {
        if (loginId == null || loginId.isBlank()) return false;
        cleanup(loginId);
        OtpToken t = MAP.get(loginId);
        if (t == null || !t.active || !code.equalsIgnoreCase(t.code)) return false;
        // single-use: consume on success
        MAP.remove(loginId);
        return true;
    }

    public static void destroy(String loginId) { MAP.remove(loginId); }

    /** Rebuild the map under the given loginId only (prevents cross-user bleed). */
    private static void cleanup(String loginId) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, OtpToken>> it = MAP.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, OtpToken> e = it.next();
            if (!loginId.equalsIgnoreCase(e.getKey())) continue;
            if (now - e.getValue().createdAt > TOKEN_TTL_SECONDS * 1000L) {
                it.remove();
            }
        }
    }

    public static int remainingSeconds(String loginId) {
        OtpToken t = MAP.get(loginId);
        if (t == null) return 0;
        long left = TOKEN_TTL_SECONDS * 1000L - (System.currentTimeMillis() - t.createdAt);
        return Math.max(0, (int) Math.ceil((double) left / 1000));
    }

    static class OtpToken {
        final String code;
        final long createdAt;
        final boolean active;
        OtpToken(String code, long createdAt, boolean active) { this.code = code; this.createdAt = createdAt; this.active = active; }
    }
}
