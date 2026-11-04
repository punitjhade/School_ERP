package vsvps;

public class Util {

    public static class AccessDenied extends RuntimeException {
        public AccessDenied(String msg) { super(msg); }
    }

    public static double num(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }

    public static int inum(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    public static void requireAdmin(Session s) {
        if (s == null || !"admin".equals(s.role)) throw new AccessDenied("This page is only available to Admin users.");
    }

    public static void requireStaff(Session s) {
        if (s == null || !(s.role.equals("admin") || s.role.equals("teacher")))
            throw new AccessDenied("This page is only available to Admin or Teacher users.");
    }

    public static void requireFinance(Session s) {
        if (s == null || !(s.role.equals("admin") || s.role.equals("accountant")))
            throw new AccessDenied("This page is only available to Admin or Accountant users.");
    }

    public static String gradeFromPct(double pct) {
        if (pct >= 91) return "A1"; if (pct >= 81) return "A2"; if (pct >= 71) return "B1";
        if (pct >= 61) return "B2"; if (pct >= 51) return "C1"; if (pct >= 41) return "C2";
        if (pct >= 33) return "D"; return "E";
    }
}
