package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.*;

public class Core {

    public static boolean dispatch(Req req, HttpExchange ex, String path) throws IOException {
        Session s = req.session;
        switch (path) {
            case "/home": home(req, ex); return true;
            case "/profile": profile(req, ex); return true;
            case "/logins": logins(req, ex); return true;
            case "/logins/create": Util.requireAdmin(s); createLogin(req, ex); return true;
            case "/logins/remove": Util.requireAdmin(s); removeLogin(req, ex); return true;
            default: return false;
        }
    }

    private static void home(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        StringBuilder c = new StringBuilder();
        c.append(welcomeBanner(s));

        if (s.role.equals("admin") || s.role.equals("accountant")) {
            int totalStudents = db.t("students").all().size();
            int totalStaff = db.t("teachers").all().size();
            int totalClasses = db.t("classes").all().size();
            double totalDue = db.t("fees").filter(f -> f.get("status").equals("Due")).stream().mapToDouble(f -> Util.num(f.get("amount"))).sum();
            int noticesCount = db.t("notices").all().size();
            var defaulters = db.t("fees").filter(f -> f.get("status").equals("Due"));
            c.append("<div class=\"grid grid-4\">")
             .append(Html.statCard("Total Students", String.valueOf(totalStudents), "card-accent-top", "\uD83C\uDF93"))
             .append(Html.statCard("Teaching Staff", String.valueOf(totalStaff), "card-accent-teal", "\uD83E\uDDD1\u200D\uD83C\uDFEB"))
             .append(Html.statCard("Total Fee Dues", Html.money(String.valueOf(totalDue)), "card-accent-danger", "\uD83D\uDCB0"))
             .append(Html.statCard("Notices Posted", String.valueOf(noticesCount), "card-accent-info", "\uD83D\uDCE2"))
             .append("</div>");

            if (s.role.equals("admin")) {
                c.append("<div class=\"grid grid-4\">")
                 .append("<a class=\"btn btn-primary quick-action\" href=\"/students\">\uD83C\uDF93<span>Manage Students</span></a>")
                 .append("<a class=\"btn btn-accent quick-action\" href=\"/attendance\">\u2705<span>Attendance</span></a>")
                 .append("<a class=\"btn btn-outline quick-action\" href=\"/fees\">\uD83D\uDCB0<span>Fees</span></a>")
                 .append("<a class=\"btn btn-outline quick-action\" href=\"/notices\">\uD83D\uDCE2<span>Post Notice</span></a>")
                 .append("</div>");
            }

            c.append("<div class=\"grid grid-3\">")
             .append(Html.statCard("Classes", String.valueOf(totalClasses), "", "\uD83C\uDFF7"))
             .append(Html.statCard("Fee Defaulters", String.valueOf(defaulters.size()), defaulters.isEmpty() ? "" : "card-accent-danger", "\u26A0\uFE0F"))
             .append(Html.statCard("Library Books", String.valueOf(db.t("books").all().size()), "", "\uD83D\uDCD6"))
             .append("</div>");

            c.append("<div class=\"card\"><div class=\"section-head\"><h2>\u26A0\uFE0F Fee Defaulters (preview)</h2><a href=\"/fees\">View all &rarr;</a></div>");
            c.append("<div class=\"table-wrap\"><table><thead><tr><th>Student</th><th>Class</th><th>Head</th><th>Amount</th><th>Due Date</th></tr></thead><tbody>");
            if (defaulters.isEmpty()) c.append("<tr class=\"empty-row\">\uD83C\uDF89<td colspan=\"5\">No fee dues \uD83C\uDF89</td></tr>");
            else for (var f : defaulters.subList(0, Math.min(6, defaulters.size())))
                c.append("<tr><td>").append(Html.esc(f.get("studentName"))).append("</td><td>").append(Html.esc(f.get("className")))
                 .append("</td><td>").append(Html.esc(f.get("head"))).append("</td><td>").append(Html.money(f.get("amount")))
                 .append("</td><td>").append(Html.fmtDate(f.get("dueDate"))).append("</td></tr>");
            c.append("</tbody></table></div></div>");
        } else if (s.role.equals("teacher")) {
            var assignedClasses = Academics.teacherAssignedClasses(s.teacherId);
            c.append("<div class=\"grid grid-4\">")
             .append("<a class=\"btn btn-primary quick-action\" href=\"/attendance\">\u2705<span>Mark Attendance</span></a>")
             .append("<a class=\"btn btn-accent quick-action\" href=\"/exams\">\uD83D\uDCDD<span>Enter Marks</span></a>")
             .append("<a class=\"btn btn-outline quick-action\" href=\"/homework\">\uD83D\uDCDA<span>Assign Homework</span></a>")
             .append("<a class=\"btn btn-outline quick-action\" href=\"/timetable\">\uD83D\uDDD3<span>View Timetable</span></a>")
             .append("</div>")
             .append("<div class=\"grid grid-3\">")
             .append(Html.statCard("My Classes", assignedClasses.isEmpty() ? "None" : String.valueOf(assignedClasses.size()), assignedClasses.isEmpty() ? "card-accent-gold" : "card-accent-top", "\uD83C\uDFF7"))
             .append(Html.statCard("Homework Posted", String.valueOf(Main.db.t("homework").all().size()), "", "\uD83D\uDCDA"))
             .append(Html.statCard("Notices", String.valueOf(Main.db.t("notices").all().size()), "", "\uD83D\uDCE2"))
             .append("</div>");
            if (assignedClasses.isEmpty()) {
                c.append("<div class=\"card\"><p>You are not currently assigned as a Class Teacher for any class. Once assigned (from Classes &amp; Houses), you'll be able to mark attendance and view your class roster here.</p></div>");
            } else {
                c.append("<div class=\"card\"><h3 style=\"margin-top:0\">\uD83C\uDFF7 My Classes</h3><div class=\"tag-list\">");
                for (String cls : assignedClasses) c.append("<a class=\"pill pill-info\" style=\"text-decoration:none\" href=\"/students?filterClass=").append(cls).append("\">Class ").append(Html.esc(cls)).append("</a>");
                c.append("</div></div>");
            }
        } else {
            String sid = s.primaryStudentId();
            var student = db.t("students").find(sid).orElse(null);
            var att = db.t("attendance").filter(a -> a.get("studentId").equals(sid));
            long present = att.stream().filter(a -> a.get("status").equals("Present")).count();
            int pct = att.isEmpty() ? 0 : (int) Math.round(present * 100.0 / att.size());
            double due = db.t("fees").filter(f -> f.get("studentId").equals(sid) && f.get("status").equals("Due")).stream().mapToDouble(f -> Util.num(f.get("amount"))).sum();
            String cls = student != null ? student.get("className") : "";
            var subs = db.t("homework_submissions");
            long pendingHw = db.t("homework").filter(h -> h.get("className").equals(cls))
                    .stream().filter(h -> subs.filter(x -> x.get("homeworkId").equals(h.get("id")) && x.get("studentId").equals(sid)).isEmpty()).count();

            if (student != null) {
                String initials = student.get("name").isBlank() ? "?" : student.get("name").substring(0, 1).toUpperCase();
                c.append("<div class=\"card\"><div class=\"list-inline\"><span class=\"avatar-ph\" style=\"width:50px;height:50px;font-size:18px\">").append(initials).append("</span>")
                 .append("<div><h3 style=\"margin:0\">").append(Html.esc(student.get("name"))).append("</h3>")
                 .append("<span class=\"pill pill-info\">Class ").append(Html.esc(student.get("className"))).append("-").append(Html.esc(student.get("section"))).append("</span> ")
                 .append("<span class=\"small\">Admission No. ").append(Html.esc(student.get("admissionNo"))).append("</span></div></div></div>");
            }
            c.append("<div class=\"grid grid-4\">")
             .append(Html.statCard("Attendance", pct + "%", pct < 75 ? "card-accent-danger" : "card-accent-top", "\u2705"))
             .append(Html.statCard("Fee Due", Html.money(String.valueOf(due)), due > 0 ? "card-accent-danger" : "", "\uD83D\uDCB0"))
             .append(Html.statCard("Pending Homework", String.valueOf(pendingHw), pendingHw > 0 ? "card-accent-gold" : "card-accent-teal", "\uD83D\uDCDA"))
             .append(Html.statCard("Notices", String.valueOf(db.t("notices").all().size()), "card-accent-info", "\uD83D\uDCE2"))
             .append("</div>");
            c.append("<div class=\"grid grid-4\">")
             .append("<a class=\"btn btn-outline quick-action\" href=\"/attendance\">\u2705<span>My Attendance</span></a>")
             .append("<a class=\"btn btn-outline quick-action\" href=\"/fees\">\uD83D\uDCB0<span>My Fees</span></a>")
             .append("<a class=\"btn btn-outline quick-action\" href=\"/homework\">\uD83D\uDCDA<span>Homework</span></a>")
             .append("<a class=\"btn btn-outline quick-action\" href=\"/timetable\">\uD83D\uDDD3<span>Timetable</span></a>")
             .append("</div>");
            c.append("<div class=\"card\"><h2>\uD83D\uDCE2 Latest Notices</h2>");
            var notices = new ArrayList<>(db.t("notices").all());
            notices.sort((a, b) -> b.get("date").compareTo(a.get("date")));
            if (notices.isEmpty()) c.append("<p class=\"small\">No notices yet.</p>");
            for (var n : notices.subList(0, Math.min(4, notices.size())))
                c.append("<div style=\"padding:10px 0;border-bottom:1px solid var(--border)\"><b>").append(Html.esc(n.get("title")))
                 .append("</b> ").append(Html.pill(n.get("category"))).append("<div class=\"small\">").append(Html.fmtDate(n.get("date"))).append("</div></div>");
            c.append("</div>");
        }
        Res.html(ex, 200, Html.page("home", s, c.toString()));
    }

    private static String welcomeBanner(Session s) {
        String greeting = switch (s.role) {
            case "admin" -> "Here's what's happening across the school today.";
            case "teacher" -> "Here's your teaching overview for today.";
            case "accountant" -> "Here's the school's financial snapshot today.";
            case "parent" -> "Here's how your ward is doing.";
            default -> "Here's your dashboard for today.";
        };
        return "<div class=\"welcome-banner\"><h2>Welcome back, " + Html.esc(firstGivenName(s.name)) + " \uD83D\uDC4B</h2><p>" + greeting + "</p></div>";
    }

    private static String firstGivenName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        int i = 0;
        while (i < parts.length - 1 && parts[i].matches("(?i)(mr|mrs|ms|dr|mx)\\.?")) i++;
        return parts.length > 0 ? parts[i] : fullName;
    }

    private static void profile(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        String c = "<div class=\"card\" style=\"max-width:480px\">" +
                "<div class=\"list-inline\" style=\"margin-bottom:16px\"><div class=\"avatar-ph\">" + Html.esc(s.name.substring(0, 1)) + "</div>" +
                "<div><h3 style=\"margin:0\">" + Html.esc(s.name) + "</h3>" + Html.pill(s.role) + "</div></div>" +
                "<p><b>Login ID:</b> " + Html.esc(s.loginId) + "</p><div class=\"divider\"></div>" +
                "<form method=\"post\" action=\"/logout\"><button class=\"btn btn-danger\" type=\"submit\">Log Out</button></form></div>";
        Res.html(ex, 200, Html.page("profile", s, c));
    }

    private static void logins(Req req, HttpExchange ex) throws IOException {
        Util.requireAdmin(req.session);
        Db db = Main.db;
        var students = db.t("students").all();
        var teachers = db.t("teachers").all();

        StringBuilder linkOptions = new StringBuilder();
        for (var st : students) linkOptions.append("<option value=\"").append(st.get("id")).append("\">").append(Html.esc(st.get("name"))).append(" (").append(Html.esc(st.get("admissionNo"))).append(")</option>");
        StringBuilder teacherOptions = new StringBuilder("<option value=\"\">-- None --</option>");
        for (var te : teachers) teacherOptions.append("<option value=\"").append(te.get("id")).append("\">").append(Html.esc(te.get("name"))).append("</option>");

        String c = "<div class=\"card\"><h2>Create Login</h2>" +
                "<form method=\"post\" action=\"/logins/create\">" +
                "<div class=\"form-grid\">" +
                Html.textField("Name", "name", "", "text") +
                "<div class=\"field\"><label>Role</label><select name=\"role\">" +
                "<option value=\"admin\">Admin</option><option value=\"teacher\">Teacher</option><option value=\"accountant\">Accountant</option>" +
                "<option value=\"parent\">Parent</option><option value=\"student\" selected>Student</option></select></div>" +
                Html.textField("Login ID", "loginId", "", "text") +
                Html.textField("Password", "password", "", "text") +
                "</div>" +
                "<div class=\"form-grid\">" +
                "<div class=\"field\"><label>Link to Student (role=student)</label><select name=\"studentLink\"><option value=\"\">-- N/A --</option>" + linkOptions + "</select></div>" +
                "<div class=\"field\"><label>Link to Ward (role=parent)</label><select name=\"wardLink\"><option value=\"\">-- N/A --</option>" + linkOptions + "</select></div>" +
                "<div class=\"field\"><label>Link to Teacher record (role=teacher, optional)</label><select name=\"teacherLink\">" + teacherOptions + "</select></div>" +
                "</div>" +
                "<button class=\"btn btn-primary\" type=\"submit\">Create Login</button>" +
                "</form></div>" +
                "<div class=\"card\"><h2>All Logins</h2><div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>Login ID</th><th>Role</th><th>Linked To</th><th>Action</th></tr></thead><tbody>";

        StringBuilder rows = new StringBuilder();
        for (var u : db.t("users").all()) {
            String linked = "\u2014";
            if (u.get("role").equals("student") && !u.getOrDefault("studentId", "").isBlank()) {
                var st = db.t("students").find(u.get("studentId")).orElse(null);
                linked = st != null ? Html.esc(st.get("name")) : "\u2014";
            } else if (u.get("role").equals("parent") && !u.getOrDefault("wardIds", "").isBlank()) {
                List<String> names = new ArrayList<>();
                for (String wid : u.get("wardIds").split(";")) db.t("students").find(wid).ifPresent(st -> names.add(st.get("name")));
                linked = Html.esc(String.join(", ", names));
            } else if (u.get("role").equals("teacher") && !u.getOrDefault("teacherId", "").isBlank()) {
                var te = db.t("teachers").find(u.get("teacherId")).orElse(null);
                linked = te != null ? Html.esc(te.get("name")) : "\u2014";
            }
            rows.append("<tr><td>").append(Html.esc(u.get("name"))).append("</td><td>").append(Html.esc(u.get("loginId")))
                .append("</td><td>").append(Html.esc(u.get("role"))).append("</td><td>").append(linked).append("</td><td>");
            if (u.get("id").equals(req.session.userId)) rows.append("<span class=\"small\">Current user</span>");
            else rows.append("<form method=\"post\" action=\"/logins/remove\" style=\"display:inline\">" + Html.hidden("id", u.get("id")) + "<button class=\"btn btn-sm btn-danger\" type=\"submit\">Remove</button></form>");
            rows.append("</td></tr>");
        }
        c += rows + "</tbody></table></div></div>";
        Res.html(ex, 200, Html.page("logins", req.session, c));
    }

    private static void createLogin(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String name = req.p("name").trim(), role = req.p("role"), loginId = req.p("loginId").trim(), password = req.p("password");
        if (name.isBlank() || loginId.isBlank() || password.isBlank()) { Res.redirect(ex, "/logins"); return; }
        boolean taken = db.t("users").filter(u -> u.get("loginId").equalsIgnoreCase(loginId)).size() > 0;
        if (taken) { Res.redirect(ex, "/logins"); return; }
        var row = db.t("users").newRow();
        row.put("id", db.newId("u")); row.put("name", name); row.put("role", role); row.put("loginId", loginId); row.put("password", password); row.put("email", "");
        if (role.equals("student")) row.put("studentId", req.p("studentLink"));
        if (role.equals("parent")) row.put("wardIds", req.p("wardLink"));
        if (role.equals("teacher") && !req.p("teacherLink").isBlank()) row.put("teacherId", req.p("teacherLink"));
        db.t("users").add(row);
        Res.redirect(ex, "/logins");
    }

    private static void removeLogin(Req req, HttpExchange ex) throws IOException {
        Main.db.t("users").deleteById(req.p("id"));
        Res.redirect(ex, "/logins");
    }
}
