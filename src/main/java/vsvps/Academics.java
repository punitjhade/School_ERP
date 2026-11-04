package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Academics {

    public static boolean dispatch(Req req, HttpExchange ex, String path) throws IOException {
        Session s = req.session;
        switch (path) {
            case "/students": students(req, ex); return true;
            case "/students/view": viewStudentDetail(req, ex); return true;
            case "/students/save": Util.requireAdmin(s); saveStudent(req, ex); return true;
            case "/students/delete": Util.requireAdmin(s); deleteStudent(req, ex); return true;
            case "/students/export": exportStudentsCsv(req, ex); return true;
            case "/students/import": Util.requireAdmin(s); importStudentsCsv(req, ex); return true;
            case "/students/promote": Util.requireAdmin(s); promoteStudents(req, ex); return true;

            case "/teachers": teachers(req, ex); return true;
            case "/teachers/save": Util.requireAdmin(s); saveTeacher(req, ex); return true;
            case "/teachers/delete": Util.requireAdmin(s); deleteTeacher(req, ex); return true;

            case "/classes": classes(req, ex); return true;
            case "/classes/save": Util.requireAdmin(s); saveClass(req, ex); return true;
            case "/classes/assign": Util.requireAdmin(s); assignClassTeacher(req, ex); return true;
            case "/classes/delete": Util.requireAdmin(s); deleteClass(req, ex); return true;
            case "/houses/save": Util.requireAdmin(s); saveHouse(req, ex); return true;
            case "/houses/delete": Util.requireAdmin(s); deleteHouse(req, ex); return true;

            case "/subjects": subjects(req, ex); return true;
            case "/subjects/save": Util.requireAdmin(s); saveSubject(req, ex); return true;
            case "/subjects/delete": Util.requireAdmin(s); deleteSubject(req, ex); return true;

            case "/faculty": faculty(req, ex); return true;

            case "/admissions": admissions(req, ex); return true;
            case "/admissions/save": saveAdmission(req, ex); return true;
            case "/admissions/delete": Util.requireAdmin(s); deleteAdmission(req, ex); return true;
            case "/admissions/convert": Util.requireAdmin(s); convertAdmission(req, ex); return true;

            case "/attendance": attendance(req, ex); return true;
            case "/attendance/save": Util.requireStaff(s); saveAttendance(req, ex); return true;

            case "/timetable": timetable(req, ex); return true;
            case "/timetable/save": Util.requireAdmin(s); saveTimetable(req, ex); return true;

            case "/homework": homework(req, ex); return true;
            case "/homework/save": Util.requireStaff(s); saveHomework(req, ex); return true;
            case "/homework/delete": Util.requireStaff(s); deleteHomework(req, ex); return true;
            case "/homework/submit": submitHomework(req, ex); return true;

            case "/exams": exams(req, ex); return true;
            case "/exams/save": Util.requireStaff(s); saveExam(req, ex); return true;
            case "/exams/marks": examMarksGrid(req, ex); return true;
            case "/exams/marks/save": Util.requireStaff(s); saveExamMarks(req, ex); return true;
            case "/exams/marks/print": printReportCard(req, ex); return true;

            default: return false;
        }
    }

    // =========================== STUDENTS ===========================
    static java.util.List<String> teacherAssignedClasses(String teacherId) {
        var out = new java.util.ArrayList<String>();
        if (teacherId == null || teacherId.isBlank()) return out;
        for (var cl : Main.db.t("classes").filter(c -> teacherId.equals(c.getOrDefault("classTeacherId", "")))) out.add(cl.get("className"));
        return out;
    }

    private static void students(Req req, HttpExchange ex) throws IOException {
        Util.requireStaff(req.session); // admin or teacher only
        Session session = req.session;
        boolean isAdmin = session.role.equals("admin");
        Db db = Main.db;
        var allStudents = db.t("students").all();

        // Determine which classes this user is allowed to browse
        java.util.List<String> allowedClasses;
        if (isAdmin) {
            var set = new TreeSet<String>();
            for (var s : allStudents) set.add(s.get("className"));
            allowedClasses = new java.util.ArrayList<>(set);
        } else {
            allowedClasses = teacherAssignedClasses(session.teacherId);
        }

        StringBuilder c = new StringBuilder();
        c.append("<div class=\"section-head\"><h2>\uD83C\uDF93 Students</h2><span class=\"muted\">").append(allStudents.size()).append(" total record(s)</span></div>");

        if (!isAdmin && allowedClasses.isEmpty()) {
            c.append("<div class=\"card\"><p>You are not currently assigned as a Class Teacher for any class, so there are no student rosters to show here yet. Ask your Admin to assign you as a Class Teacher from the Classes &amp; Houses page.</p></div>");
            Res.html(ex, 200, Html.page("students", session, c.toString()));
            return;
        }

        if (isAdmin) c.append(studentForm(null));

        // Class overview cards
        c.append("<div class=\"section-head\"><h3 style=\"margin:0\">Browse by Class</h3></div><div class=\"grid grid-4\">");
        String[] accentCycle = {"card-accent-top", "card-accent-teal", "card-accent-info", "card-accent-gold"};
        int accentIdx = 0;
        for (String cls : allowedClasses) {
            long count = allStudents.stream().filter(s -> s.get("className").equals(cls)).count();
            c.append("<a href=\"/students?filterClass=").append(cls).append("\" style=\"text-decoration:none\">")
             .append("<div class=\"card class-card ").append(accentCycle[accentIdx++ % accentCycle.length]).append("\">")
             .append("<div class=\"class-card-icon\">\uD83C\uDF93</div>")
             .append("<div class=\"class-card-name\">Class ").append(Html.esc(cls)).append("</div>")
             .append("<div class=\"class-card-count\">").append(count).append(" student(s)</div>")
             .append("</div></a>");
        }
        c.append("</div>");

        String filterClass = req.p("filterClass");
        if (!isAdmin && !filterClass.isBlank() && !allowedClasses.contains(filterClass)) throw new Util.AccessDenied("You can only view students in classes where you are the assigned Class Teacher.");
        if (!isAdmin && filterClass.isBlank() && allowedClasses.size() == 1) filterClass = allowedClasses.get(0); // auto-open a teacher's single class

        if (!filterClass.isBlank()) {
            final String fClass = filterClass;
            var rows = allStudents.stream().filter(s -> s.get("className").equals(fClass)).toList();
            c.append("<div class=\"card\" style=\"padding:0\">")
             .append("<div class=\"section-head\" style=\"padding:16px 16px 0\"><h3 style=\"margin:0\">Class ").append(Html.esc(filterClass)).append(" Roster</h3><span class=\"muted\">").append(rows.size()).append(" student(s)</span></div>")
             .append("<div class=\"table-wrap\"><table><thead><tr>")
             .append("<th>Name</th><th>Admission No.</th><th>Section</th><th>Father's Name</th><th>Parent Phone</th><th>House</th><th>Stream</th><th>Actions</th>")
             .append("</tr></thead><tbody>");
            if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"8\">No students in this class yet.</td></tr>");
            for (var r : rows) {
                c.append("<tr><td>").append(Html.esc(r.get("name"))).append("</td><td>").append(Html.esc(r.get("admissionNo")))
                 .append("</td><td>").append(Html.esc(r.get("section")))
                 .append("</td><td>").append(r.get("fatherName").isBlank() ? "\u2014" : Html.esc(r.get("fatherName")))
                 .append("</td><td>").append(Html.esc(r.get("parentPhone"))).append("</td><td>").append(Html.esc(r.get("house")))
                 .append("</td><td>").append(r.get("stream").isBlank() ? "\u2014" : Html.esc(r.get("stream"))).append("</td>");
                c.append("<td class=\"btn-row\">")
                 .append("<a class=\"btn btn-outline btn-sm\" href=\"/students/view?id=").append(r.get("id")).append("\">View</a>");
                if (isAdmin) {
                    c.append("<a class=\"btn btn-outline btn-sm\" href=\"/students?edit=").append(r.get("id")).append("&filterClass=").append(filterClass).append("\">Edit</a>")
                     .append("<form method=\"post\" action=\"/students/delete\" style=\"display:inline\">").append(Html.hidden("id", r.get("id")))
                     .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form>");
                }
                c.append("</td></tr>");
            }
            c.append("</tbody></table></div></div>");
        }

        String editId = req.p("edit");
        if (isAdmin && !editId.isBlank()) {
            var editRow = db.t("students").find(editId).orElse(null);
            if (editRow != null) c.append(studentForm(editRow));
        }

        if (isAdmin) {
            var rows = allStudents;
            c.append("<div class=\"card\"><div class=\"section-head\"><h2>Bulk Tools</h2></div>");
            c.append("<h3>CSV Export / Import</h3>");
            c.append("<div class=\"btn-row\" style=\"margin-bottom:14px\">");
            c.append("<a class=\"btn btn-outline\" href=\"/students/export\">\u2B07 Export CSV</a></div>");
            c.append("<form method=\"post\" action=\"/students/import\">");
            c.append("<div class=\"field\"><label>Paste CSV content (use \u201cExport CSV\u201d above first to see the exact header row/column order expected)</label>");
            c.append("<textarea name=\"csvText\" rows=\"5\" placeholder=\"name,admissionNo,className,section,academicYear,dob,gender,...\"></textarea></div>");
            c.append("<button class=\"btn btn-outline\" type=\"submit\">\u2B06 Import CSV</button></form>");
            c.append("<div class=\"divider\"></div><h3>Bulk Promotion</h3><p class=\"small\">Select students, choose a target class/section, then promote.</p>");
            c.append("<form method=\"post\" action=\"/students/promote\">");
            c.append("<div class=\"table-wrap\"><table><thead><tr><th></th><th>Name</th><th>Current Class</th></tr></thead><tbody>");
            for (var r : rows) c.append("<tr><td><input type=\"checkbox\" name=\"sel\" value=\"").append(r.get("id")).append("\"></td><td>")
                    .append(Html.esc(r.get("name"))).append("</td><td>").append(Html.esc(r.get("className"))).append("-").append(Html.esc(r.get("section"))).append("</td></tr>");
            c.append("</tbody></table></div>");
            c.append("<div class=\"form-grid\" style=\"margin-top:12px\">")
             .append(Html.textField("Target Class", "targetClass", "", "text"))
             .append(Html.textField("Target Section", "targetSection", "", "text")).append("</div>");
            c.append("<button class=\"btn btn-primary\" type=\"submit\">Promote Selected</button></form></div>");
        }

        Res.html(ex, 200, Html.page("students", session, c.toString()));
    }

    private static void viewStudentDetail(Req req, HttpExchange ex) throws IOException {
        Util.requireStaff(req.session);
        Session session = req.session;
        Db db = Main.db;
        var s = db.t("students").find(req.p("id")).orElse(null);
        if (s == null) { Res.redirect(ex, "/students"); return; }
        if (!session.role.equals("admin")) {
            var allowed = teacherAssignedClasses(session.teacherId);
            if (!allowed.contains(s.get("className"))) throw new Util.AccessDenied("You can only view students in classes where you are the assigned Class Teacher.");
        }

        String initials = s.get("name").isBlank() ? "?" : s.get("name").substring(0, 1).toUpperCase();
        StringBuilder c = new StringBuilder();
        c.append("<div class=\"card\">");
        c.append("<div class=\"list-inline\" style=\"margin-bottom:18px\"><span class=\"avatar-ph\" style=\"width:56px;height:56px;font-size:20px\">").append(initials).append("</span>")
         .append("<div><h2 style=\"margin:0\">").append(Html.esc(s.get("name"))).append("</h2>")
         .append("<span class=\"pill pill-info\">Class ").append(Html.esc(s.get("className"))).append("-").append(Html.esc(s.get("section"))).append("</span> ")
         .append("<span class=\"small\">Admission No. ").append(Html.esc(s.get("admissionNo"))).append("</span></div></div>");

        c.append(detailSection("Basic Information", new String[][]{
            {"Academic Year", s.get("academicYear")}, {"Date of Birth", Html.fmtDate(s.get("dob"))}, {"Gender", s.get("gender")},
            {"Blood Group", s.get("bloodGroup")}, {"Nationality", s.get("nationality")}, {"Religion", s.get("religion")},
            {"Category", s.get("category")}, {"Mother Tongue", s.get("motherTongue")}, {"Aadhar No.", s.get("aadharNo")},
            {"Identification Marks", s.get("identificationMarks")}, {"House", s.get("house")}, {"Stream", s.get("stream")},
        }));
        c.append(detailSection("Parent / Guardian Information", new String[][]{
            {"Father's Name", s.get("fatherName")}, {"Father's Occupation", s.get("fatherOccupation")}, {"Father's Phone", s.get("fatherPhone")},
            {"Mother's Name", s.get("motherName")}, {"Mother's Occupation", s.get("motherOccupation")}, {"Mother's Phone", s.get("motherPhone")},
            {"Primary Contact Phone", s.get("parentPhone")},
        }));
        c.append(detailSection("Contact & Address", new String[][]{
            {"Address", s.get("address")}, {"City", s.get("city")}, {"State", s.get("state")}, {"Pin Code", s.get("pincode")}, {"Student Email", s.get("studentEmail")},
        }));
        c.append(detailSection("Academic & Other", new String[][]{
            {"Previous School", s.get("previousSchool")},
        }));

        c.append("<div class=\"btn-row\" style=\"margin-top:8px\">")
         .append("<a class=\"btn btn-outline\" href=\"/students?filterClass=").append(s.get("className")).append("\">\u2190 Back to Class ").append(Html.esc(s.get("className"))).append("</a>");
        if (session.role.equals("admin")) c.append("<a class=\"btn btn-primary\" href=\"/students?edit=").append(s.get("id")).append("&filterClass=").append(s.get("className")).append("\">Edit Student</a>");
        c.append("</div></div>");

        Res.html(ex, 200, Html.page("students", session, c.toString()));
    }

    private static String detailSection(String title, String[][] fields) {
        StringBuilder sb = new StringBuilder("<div class=\"divider\"></div><h4 class=\"detail-section-title\">").append(Html.esc(title)).append("</h4><div class=\"detail-grid\">");
        for (String[] f : fields) {
            String val = f[1] == null || f[1].isBlank() ? "\u2014" : Html.esc(f[1]);
            sb.append("<div class=\"detail-item\"><div class=\"detail-label\">").append(Html.esc(f[0])).append("</div><div class=\"detail-value\">").append(val).append("</div></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    private static String sectionHeading(String text) {
        return "<h4 style=\"grid-column:1/-1;margin:10px 0 2px;padding-top:10px;border-top:1px solid var(--border);color:var(--brand);font-size:13px;text-transform:uppercase;letter-spacing:.03em;\">" + Html.esc(text) + "</h4>";
    }

    private static String studentForm(LinkedHashMap<String, String> r) {
        boolean editing = r != null;
        String id = editing ? r.get("id") : "";
        java.util.function.Function<String, String> v = key -> editing ? r.getOrDefault(key, "") : "";
        return "<div class=\"card\"><h3>" + (editing ? "Edit Student" : "Add Student") + "</h3>" +
                "<form method=\"post\" action=\"/students/save\">" + Html.hidden("id", id) +
                "<div class=\"form-grid\">" +

                sectionHeading("Basic Information") +
                Html.textField("Full Name", "name", v.apply("name"), "text") +
                Html.textField("Admission No.", "admissionNo", v.apply("admissionNo"), "text") +
                Html.textField("Class", "className", v.apply("className"), "text") +
                Html.textField("Section", "section", v.apply("section"), "text") +
                Html.textField("Academic Year", "academicYear", v.apply("academicYear").isBlank() ? "2026-27" : v.apply("academicYear"), "text") +
                Html.textField("Date of Birth", "dob", v.apply("dob"), "date") +
                Html.selectField("Gender", "gender", new String[]{"Male", "Female", "Other"}, editing ? v.apply("gender") : "Male") +
                Html.selectField("Blood Group", "bloodGroup", new String[]{"", "A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}, v.apply("bloodGroup")) +
                Html.textField("Nationality", "nationality", v.apply("nationality").isBlank() ? "Indian" : v.apply("nationality"), "text") +
                Html.textField("Religion", "religion", v.apply("religion"), "text") +
                Html.selectField("Category", "category", new String[]{"General", "OBC", "SC", "ST", "Other"}, editing ? v.apply("category") : "General") +
                Html.textField("Mother Tongue", "motherTongue", v.apply("motherTongue"), "text") +
                Html.textField("Aadhar No.", "aadharNo", v.apply("aadharNo"), "text") +
                Html.textField("Identification Marks", "identificationMarks", v.apply("identificationMarks"), "text") +

                sectionHeading("Parent / Guardian Information") +
                Html.textField("Father's Name", "fatherName", v.apply("fatherName"), "text") +
                Html.textField("Father's Occupation", "fatherOccupation", v.apply("fatherOccupation"), "text") +
                Html.textField("Father's Phone", "fatherPhone", v.apply("fatherPhone"), "text") +
                Html.textField("Mother's Name", "motherName", v.apply("motherName"), "text") +
                Html.textField("Mother's Occupation", "motherOccupation", v.apply("motherOccupation"), "text") +
                Html.textField("Mother's Phone", "motherPhone", v.apply("motherPhone"), "text") +
                Html.textField("Primary Contact Phone", "parentPhone", v.apply("parentPhone"), "text") +

                sectionHeading("Contact & Address") +
                Html.textField("Residential Address", "address", v.apply("address"), "text") +
                Html.textField("City", "city", v.apply("city"), "text") +
                Html.textField("State", "state", v.apply("state").isBlank() ? "Madhya Pradesh" : v.apply("state"), "text") +
                Html.textField("Pin Code", "pincode", v.apply("pincode"), "text") +
                Html.textField("Student Email (optional)", "studentEmail", v.apply("studentEmail"), "email") +

                sectionHeading("Academic & Other") +
                Html.textField("Previous School (if any)", "previousSchool", v.apply("previousSchool"), "text") +
                Html.selectField("House", "house", new String[]{"Red", "Blue", "Green", "Yellow"}, editing ? v.apply("house") : "Red") +
                Html.textField("Stream (if applicable)", "stream", v.apply("stream"), "text") +

                "</div><div class=\"btn-row\" style=\"margin-top:14px\"><button class=\"btn btn-primary\" type=\"submit\">Save</button>" +
                "<a class=\"btn btn-outline\" href=\"/students\">Cancel</a></div></form></div>";
    }

    private static final String[] STUDENT_FORM_FIELDS = {
        "name", "admissionNo", "className", "section", "academicYear", "dob", "gender", "bloodGroup",
        "nationality", "religion", "category", "motherTongue", "aadharNo", "identificationMarks",
        "fatherName", "fatherOccupation", "fatherPhone", "motherName", "motherOccupation", "motherPhone", "parentPhone",
        "address", "city", "state", "pincode", "studentEmail", "previousSchool", "house", "stream"
    };

    private static void saveStudent(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String id = req.p("id");
        var row = id.isBlank() ? db.t("students").newRow() : db.t("students").find(id).orElse(db.t("students").newRow());
        if (id.isBlank()) row.put("id", db.newId("s"));
        for (String field : STUDENT_FORM_FIELDS) row.put(field, req.p(field));
        if (id.isBlank()) db.t("students").add(row); else db.t("students").save();
        Res.redirect(ex, "/students");
    }

    private static void deleteStudent(Req req, HttpExchange ex) throws IOException {
        Main.db.t("students").deleteById(req.p("id"));
        Res.redirect(ex, "/students");
    }

    private static void exportStudentsCsv(Req req, HttpExchange ex) throws IOException {
        List<List<String>> out = new ArrayList<>();
        out.add(Arrays.asList(STUDENT_FORM_FIELDS));
        for (var r : Main.db.t("students").all()) {
            List<String> line = new ArrayList<>();
            for (String field : STUDENT_FORM_FIELDS) line.add(r.getOrDefault(field, ""));
            out.add(line);
        }
        Res.download(ex, "students.csv", "text/csv", Csv.format(out).getBytes(StandardCharsets.UTF_8));
    }

    private static void importStudentsCsv(Req req, HttpExchange ex) throws IOException {
        String text = req.p("csvText");
        var parsed = Csv.parse(text);
        int count = 0;
        if (!parsed.isEmpty()) {
            List<String> header = parsed.get(0);
            Db db = Main.db;
            for (int i = 1; i < parsed.size(); i++) {
                List<String> line = parsed.get(i);
                Map<String, String> m = new HashMap<>();
                for (int c = 0; c < header.size() && c < line.size(); c++) m.put(header.get(c).trim(), line.get(c));
                if (m.getOrDefault("name", "").isBlank()) continue;
                var row = db.t("students").newRow();
                row.put("id", db.newId("s"));
                for (String field : STUDENT_FORM_FIELDS) row.put(field, m.getOrDefault(field, ""));
                db.t("students").add(row);
                count++;
            }
        }
        Res.redirect(ex, "/students");
    }

    private static void promoteStudents(Req req, HttpExchange ex) throws IOException {
        List<String> ids = req.rawMulti.getOrDefault("sel", java.util.Collections.emptyList());
        String targetClass = req.p("targetClass").trim(), targetSection = req.p("targetSection").trim();
        if (!ids.isEmpty() && !targetClass.isBlank() && !targetSection.isBlank()) {
            for (var r : Main.db.t("students").all()) {
                if (ids.contains(r.get("id"))) { r.put("className", targetClass); r.put("section", targetSection); }
            }
            Main.db.t("students").save();
        }
        Res.redirect(ex, "/students");
    }

    // =========================== TEACHERS ===========================
    private static void teachers(Req req, HttpExchange ex) throws IOException {
        boolean canEdit = req.session.role.equals("admin");
        var rows = Main.db.t("teachers").all();
        StringBuilder c = new StringBuilder("<div class=\"section-head\"><h2>\uD83E\uDDD1\u200D\uD83C\uDFEB Teachers</h2><span class=\"muted\">" + rows.size() + " record(s)</span></div>");
        if (canEdit) c.append(teacherForm(null));
        String editId = req.p("edit");
        if (canEdit && !editId.isBlank()) Main.db.t("teachers").find(editId).ifPresent(r -> c.append(teacherForm(r)));

        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>Emp Code</th><th>Designation</th><th>Subject</th><th>Phone</th><th>Email</th><th>Qualification</th><th>Joined</th><th>Class Teacher Of</th><th>Permissions</th>")
         .append(canEdit ? "<th>Actions</th>" : "").append("</tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"11\">No records yet.</td></tr>");
        for (var r : rows) {
            var assignedClasses = Main.db.t("classes").filter(cl -> r.get("id").equals(cl.getOrDefault("classTeacherId", "")));
            StringBuilder assignedLabel = new StringBuilder();
            for (var cl : assignedClasses) { if (assignedLabel.length() > 0) assignedLabel.append(", "); assignedLabel.append(cl.get("className")); }
            c.append("<tr><td>").append(Html.esc(r.get("name"))).append("</td><td>").append(Html.esc(r.get("empCode")))
             .append("</td><td>").append(Html.esc(r.get("designation"))).append("</td><td>").append(Html.esc(r.get("subject")))
             .append("</td><td>").append(Html.esc(r.get("phone"))).append("</td><td>").append(r.get("email").isBlank() ? "\u2014" : Html.esc(r.get("email")))
             .append("</td><td>").append(r.get("qualification").isBlank() ? "\u2014" : Html.esc(r.get("qualification")))
             .append("</td><td>").append(r.get("joiningDate").isBlank() ? "\u2014" : Html.fmtDate(r.get("joiningDate")))
             .append("</td><td>").append(assignedLabel.length() == 0 ? "\u2014" : "<span class=\"pill pill-info\">" + Html.esc(assignedLabel.toString()) + "</span>").append("</td><td>")
             .append("true".equals(r.get("canMarkAttendance")) ? "<span class=\"pill pill-success\">Attendance</span> " : "")
             .append("true".equals(r.get("canEnterMarks")) ? "<span class=\"pill pill-info\">Marks</span> " : "")
             .append("true".equals(r.get("canManageFees")) ? "<span class=\"pill pill-gold\">Fees</span>" : "").append("</td>");
            if (canEdit) c.append("<td class=\"btn-row\"><a class=\"btn btn-outline btn-sm\" href=\"/teachers?edit=").append(r.get("id")).append("\">Edit</a>")
                    .append("<form method=\"post\" action=\"/teachers/delete\" style=\"display:inline\">").append(Html.hidden("id", r.get("id")))
                    .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td>");
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("teachers", req.session, c.toString()));
    }

    private static String teacherForm(LinkedHashMap<String, String> r) {
        boolean editing = r != null;
        java.util.function.Function<String, String> v = key -> editing ? r.getOrDefault(key, "") : "";
        return "<div class=\"card\"><h3>" + (editing ? "Edit Teacher" : "Add Teacher") + "</h3><form method=\"post\" action=\"/teachers/save\">"
                + Html.hidden("id", editing ? r.get("id") : "") + "<div class=\"form-grid\">"
                + Html.textField("Full Name", "name", v.apply("name"), "text")
                + Html.textField("Employee Code", "empCode", v.apply("empCode"), "text")
                + Html.textField("Designation", "designation", v.apply("designation"), "text")
                + Html.textField("Subject", "subject", v.apply("subject"), "text")
                + Html.textField("Phone", "phone", v.apply("phone"), "text")
                + Html.textField("Email", "email", v.apply("email"), "email")
                + Html.textField("Qualification", "qualification", v.apply("qualification"), "text")
                + Html.textField("Date of Joining", "joiningDate", v.apply("joiningDate"), "date")
                + "</div><div class=\"form-grid\">"
                + Html.checkboxField("Can Mark Attendance", "canMarkAttendance", editing && "true".equals(r.get("canMarkAttendance")))
                + Html.checkboxField("Can Enter Marks", "canEnterMarks", editing && "true".equals(r.get("canEnterMarks")))
                + Html.checkboxField("Can Manage Fees", "canManageFees", editing && "true".equals(r.get("canManageFees")))
                + "</div><p class=\"hint\">Class teacher assignment is managed from the Classes &amp; Houses page.</p>"
                + "<div class=\"btn-row\"><button class=\"btn btn-primary\" type=\"submit\">Save</button><a class=\"btn btn-outline\" href=\"/teachers\">Cancel</a></div></form></div>";
    }

    private static void saveTeacher(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String id = req.p("id");
        var row = id.isBlank() ? db.t("teachers").newRow() : db.t("teachers").find(id).orElse(db.t("teachers").newRow());
        if (id.isBlank()) row.put("id", db.newId("t"));
        row.put("name", req.p("name")); row.put("empCode", req.p("empCode")); row.put("designation", req.p("designation"));
        row.put("subject", req.p("subject")); row.put("phone", req.p("phone")); row.put("email", req.p("email"));
        row.put("qualification", req.p("qualification")); row.put("joiningDate", req.p("joiningDate"));
        row.put("canMarkAttendance", String.valueOf(req.checked("canMarkAttendance")));
        row.put("canEnterMarks", String.valueOf(req.checked("canEnterMarks")));
        row.put("canManageFees", String.valueOf(req.checked("canManageFees")));
        if (id.isBlank()) db.t("teachers").add(row); else db.t("teachers").save();
        Res.redirect(ex, "/teachers");
    }

    private static void deleteTeacher(Req req, HttpExchange ex) throws IOException {
        Main.db.t("teachers").deleteById(req.p("id"));
        Res.redirect(ex, "/teachers");
    }

    // =========================== CLASSES & HOUSES ===========================
    private static void classes(Req req, HttpExchange ex) throws IOException {
        boolean canEdit = req.session.role.equals("admin");
        Db db = Main.db;
        var teachers = db.t("teachers").all();
        StringBuilder c = new StringBuilder("<div class=\"section-head\"><h2>\uD83C\uDFF7 Classes</h2></div>");
        if (canEdit) {
            c.append("<div class=\"card\"><h3>Add Class</h3><form method=\"post\" action=\"/classes/save\"><div class=\"form-grid\">")
             .append(Html.textField("Class Name", "className", "", "text"))
             .append(Html.textField("Sections (comma-separated)", "sections", "", "text"))
             .append("<div class=\"field\"><label>Class Teacher</label><select name=\"classTeacherId\"><option value=\"\">-- None assigned --</option>");
            for (var t : teachers) c.append("<option value=\"").append(t.get("id")).append("\">").append(Html.esc(t.get("name"))).append(" (").append(Html.esc(t.get("subject"))).append(")</option>");
            c.append("</select></div>")
             .append("</div><button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");
        }
        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Class</th><th>Sections</th><th>Class Teacher</th>")
         .append(canEdit ? "<th>Reassign</th><th>Actions</th>" : "").append("</tr></thead><tbody>");
        if (db.t("classes").all().isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"5\">No classes yet.</td></tr>");
        for (var r : db.t("classes").all()) {
            String teacherId = r.getOrDefault("classTeacherId", "");
            var teacher = teacherId.isBlank() ? null : db.t("teachers").find(teacherId).orElse(null);
            c.append("<tr><td>").append(Html.esc(r.get("className"))).append("</td><td>").append(Html.esc(r.get("sections")))
             .append("</td><td>").append(teacher != null ? Html.esc(teacher.get("name")) : "<span class=\"pill pill-gold\">Unassigned</span>").append("</td>");
            if (canEdit) {
                c.append("<td><form method=\"post\" action=\"/classes/assign\" style=\"display:flex;gap:6px\">").append(Html.hidden("id", r.get("id")))
                 .append("<select name=\"classTeacherId\"><option value=\"\">-- None --</option>");
                for (var t : teachers) c.append("<option value=\"").append(t.get("id")).append("\"").append(t.get("id").equals(teacherId) ? " selected" : "").append(">").append(Html.esc(t.get("name"))).append("</option>");
                c.append("</select><button class=\"btn btn-outline btn-sm\" type=\"submit\">Assign</button></form></td>");
                c.append("<td><form method=\"post\" action=\"/classes/delete\">").append(Html.hidden("id", r.get("id")))
                 .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td>");
            }
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");

        c.append("<div class=\"section-head\" style=\"margin-top:24px\"><h2>\uD83C\uDFE0 Houses</h2></div>");
        if (canEdit) c.append("<div class=\"card\"><h3>Add House</h3><form method=\"post\" action=\"/houses/save\"><div class=\"form-grid\">")
                .append(Html.textField("House Name", "name", "", "text"))
                .append(Html.textField("Captain", "captain", "", "text"))
                .append(Html.textField("Points", "points", "0", "number"))
                .append("</div><button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");
        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>House</th><th>Captain</th><th>Points</th>")
         .append(canEdit ? "<th>Actions</th>" : "").append("</tr></thead><tbody>");
        for (var r : db.t("houses").all()) {
            c.append("<tr><td>").append(Html.esc(r.get("name"))).append("</td><td>").append(Html.esc(r.get("captain")))
             .append("</td><td>").append(Html.esc(r.get("points"))).append("</td>");
            if (canEdit) c.append("<td><form method=\"post\" action=\"/houses/delete\">").append(Html.hidden("id", r.get("id")))
                    .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td>");
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("classes", req.session, c.toString()));
    }

    private static void saveClass(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("classes").newRow();
        row.put("id", db.newId("cl")); row.put("className", req.p("className")); row.put("sections", req.p("sections")); row.put("classTeacherId", req.p("classTeacherId"));
        db.t("classes").add(row);
        Res.redirect(ex, "/classes");
    }

    private static void assignClassTeacher(Req req, HttpExchange ex) throws IOException {
        var cls = Main.db.t("classes").find(req.p("id")).orElse(null);
        if (cls != null) { cls.put("classTeacherId", req.p("classTeacherId")); Main.db.t("classes").save(); }
        Res.redirect(ex, "/classes");
    }
    private static void deleteClass(Req req, HttpExchange ex) throws IOException { Main.db.t("classes").deleteById(req.p("id")); Res.redirect(ex, "/classes"); }

    private static void saveHouse(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("houses").newRow();
        row.put("id", db.newId("h")); row.put("name", req.p("name")); row.put("captain", req.p("captain")); row.put("points", req.p("points"));
        db.t("houses").add(row);
        Res.redirect(ex, "/classes");
    }
    private static void deleteHouse(Req req, HttpExchange ex) throws IOException { Main.db.t("houses").deleteById(req.p("id")); Res.redirect(ex, "/classes"); }

    // =========================== SUBJECTS ===========================
    private static void subjects(Req req, HttpExchange ex) throws IOException {
        boolean canEdit = req.session.role.equals("admin");
        Db db = Main.db;
        StringBuilder c = new StringBuilder("<div class=\"section-head\"><h2>\uD83D\uDCD8 Subjects</h2></div>");
        if (canEdit) c.append("<div class=\"card\"><h3>Add Subject</h3><form method=\"post\" action=\"/subjects/save\"><div class=\"form-grid\">")
                .append(Html.textField("Subject Name", "name", "", "text")).append(Html.textField("Code", "code", "", "text"))
                .append(Html.textField("Class", "className", "", "text")).append(Html.textField("Assigned Teacher", "teacher", "", "text"))
                .append("</div><button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");
        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>Code</th><th>Class</th><th>Teacher</th>")
         .append(canEdit ? "<th>Actions</th>" : "").append("</tr></thead><tbody>");
        for (var r : db.t("subjects").all()) {
            c.append("<tr><td>").append(Html.esc(r.get("name"))).append("</td><td>").append(Html.esc(r.get("code")))
             .append("</td><td>").append(Html.esc(r.get("className"))).append("</td><td>").append(Html.esc(r.get("teacher"))).append("</td>");
            if (canEdit) c.append("<td><form method=\"post\" action=\"/subjects/delete\">").append(Html.hidden("id", r.get("id")))
                    .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td>");
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("subjects", req.session, c.toString()));
    }
    private static void saveSubject(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("subjects").newRow();
        row.put("id", db.newId("sub")); row.put("name", req.p("name")); row.put("code", req.p("code")); row.put("className", req.p("className")); row.put("teacher", req.p("teacher"));
        db.t("subjects").add(row);
        Res.redirect(ex, "/subjects");
    }
    private static void deleteSubject(Req req, HttpExchange ex) throws IOException { Main.db.t("subjects").deleteById(req.p("id")); Res.redirect(ex, "/subjects"); }

    // =========================== FACULTY (read-only) ===========================
    private static void faculty(Req req, HttpExchange ex) throws IOException {
        StringBuilder c = new StringBuilder("<div class=\"section-head\"><h2>\uD83D\uDC65 Faculty Directory</h2></div><div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>Role</th><th>Subject</th><th>Phone</th></tr></thead><tbody>");
        for (var r : Main.db.t("teachers").all())
            c.append("<tr><td>").append(Html.esc(r.get("name"))).append("</td><td>").append(Html.esc(r.get("designation")))
             .append("</td><td>").append(Html.esc(r.get("subject"))).append("</td><td>").append(Html.esc(r.get("phone"))).append("</td></tr>");
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("faculty", req.session, c.toString()));
    }

    // =========================== ADMISSIONS ===========================
    private static void admissions(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var rows = db.t("admissions").all();
        StringBuilder c = new StringBuilder("<div class=\"section-head\"><h2>\uD83D\uDCDD Admission Enquiries</h2></div>");
        c.append("<div class=\"card\"><h3>Add Enquiry</h3><form method=\"post\" action=\"/admissions/save\"><div class=\"form-grid\">")
         .append(Html.textField("Student Name", "name", "", "text")).append(Html.textField("DOB", "dob", "", "date"))
         .append(Html.textField("Class Applying For", "className", "", "text")).append(Html.textField("Parent Phone", "parentPhone", "", "text"))
         .append(Html.textField("Address", "address", "", "text"))
         .append(Html.selectField("Status", "status", new String[]{"New", "Followed Up", "Converted", "Rejected"}, "New"))
         .append("</div>").append(Html.textareaField("Notes", "notes", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");

        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Name</th><th>DOB</th><th>Class</th><th>Parent Phone</th><th>Status</th><th>Notes</th><th>Actions</th></tr></thead><tbody>");
        for (var r : rows) {
            c.append("<tr><td>").append(Html.esc(r.get("name"))).append("</td><td>").append(Html.fmtDate(r.get("dob")))
             .append("</td><td>").append(Html.esc(r.get("className"))).append("</td><td>").append(Html.esc(r.get("parentPhone")))
             .append("</td><td>").append(Html.pill(r.get("status"))).append("</td><td>").append(Html.esc(r.get("notes"))).append("</td><td class=\"btn-row\">");
            if (!r.get("status").equals("Converted"))
                c.append("<form method=\"post\" action=\"/admissions/convert\" style=\"display:inline\">").append(Html.hidden("id", r.get("id")))
                 .append("<button class=\"btn btn-accent btn-sm\" type=\"submit\">Convert to Student</button></form>");
            else c.append("<span class=\"pill pill-success\">Converted</span>");
            c.append("<form method=\"post\" action=\"/admissions/delete\" style=\"display:inline\">").append(Html.hidden("id", r.get("id")))
             .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td></tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("admissions", req.session, c.toString()));
    }
    private static void saveAdmission(Req req, HttpExchange ex) throws IOException {
        Util.requireAdmin(req.session);
        Db db = Main.db;
        var row = db.t("admissions").newRow();
        row.put("id", db.newId("ad")); row.put("name", req.p("name")); row.put("dob", req.p("dob")); row.put("className", req.p("className"));
        row.put("parentPhone", req.p("parentPhone")); row.put("address", req.p("address")); row.put("status", req.p("status")); row.put("notes", req.p("notes"));
        db.t("admissions").add(row);
        Res.redirect(ex, "/admissions");
    }
    private static void deleteAdmission(Req req, HttpExchange ex) throws IOException { Main.db.t("admissions").deleteById(req.p("id")); Res.redirect(ex, "/admissions"); }
    private static void convertAdmission(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var lead = db.t("admissions").find(req.p("id")).orElse(null);
        if (lead != null) {
            var s = db.t("students").newRow();
            s.put("id", db.newId("s")); s.put("name", lead.get("name")); s.put("admissionNo", "VS" + (1000 + new Random().nextInt(9000)));
            s.put("className", lead.get("className")); s.put("section", "A"); s.put("dob", lead.get("dob")); s.put("gender", "");
            s.put("parentPhone", lead.get("parentPhone")); s.put("house", ""); s.put("stream", "");
            db.t("students").add(s);
            lead.put("status", "Converted");
            db.t("admissions").save();
        }
        Res.redirect(ex, "/admissions");
    }

    // =========================== ATTENDANCE ===========================
    private static void attendance(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean staffView = s.role.equals("admin") || s.role.equals("teacher");
        StringBuilder c = new StringBuilder();
        if (staffView) {
            String cls = req.p("className"), sec = req.p("section"), date = req.p("date", Db.today());
            var classNames = new TreeSet<String>();
            boolean isAdmin = s.role.equals("admin");
            if (isAdmin) {
                for (var st : db.t("students").all()) classNames.add(st.get("className"));
            } else {
                classNames.addAll(teacherAssignedClasses(s.teacherId));
                if (classNames.isEmpty()) {
                    c.append("<div class=\"card\"><h2>\u2705 Mark Attendance</h2><p>You are not currently assigned as a Class Teacher for any class, so you cannot mark attendance here. Ask your Admin to assign you as a Class Teacher from the Classes &amp; Houses page.</p></div>");
                    Res.html(ex, 200, Html.page("attendance", s, c.toString()));
                    return;
                }
                if (cls.isBlank() && classNames.size() == 1) cls = classNames.iterator().next();
                if (!cls.isBlank() && !classNames.contains(cls)) throw new Util.AccessDenied("You can only mark attendance for classes where you are the assigned Class Teacher.");
            }

            c.append("<div class=\"card\"><div class=\"section-head\"><h2>\u2705 Mark Attendance</h2></div>")
             .append("<form method=\"get\" action=\"/attendance\"><div class=\"form-grid\">")
             .append("<div class=\"field\"><label>Class</label><select name=\"className\"><option value=\"\">-- Select --</option>");
            for (String cn : classNames) c.append("<option value=\"").append(cn).append("\"").append(cn.equals(cls) ? " selected" : "").append(">Class ").append(cn).append("</option>");
            c.append("</select></div>")
             .append(Html.textField("Section (optional)", "section", sec, "text"))
             .append(Html.textField("Date", "date", date, "date"))
             .append("</div><button class=\"btn btn-primary btn-sm\" type=\"submit\">Load Roster</button></form>");

            if (!cls.isBlank()) {
                final String fCls = cls;
                var roster = db.t("students").filter(st -> st.get("className").equals(fCls) && (sec.isBlank() || st.get("section").equalsIgnoreCase(sec)));
                if (roster.isEmpty()) {
                    c.append("<p class=\"small\" style=\"margin-top:14px\">No students found for this class/section.</p>");
                } else {
                    long presentCount = 0;
                    for (var st : roster) {
                        var existing = db.t("attendance").filter(a -> a.get("studentId").equals(st.get("id")) && a.get("date").equals(date));
                        if (existing.isEmpty() || existing.get(0).get("status").equals("Present")) presentCount++;
                    }
                    c.append("<div class=\"divider\"></div>");
                    c.append("<div class=\"section-head\"><h3 style=\"margin:0\">Class ").append(Html.esc(cls)).append(sec.isBlank() ? "" : "-" + Html.esc(sec))
                     .append(" \u2014 ").append(Html.fmtDate(date)).append("</h3><span class=\"muted\">").append(roster.size()).append(" student(s), ").append(presentCount).append(" present so far</span></div>");
                    c.append("<form method=\"post\" action=\"/attendance/save\">").append(Html.hidden("className", cls)).append(Html.hidden("section", sec)).append(Html.hidden("date", date));
                    c.append("<div class=\"btn-row\" style=\"margin-bottom:12px\">")
                     .append("<button class=\"btn btn-outline btn-sm\" type=\"submit\" name=\"quickAction\" value=\"allPresent\">\u2713 Mark All Present</button>")
                     .append("<button class=\"btn btn-outline btn-sm\" type=\"submit\" name=\"quickAction\" value=\"allAbsent\">\u2715 Mark All Absent</button>")
                     .append("</div>");
                    c.append("<div class=\"table-wrap\"><table><thead><tr><th>Student</th><th>Admission No.</th><th>Attendance</th></tr></thead><tbody>");
                    for (var st : roster) {
                        var existing = db.t("attendance").filter(a -> a.get("studentId").equals(st.get("id")) && a.get("date").equals(date));
                        String status = existing.isEmpty() ? "Present" : existing.get(0).get("status");
                        String sid = st.get("id");
                        String initials = st.get("name").isBlank() ? "?" : st.get("name").substring(0, 1).toUpperCase();
                        c.append("<tr><td><div class=\"list-inline\"><span class=\"avatar-ph\">").append(initials).append("</span>").append(Html.esc(st.get("name"))).append("</div></td>")
                         .append("<td>").append(Html.esc(st.get("admissionNo"))).append("</td><td>")
                         .append("<span class=\"attn-toggle\">")
                         .append("<input class=\"attn-radio\" type=\"radio\" id=\"p_").append(sid).append("\" name=\"status_").append(sid).append("\" value=\"Present\"").append(status.equals("Present") ? " checked" : "").append("/>")
                         .append("<label class=\"attn-btn present\" for=\"p_").append(sid).append("\">Present</label>")
                         .append("<input class=\"attn-radio\" type=\"radio\" id=\"a_").append(sid).append("\" name=\"status_").append(sid).append("\" value=\"Absent\"").append(status.equals("Absent") ? " checked" : "").append("/>")
                         .append("<label class=\"attn-btn absent\" for=\"a_").append(sid).append("\">Absent</label>")
                         .append("</span>").append(Html.hidden("ids", sid)).append("</td></tr>");
                    }
                    c.append("</tbody></table></div><button class=\"btn btn-primary\" style=\"margin-top:14px\" type=\"submit\">Submit Attendance</button></form>");
                }
            }
            c.append("</div>");
        } else {
            String sid = s.primaryStudentId();
            var rows = db.t("attendance").filter(a -> a.get("studentId").equals(sid));
            rows.sort((a, b) -> b.get("date").compareTo(a.get("date")));
            long present = rows.stream().filter(a -> a.get("status").equals("Present")).count();
            int pct = rows.isEmpty() ? 0 : (int) Math.round(present * 100.0 / rows.size());
            c.append("<div class=\"grid grid-3\">")
             .append(Html.statCard("Attendance %", pct + "%", "card-accent-top"))
             .append(Html.statCard("Days Present", String.valueOf(present), ""))
             .append(Html.statCard("Days Absent", String.valueOf(rows.size() - present), ""))
             .append("</div><div class=\"card\"><h2>Attendance History</h2><div class=\"table-wrap\"><table><thead><tr><th>Date</th><th>Status</th></tr></thead><tbody>");
            if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"2\">No attendance records yet.</td></tr>");
            for (var r : rows) c.append("<tr><td>").append(Html.fmtDate(r.get("date"))).append("</td><td>").append(Html.pill(r.get("status"))).append("</td></tr>");
            c.append("</tbody></table></div></div>");
        }
        Res.html(ex, 200, Html.page("attendance", s, c.toString()));
    }

    private static void saveAttendance(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String cls = req.p("className"), sec = req.p("section"), date = req.p("date");
        if (!req.session.role.equals("admin") && !teacherAssignedClasses(req.session.teacherId).contains(cls))
            throw new Util.AccessDenied("You can only mark attendance for classes where you are the assigned Class Teacher.");
        String quickAction = req.p("quickAction");
        List<String> ids = req.rawMulti.getOrDefault("ids", java.util.Collections.emptyList());
        for (String sid : ids) {
            var st = db.t("students").find(sid).orElse(null);
            if (st == null) continue;
            String status;
            if ("allPresent".equals(quickAction)) status = "Present";
            else if ("allAbsent".equals(quickAction)) status = "Absent";
            else status = req.p("status_" + sid, "Present");
            var existing = db.t("attendance").filter(a -> a.get("studentId").equals(sid) && a.get("date").equals(date));
            if (!existing.isEmpty()) {
                existing.get(0).put("status", status);
            } else {
                var row = db.t("attendance").newRow();
                row.put("id", db.newId("att")); row.put("studentId", sid); row.put("studentName", st.get("name"));
                row.put("className", st.get("className")); row.put("section", st.get("section")); row.put("date", date); row.put("status", status);
                db.t("attendance").rows.add(row);
            }
        }
        db.t("attendance").save();
        Res.redirect(ex, "/attendance?className=" + cls + "&section=" + sec + "&date=" + date);
    }

    // =========================== TIMETABLE ===========================
    private static final int TIMETABLE_PERIODS = 7;

    private static void timetable(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean canEdit = s.role.equals("admin");
        var classNames = new TreeSet<String>();
        for (var r : db.t("timetables").all()) classNames.add(r.get("className"));
        String selectedClass = req.p("className");
        if (selectedClass.isBlank()) {
            if (s.role.equals("student")) { var st = db.t("students").find(s.studentId).orElse(null); if (st != null) selectedClass = st.get("className"); }
            else if (s.role.equals("parent") && !s.wardIds.isEmpty()) { var st = db.t("students").find(s.wardIds.get(0)).orElse(null); if (st != null) selectedClass = st.get("className"); }
            else if (!classNames.isEmpty()) selectedClass = classNames.iterator().next();
        }
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        final String cls = selectedClass;

        StringBuilder c = new StringBuilder("<div class=\"card\"><div class=\"section-head\"><h2>\uD83D\uDDD3 Weekly Timetable</h2>");
        c.append("<form method=\"get\" style=\"display:flex;gap:8px;align-items:center\"><select name=\"className\">");
        for (String cn : classNames) c.append("<option value=\"").append(cn).append("\"").append(cn.equals(cls) ? " selected" : "").append(">Class ").append(cn).append("</option>");
        c.append("</select><button class=\"btn btn-outline btn-sm\" type=\"submit\">View</button></form></div>");

        if (canEdit) c.append("<form method=\"post\" action=\"/timetable/save\">").append(Html.hidden("className", cls));
        c.append("<div class=\"table-wrap timetable-grid\"><table><thead><tr><th class=\"tt-day-col\">Day</th>");
        for (int p = 1; p <= TIMETABLE_PERIODS; p++) c.append("<th>Period ").append(p).append("</th>");
        c.append("</tr></thead><tbody>");
        for (String d : days) {
            var row = db.t("timetables").filter(r -> r.get("className").equals(cls) && r.get("day").equals(d));
            String periodsStr = row.isEmpty() ? "" : row.get(0).get("periods");
            String[] parts = periodsStr.isBlank() ? new String[0] : periodsStr.split(",");
            c.append("<tr><td class=\"tt-day-col\"><b>").append(fullDayName(d)).append("</b></td>");
            for (int p = 0; p < TIMETABLE_PERIODS; p++) {
                String val = p < parts.length ? parts[p].trim() : "";
                if (canEdit) c.append("<td><input class=\"tt-cell\" type=\"text\" name=\"period_").append(d).append("_").append(p).append("\" value=\"").append(Html.esc(val)).append("\" placeholder=\"\u2014\"/></td>");
                else c.append("<td class=\"tt-cell-view\">").append(val.isBlank() ? "\u2014" : Html.esc(val)).append("</td>");
            }
            c.append("</tr>");
        }
        c.append("</tbody></table></div>");
        if (canEdit) c.append("<button class=\"btn btn-primary\" style=\"margin-top:14px\" type=\"submit\">Save Timetable</button></form>");
        c.append("</div>");
        Res.html(ex, 200, Html.page("timetable", s, c.toString()));
    }

    private static String fullDayName(String abbr) {
        return switch (abbr) {
            case "Mon" -> "Monday"; case "Tue" -> "Tuesday"; case "Wed" -> "Wednesday";
            case "Thu" -> "Thursday"; case "Fri" -> "Friday"; case "Sat" -> "Saturday"; default -> abbr;
        };
    }

    private static void saveTimetable(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String cls = req.p("className");
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            StringBuilder periods = new StringBuilder();
            for (int p = 0; p < TIMETABLE_PERIODS; p++) {
                String val = req.p("period_" + d + "_" + p).trim();
                if (periods.length() > 0) periods.append(",");
                periods.append(val);
            }
            // trim trailing empty entries for a clean stored value
            String joined = periods.toString();
            while (joined.endsWith(",")) joined = joined.substring(0, joined.length() - 1);
            var row = db.t("timetables").filter(r -> r.get("className").equals(cls) && r.get("day").equals(d));
            if (!row.isEmpty()) row.get(0).put("periods", joined);
            else {
                var nr = db.t("timetables").newRow();
                nr.put("id", db.newId("tt")); nr.put("className", cls); nr.put("day", d); nr.put("periods", joined);
                db.t("timetables").rows.add(nr);
            }
        }
        db.t("timetables").save();
        Res.redirect(ex, "/timetable?className=" + cls);
    }

    // =========================== HOMEWORK ===========================
    private static void homework(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        StringBuilder c = new StringBuilder();
        if (s.role.equals("admin") || s.role.equals("teacher")) {
            c.append("<div class=\"card\"><h3>\uD83D\uDCDA Assign Homework</h3><form method=\"post\" action=\"/homework/save\"><div class=\"form-grid\">")
             .append(Html.textField("Class", "className", "", "text")).append(Html.textField("Subject", "subject", "", "text"))
             .append(Html.textField("Title", "title", "", "text")).append(Html.textField("Due Date", "dueDate", "", "date"))
             .append("</div>").append(Html.textareaField("Description", "description", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");
            c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Class</th><th>Subject</th><th>Title</th><th>Due Date</th><th>Actions</th></tr></thead><tbody>");
            for (var r : db.t("homework").all()) {
                c.append("<tr><td>").append(Html.esc(r.get("className"))).append("</td><td>").append(Html.esc(r.get("subject")))
                 .append("</td><td>").append(Html.esc(r.get("title"))).append("</td><td>").append(Html.fmtDate(r.get("dueDate")))
                 .append("</td><td><form method=\"post\" action=\"/homework/delete\">").append(Html.hidden("id", r.get("id")))
                 .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td></tr>");
            }
            c.append("</tbody></table></div></div>");
        } else {
            String sid = s.primaryStudentId();
            var student = db.t("students").find(sid).orElse(null);
            String cls = student != null ? student.get("className") : "";
            var list = db.t("homework").filter(h -> h.get("className").equals(cls));
            c.append("<div class=\"card\"><h2>\uD83D\uDCDA Homework \u2014 Class ").append(Html.esc(cls)).append("</h2><div class=\"table-wrap\"><table><thead><tr><th>Subject</th><th>Title</th><th>Description</th><th>Due Date</th><th>Status</th>")
             .append(s.role.equals("student") ? "<th>Action</th>" : "").append("</tr></thead><tbody>");
            if (list.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"6\">No homework assigned.</td></tr>");
            for (var h : list) {
                boolean submitted = !db.t("homework_submissions").filter(x -> x.get("homeworkId").equals(h.get("id")) && x.get("studentId").equals(sid)).isEmpty();
                c.append("<tr><td>").append(Html.esc(h.get("subject"))).append("</td><td>").append(Html.esc(h.get("title")))
                 .append("</td><td>").append(Html.esc(h.get("description"))).append("</td><td>").append(Html.fmtDate(h.get("dueDate")))
                 .append("</td><td>").append(submitted ? "<span class=\"pill pill-success\">Submitted</span>" : "<span class=\"pill pill-gold\">Pending</span>").append("</td>");
                if (s.role.equals("student")) {
                    c.append("<td>");
                    if (!submitted) c.append("<form method=\"post\" action=\"/homework/submit\">").append(Html.hidden("homeworkId", h.get("id")))
                            .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Mark Submitted</button></form>");
                    else c.append("\u2014");
                    c.append("</td>");
                }
                c.append("</tr>");
            }
            c.append("</tbody></table></div></div>");
        }
        Res.html(ex, 200, Html.page("homework", s, c.toString()));
    }
    private static void saveHomework(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("homework").newRow();
        row.put("id", db.newId("hw")); row.put("className", req.p("className")); row.put("subject", req.p("subject"));
        row.put("title", req.p("title")); row.put("description", req.p("description")); row.put("dueDate", req.p("dueDate"));
        db.t("homework").add(row);
        Res.redirect(ex, "/homework");
    }
    private static void deleteHomework(Req req, HttpExchange ex) throws IOException { Main.db.t("homework").deleteById(req.p("id")); Res.redirect(ex, "/homework"); }
    private static void submitHomework(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("homework_submissions").newRow();
        row.put("id", db.newId("hs")); row.put("homeworkId", req.p("homeworkId")); row.put("studentId", req.session.studentId); row.put("submittedAt", Db.today());
        db.t("homework_submissions").add(row);
        Res.redirect(ex, "/homework");
    }

    // =========================== EXAMS & MARKS ===========================
    private static void exams(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        StringBuilder c = new StringBuilder();
        if (s.role.equals("admin") || s.role.equals("teacher")) {
            c.append("<div class=\"card\"><div class=\"section-head\"><h2>Create Exam</h2></div>")
             .append("<form method=\"post\" action=\"/exams/save\"><div class=\"form-grid\">")
             .append(Html.textField("Exam Title", "title", "", "text")).append(Html.textField("Class", "className", "", "text")).append("</div>")
             .append("<label>Subjects &amp; Max Marks (up to 6)</label>");
            for (int i = 0; i < 6; i++) {
                c.append("<div class=\"form-grid\" style=\"margin-bottom:6px\">")
                 .append("<div class=\"field\"><input type=\"text\" name=\"subjName").append(i).append("\" placeholder=\"Subject name\"/></div>")
                 .append("<div class=\"field\"><input type=\"number\" name=\"subjMax").append(i).append("\" placeholder=\"Max marks\" value=\"100\"/></div></div>");
            }
            c.append("<button class=\"btn btn-primary\" type=\"submit\">Save Exam</button></form></div>");

            c.append("<div class=\"card\"><h2>\uD83D\uDCDD Exams</h2><div class=\"table-wrap\"><table><thead><tr><th>Title</th><th>Class</th><th>Subjects</th><th>Action</th></tr></thead><tbody>");
            if (db.t("exams").all().isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"4\">No exams created yet.</td></tr>");
            for (var e : db.t("exams").all()) {
                c.append("<tr><td>").append(Html.esc(e.get("title"))).append("</td><td>").append(Html.esc(e.get("className")))
                 .append("</td><td>").append(Html.esc(e.get("subjects").replace(";", ", ").replaceAll(":\\d+", "")))
                 .append("</td><td><a class=\"btn btn-sm btn-accent\" href=\"/exams/marks?examId=").append(e.get("id")).append("\">Enter Marks</a></td></tr>");
            }
            c.append("</tbody></table></div></div>");
        } else {
            String sid = s.primaryStudentId();
            var exams = db.t("exams").all();
            String examId = req.p("examId", exams.isEmpty() ? "" : exams.get(0).get("id"));
            c.append("<div class=\"card\"><div class=\"section-head\"><h2>Report Card</h2><form method=\"get\" style=\"display:flex;gap:8px;align-items:center\"><select name=\"examId\">");
            for (var e : exams) c.append("<option value=\"").append(e.get("id")).append("\"").append(e.get("id").equals(examId) ? " selected" : "").append(">").append(Html.esc(e.get("title"))).append("</option>");
            c.append("</select><button class=\"btn btn-outline btn-sm\" type=\"submit\">View</button></form></div>");
            c.append(reportCardHtml(examId, sid));
            c.append("</div>");

            c.append("<div class=\"card\"><h2>Grade History</h2><div class=\"table-wrap\"><table><thead><tr><th>Term</th><th>Percentage</th><th>Grade</th><th>Result</th></tr></thead><tbody>");
            var gh = db.t("grade_history").filter(g -> g.get("studentId").equals(sid));
            if (gh.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"4\">No past records.</td></tr>");
            for (var g : gh) c.append("<tr><td>").append(Html.esc(g.get("term"))).append("</td><td>").append(g.get("percentage")).append("%</td><td>")
                    .append(Html.esc(g.get("grade"))).append("</td><td>").append(Html.esc(g.get("result"))).append("</td></tr>");
            c.append("</tbody></table></div></div>");
        }
        Res.html(ex, 200, Html.page("exams", s, c.toString()));
    }

    private static String reportCardHtml(String examId, String sid) {
        Db db = Main.db;
        var exam = db.t("exams").find(examId).orElse(null);
        if (exam == null) return "<p class=\"small\">No exams available yet.</p>";
        Map<String, Integer> subjMax = new LinkedHashMap<>();
        for (String part : exam.get("subjects").split(";")) {
            String[] kv = part.split(":");
            if (kv.length == 2) subjMax.put(kv[0], Util.inum(kv[1]));
        }
        var mine = db.t("exam_marks").filter(m -> m.get("examId").equals(examId) && m.get("studentId").equals(sid));
        Map<String, Integer> myMarks = new LinkedHashMap<>();
        if (!mine.isEmpty()) for (String part : mine.get(0).get("marks").split(";")) {
            String[] kv = part.split(":"); if (kv.length == 2) myMarks.put(kv[0], Util.inum(kv[1]));
        }
        var classMarksRows = db.t("exam_marks").filter(m -> m.get("examId").equals(examId));
        List<double[]> ranked = new ArrayList<>(); // [pctIndexPlaceholder] just track pct values with studentId separately
        List<String> rankedIds = new ArrayList<>();
        List<Double> rankedPcts = new ArrayList<>();
        int totalMax = subjMax.values().stream().mapToInt(Integer::intValue).sum();
        for (var mr : classMarksRows) {
            Map<String, Integer> mk = new LinkedHashMap<>();
            for (String part : mr.get("marks").split(";")) { String[] kv = part.split(":"); if (kv.length == 2) mk.put(kv[0], Util.inum(kv[1])); }
            int obtained = 0; for (String subj : subjMax.keySet()) obtained += mk.getOrDefault(subj, 0);
            double pct = totalMax > 0 ? obtained * 100.0 / totalMax : 0;
            rankedIds.add(mr.get("studentId")); rankedPcts.add(pct);
        }
        // sort indices by pct desc
        Integer[] order = new Integer[rankedIds.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Double.compare(rankedPcts.get(b), rankedPcts.get(a)));
        int rank = -1;
        for (int i = 0; i < order.length; i++) if (rankedIds.get(order[i]).equals(sid)) { rank = i + 1; break; }

        int myObtained = 0; for (String subj : subjMax.keySet()) myObtained += myMarks.getOrDefault(subj, 0);
        double myPct = mine.isEmpty() || totalMax == 0 ? 0 : myObtained * 100.0 / totalMax;

        StringBuilder c = new StringBuilder("<div class=\"table-wrap\"><table><thead><tr><th>Subject</th><th>Max Marks</th><th>Obtained</th></tr></thead><tbody>");
        for (var e : subjMax.entrySet())
            c.append("<tr><td>").append(Html.esc(e.getKey())).append("</td><td>").append(e.getValue()).append("</td><td>")
             .append(mine.isEmpty() ? "\u2014" : myMarks.getOrDefault(e.getKey(), 0)).append("</td></tr>");
        c.append("</tbody></table></div>");
        c.append("<div class=\"grid grid-3\" style=\"margin-top:14px\">")
         .append(Html.statCard("Percentage", mine.isEmpty() ? "\u2014" : String.format("%.1f%%", myPct), ""))
         .append(Html.statCard("Grade", mine.isEmpty() ? "\u2014" : Util.gradeFromPct(myPct), ""))
         .append(Html.statCard("Class Rank", mine.isEmpty() ? "\u2014" : "#" + rank, ""))
         .append("</div><a class=\"btn btn-outline\" style=\"margin-top:12px\" href=\"/exams/marks/print?examId=" + examId + "&studentId=" + sid + "\" target=\"_blank\">\uD83D\uDDA8 Print Report Card</a>");
        return c.toString();
    }

    private static void printReportCard(Req req, HttpExchange ex) throws IOException {
        String examId = req.p("examId"), studentId = req.p("studentId");
        var student = Main.db.t("students").find(studentId).orElse(null);
        var exam = Main.db.t("exams").find(examId).orElse(null);
        if (student == null || exam == null) { Res.redirect(ex, "/exams"); return; }
        String body = "<div class=\"card\" style=\"max-width:640px\"><h2>" + Html.esc(Db.SCHOOL_NAME) + "</h2>" +
                "<p class=\"small\">" + Html.esc(Db.SCHOOL_ADDRESS) + " \u00B7 " + Html.esc(Db.SCHOOL_AFFIL) + "</p><div class=\"divider\"></div>" +
                "<h3>Report Card \u2014 " + Html.esc(exam.get("title")) + "</h3>" +
                "<p><b>Student:</b> " + Html.esc(student.get("name")) + " &nbsp; <b>Class:</b> " + Html.esc(student.get("className")) + "-" + Html.esc(student.get("section")) +
                " &nbsp; <b>Admission No:</b> " + Html.esc(student.get("admissionNo")) + "</p>" +
                reportCardHtml(examId, studentId) + "</div>";
        Res.html(ex, 200, Html.printPage("Report Card", body));
    }

    private static void saveExam(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String title = req.p("title").trim(), className = req.p("className").trim();
        StringBuilder subjects = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            String name = req.p("subjName" + i).trim();
            if (name.isBlank()) continue;
            int max = Util.inum(req.p("subjMax" + i, "100"));
            if (subjects.length() > 0) subjects.append(";");
            subjects.append(name).append(":").append(max);
        }
        if (!title.isBlank() && !className.isBlank() && subjects.length() > 0) {
            var row = db.t("exams").newRow();
            row.put("id", db.newId("ex")); row.put("title", title); row.put("className", className); row.put("subjects", subjects.toString());
            db.t("exams").add(row);
        }
        Res.redirect(ex, "/exams");
    }

    private static void examMarksGrid(Req req, HttpExchange ex) throws IOException {
        Util.requireStaff(req.session);
        Db db = Main.db;
        String examId = req.p("examId");
        var exam = db.t("exams").find(examId).orElse(null);
        if (exam == null) { Res.redirect(ex, "/exams"); return; }
        var roster = db.t("students").filter(s -> s.get("className").equals(exam.get("className")));
        List<String> subjNames = new ArrayList<>();
        List<Integer> subjMaxes = new ArrayList<>();
        for (String part : exam.get("subjects").split(";")) { String[] kv = part.split(":"); if (kv.length == 2) { subjNames.add(kv[0]); subjMaxes.add(Util.inum(kv[1])); } }

        StringBuilder c = new StringBuilder("<div class=\"card\"><h2>Marks Entry \u2014 ").append(Html.esc(exam.get("title"))).append(" (Class ").append(Html.esc(exam.get("className"))).append(")</h2>");
        c.append("<form method=\"post\" action=\"/exams/marks/save\">").append(Html.hidden("examId", examId));
        c.append("<div class=\"table-wrap\"><table><thead><tr><th>Student</th>");
        for (int i = 0; i < subjNames.size(); i++) c.append("<th>").append(Html.esc(subjNames.get(i))).append(" (/").append(subjMaxes.get(i)).append(")</th>");
        c.append("</tr></thead><tbody>");
        for (var st : roster) {
            var existing = db.t("exam_marks").filter(m -> m.get("examId").equals(examId) && m.get("studentId").equals(st.get("id")));
            Map<String, Integer> mk = new LinkedHashMap<>();
            if (!existing.isEmpty()) for (String part : existing.get(0).get("marks").split(";")) { String[] kv = part.split(":"); if (kv.length == 2) mk.put(kv[0], Util.inum(kv[1])); }
            c.append("<tr>").append(Html.hidden("studentIds", st.get("id"))).append("<td>").append(Html.esc(st.get("name"))).append("</td>");
            for (String subj : subjNames) c.append("<td><input type=\"number\" name=\"marks_").append(st.get("id")).append("_").append(subj.replaceAll("[^a-zA-Z0-9]", "")).append("\" value=\"").append(mk.getOrDefault(subj, 0)).append("\" min=\"0\"/></td>");
            c.append("</tr>");
        }
        c.append("</tbody></table></div>");
        // Keep a hidden list of subject names so we can reconstruct on save
        c.append(Html.hidden("subjNames", String.join(",", subjNames)));
        c.append("<button class=\"btn btn-primary\" style=\"margin-top:14px\" type=\"submit\">Save Marks</button></form></div>");
        Res.html(ex, 200, Html.page("exams", req.session, c.toString()));
    }

    private static void saveExamMarks(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String examId = req.p("examId");
        List<String> studentIds = req.rawMulti.getOrDefault("studentIds", java.util.Collections.emptyList());
        List<String> subjNames = Arrays.asList(req.p("subjNames").split(","));
        for (String sid : studentIds) {
            StringBuilder marks = new StringBuilder();
            for (String subj : subjNames) {
                if (subj.isBlank()) continue;
                String field = "marks_" + sid + "_" + subj.replaceAll("[^a-zA-Z0-9]", "");
                int val = Util.inum(req.p(field, "0"));
                if (marks.length() > 0) marks.append(";");
                marks.append(subj).append(":").append(val);
            }
            var existing = db.t("exam_marks").filter(m -> m.get("examId").equals(examId) && m.get("studentId").equals(sid));
            if (!existing.isEmpty()) existing.get(0).put("marks", marks.toString());
            else {
                var row = db.t("exam_marks").newRow();
                row.put("id", db.newId("mk")); row.put("examId", examId); row.put("studentId", sid); row.put("marks", marks.toString());
                db.t("exam_marks").rows.add(row);
            }
        }
        db.t("exam_marks").save();
        Res.redirect(ex, "/exams");
    }
}
