package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/** Authentication: two paths share the same login page UI.
 *
 *  Path A (default): email + password. For gmail@ accounts this honors an
 *  "send me an OTP" checkbox — full flow lives under /otp-login/:uid.
 *  Path B (fallback): plaintext passwords, kept for any legacy demo accounts.
 *  Real deployments must store bcrypt/Argon2 hashes and drop Path B. */
public class Auth {

    public static final String OTP_DOMAIN = "gmail.com";

    private static Function<String, String> pHash = s -> {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) b.append((char) (s.charAt(i) % 94 + 33));
        return b.toString();
    };

    public static void login(Req req, HttpExchange ex) throws IOException {
        if (req.session != null) { Res.redirect(ex, "/home"); return; }

        if ("POST".equalsIgnoreCase(req.method)) {
            String loginId = req.p("loginId").trim();
            String password = req.p("password");
            boolean otpPref = "true".equalsIgnoreCase(req.p("otpPref"));
            boolean useOtp = otpPref && isGmailDomain(loginId);

            if (useOtp) {
                String code = req.p("otpCode");
                if (!Security.verify(loginId, code)) {
                    Security.create(loginId); // roll a fresh token so the UI keeps refreshing
                    Res.html(ex, 200, renderLoginWithOtpError(loginId));
                    return;
                }
                // Verify account exists AND is hosted on @gmail.com domain
                List<LinkedHashMap<String, String>> candidates = Main.db.t("users").filter(u -> u.get("loginId").equalsIgnoreCase(loginId));
                LinkedHashMap<String, String> userOtp = null;
                for (LinkedHashMap<String, String> c : candidates) {
                    if (c.get("email").toLowerCase().endsWith("@" + OTP_DOMAIN)) { userOtp = c; break; }
                }
                if (userOtp == null) {
                    Security.destroy(loginId);
                    Res.html(ex, 200, renderLoginInvalid("Login ID not found on domain @gmail.com."));
                    return;
                }
                if (!matchesPassword(userOtp.get("passwordHash"), password)) {
                    Security.destroy(loginId);
                    Res.html(ex, 200, renderLoginWithOtpError(loginId));
                    return;
                }
                createSessionAndRedirect(ex, userOtp.get("id"), userOtp.get("loginId"),
                        userOtp.get("role"), userOtp.get("name"),
                        userOtp.getOrDefault("studentId", ""),
                        userOtp.getOrDefault("teacherId", ""),
                        parseWardIds(userOtp.getOrDefault("wardIds", "")));
                return;
            }

            // ---- Legacy path (non-gmail / demo accounts only) ----
            List<LinkedHashMap<String, String>> match = Main.db.t("users").filter(u -> u.get("loginId").equalsIgnoreCase(loginId) && u.get("password").equals(password));
            if (match.isEmpty()) {
                Res.html(ex, 200, renderLoginInvalid("Invalid Login ID or password."));
                return;
            }
            LinkedHashMap<String, String> u = match.get(0);
            List<String> wardIds = new ArrayList<>();
            String wardField = u.getOrDefault("wardIds", "");
            if (!wardField.isBlank()) wardIds.addAll(Arrays.asList(wardField.split(";")));
            Session s = Main.sessions.create(u.get("id"), u.get("loginId"), u.get("role"), u.get("name"),
                    u.getOrDefault("studentId", ""), u.getOrDefault("teacherId", ""), wardIds);
            Res.setCookie(ex, "SID", s.sessionId, 60 * 60 * 8);
            Res.redirect(ex, "/home");
            return;
        }
        Res.html(ex, 200, renderLogin(null, false, ""));
    }

    private static String loginIdPrompt() {
        return "<div class=\"field\" style=\"margin-bottom:18px\"><label>Login ID</label>" +
                "<div class=\"input-icon-wrap\"><span class=\"input-icon\">👤</span>" +
                "<input class=\"has-icon\" type=\"text\" name=\"loginId\" placeholder=\"e.g. admin1\" autofocus/></div></div>" +
                "<p class=\"hint\">Use your school-issued Login ID (e.g. admin1, teacher1, student1). Not sure? Contact your school admin.</p>";
    }

    public static void logout(Req req, HttpExchange ex) throws IOException {
        String sid = req.cookies.get("SID");
        Main.sessions.destroy(sid);
        Res.setCookie(ex, "SID", "", 0);
        Res.redirect(ex, "/login");
    }

    private static List<String> parseWardIds(String v) {
        if (v.isBlank()) return Collections.emptyList();
        return Arrays.asList(v.split(";"));
    }

    private static boolean matchesPassword(String stored, String provided) {
        if (stored == null || provided == null) return false;
        if (stored.isEmpty() || provided.isEmpty()) return false;
        if (stored.startsWith("$")) return stored.equals(provided); // bcrypt placeholder marker
        if (stored.startsWith("hashed:")) return pHash.apply(provided).equals(stored.substring("hashed:".length()));
        if (stored.startsWith("a:") && stored.length() >= 42) return pHash.apply(provided).equals(stored.substring(2));
        return stored.equals(provided); // plaintext fallback (not expected in production builds)
    }

    private static boolean isGmailDomain(String email) {
        int at = email.lastIndexOf('@');
        return at > 0 && at < email.length() - 1 && email.substring(at + 1).equalsIgnoreCase(OTP_DOMAIN);
    }

    private static void createSessionAndRedirect(HttpExchange ex, String userId, String loginId, String role, String name,
                                                 String studentId, String teacherId, List<String> wardIds) throws IOException {
        Session s = Main.sessions.create(userId, loginId, role, name, studentId, teacherId, wardIds);
        Res.setCookie(ex, "SID", s.sessionId, 60 * 60 * 8);
        Res.redirect(ex, "/home");
    }

    // ---------------- Renderers ----------------
    private static String renderLogin(String error, boolean showOtpHint, String codeHint) { return renderLogin4(error, false, "", ""); }
    private static String renderLogin4(String error, boolean showOtpHint, String codeHint, String customLoginId) {
        String box = "";
        box += "<div class=\"field\" style=\"margin-bottom:18px\"><label>Login ID</label>" +
                "<div class=\"input-icon-wrap\"><span class=\"input-icon\">👤</span>" +
                "<input class=\"has-icon\" type=\"text\" name=\"loginId\" value=\"" + Html.esc(customLoginId == null ? "" : customLoginId) + "\" placeholder=\"e.g. admin1\" autofocus/></div></div>";
        box += "<div class=\"field\" style=\"margin-bottom:14px\"><label>Password</label>" +
                "<div class=\"input-icon-wrap\"><span class=\"input-icon\">🔒</span>" +
                "<input class=\"has-icon\" type=\"password\" name=\"password\" placeholder=\"Enter password\"/></div></div>";
        box += "<div class=\"field checkbox-row\"><input type=\"checkbox\" name=\"otpPref\" id=\"otpPref\"/>" +
                "<label style=\"margin:0\">Send me an OTP instead of entering my password</label></div>";
        if (showOtpHint) {
            box += "<div class=\"otp-area\">" +
                    "<div class=\"otp-title\">💲 Enter OTP sent to you@gmail.com</div>" +
                    "<div class=\"otp-code\"><input maxlength=\"6\" inputmode=\"numeric\" name=\"otpCode\" placeholder=\"123456\"/></div>" +
                    "<div class=\"otp-tip\">A fresh 6-digit code is generated for every login attempt. If you don't receive it in under a minute, try refreshing.</div>" +
                    "</div>";
        } else if (codeHint != null) {
            box += "<div class=\"error-box show\"><span class=\"dot\"></span>" + Html.esc(codeHint) + "</div>";
        }
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/>" +
                "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>" +
                "<title>VSVPS Connect — Login</title><link rel=\"stylesheet\" href=\"/css/style.css?v=18\"/></head><body>" +
                "<div class=\"login-page\"><div class=\"login-shell\">" +
                "<div class=\"login-left login-left-photo\">" +
                "<div class=\"login-left-overlay\"></div>" +
                "<div class=\"login-left-inner\">" +
                "<img src=\"/images/logo-v2.png\" alt=\"VSVPS logo\" class=\"login-emblem\"/>" +
                "<div class=\"login-accent-bar\"></div>" +
                "<h2>Welcoming every learner,<br/>every single day.</h2>" +
                "<p>VSVPS Connect brings students, teachers, parents, and staff onto one simple, secure platform — attendance, fees, exams, and more, all in one place.</p>" +
                "</div></div></div></div>" +
                "<div class=\"login-right\">" +
                "<div class=\"login-form-wrap\">" +
                "<div class=\"login-brand\"><img src=\"/images/logo-v2.png\" alt=\"VSVPS logo\" class=\"logo-dot\"/>" +
                "<div><div class=\"login-brand-name\">VSVPS Connect</div>" +
                "<div class=\"login-brand-sub\">Vidhya Sanskar Valley Public School</div></div></div>" +
                "<h1 class=\"login-heading\">Log In</h1>" +
                "<p class=\"login-subheading\">Enter your credentials to access your dashboard.</p>" +
                "<p class=\"login-forgot\">Forgot your password? <a href=\"/login\">Contact your school admin to reset it.</a></p>" +
                (error != null ? "<div class=\"error-box show\">" + Html.esc(error) + "</div>" : "") +
                "<form method=\"post\" action=\"/login\">" + box +
                "<button class=\"btn btn-primary login-submit\" type=\"submit\">Log In</button>" +
                "</form></div></div></div></div></body></html>";
    }

    private static String renderLoginWithOtpError(String loginId) { return renderLogin4(null, true, "Incorrect email or OTP. Please check your inbox.", ""); }
    private static String renderLoginInvalid(String error) { return renderLogin4(error, true, "Authentication failed. Please try again.", ""); }
}
