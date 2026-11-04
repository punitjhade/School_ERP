package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.*;

public class Services {

    public static boolean dispatch(Req req, HttpExchange ex, String path) throws IOException {
        Session s = req.session;
        switch (path) {
            case "/leave": leave(req, ex); return true;
            case "/leave/apply": applyLeave(req, ex); return true;
            case "/leave/status": Util.requireStaff(s); setLeaveStatus(req, ex); return true;

            case "/notices": notices(req, ex); return true;
            case "/notices/save": Util.requireStaff(s); postNotice(req, ex); return true;

            case "/certificates": certificates(req, ex); return true;
            case "/certificates/request": requestCertificate(req, ex); return true;
            case "/certificates/issue": Util.requireAdmin(s); issueCertificate(req, ex); return true;
            case "/certificates/print": printCertificate(req, ex); return true;

            case "/library": library(req, ex); return true;
            case "/library/save": Util.requireStaff(s); saveBook(req, ex); return true;
            case "/library/delete": Util.requireStaff(s); deleteBook(req, ex); return true;
            case "/library/issue": Util.requireStaff(s); issueBook(req, ex); return true;
            case "/library/return": Util.requireStaff(s); returnBook(req, ex); return true;

            case "/gallery": gallery(req, ex); return true;
            case "/gallery/save": Util.requireStaff(s); saveGalleryItem(req, ex); return true;
            case "/gallery/delete": Util.requireStaff(s); deleteGalleryItem(req, ex); return true;

            case "/events": events(req, ex); return true;
            case "/events/register": registerEvent(req, ex); return true;

            case "/reminders": reminders(req, ex); return true;
            case "/reminders/save": saveReminder(req, ex); return true;
            case "/reminders/delete": deleteReminder(req, ex); return true;

            case "/tickets": tickets(req, ex); return true;
            case "/tickets/raise": raiseTicket(req, ex); return true;
            case "/tickets/resolve": Util.requireAdmin(s); resolveTicket(req, ex); return true;

            case "/feedback": feedback(req, ex); return true;
            case "/feedback/submit": submitFeedback(req, ex); return true;
            case "/feedback/complete": Util.requireAdmin(s); completeFeedback(req, ex); return true;

            case "/hostel": hostel(req, ex); return true;
            case "/hostel/save": Util.requireAdmin(s); saveHostel(req, ex); return true;

            case "/idcards": idCards(req, ex); return true;

            default: return false;
        }
    }

    // =========================== LEAVE ===========================
    private static void leave(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean staff = s.role.equals("admin") || s.role.equals("teacher");
        var rows = staff ? db.t("leave_apps").all() : db.t("leave_apps").filter(l -> l.get("appliedBy").equals(s.userId));

        StringBuilder c = new StringBuilder("<div class=\"card\"><h2>Apply for Leave</h2><form method=\"post\" action=\"/leave/apply\">")
                .append("<div class=\"form-grid\">").append(Html.textField("From", "fromDate", "", "date")).append(Html.textField("To", "toDate", "", "date")).append("</div>")
                .append(Html.textareaField("Reason", "reason", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Submit Application</button></form></div>");

        c.append("<div class=\"card\"><h2>").append(staff ? "All Applications" : "My Applications").append("</h2>")
         .append("<div class=\"table-wrap\"><table><thead><tr><th>Applicant</th><th>Role</th><th>From</th><th>To</th><th>Reason</th><th>Status</th>")
         .append(staff ? "<th>Action</th>" : "").append("</tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"7\">No applications.</td></tr>");
        for (var r : rows) {
            c.append("<tr><td>").append(Html.esc(r.get("appliedByName"))).append("</td><td>").append(Html.esc(r.get("role")))
             .append("</td><td>").append(Html.fmtDate(r.get("fromDate"))).append("</td><td>").append(Html.fmtDate(r.get("toDate")))
             .append("</td><td>").append(Html.esc(r.get("reason"))).append("</td><td>").append(Html.pill(r.get("status"))).append("</td>");
            if (staff) {
                c.append("<td class=\"btn-row\">");
                if (r.get("status").equals("Pending")) {
                    c.append("<form method=\"post\" action=\"/leave/status\" style=\"display:inline\">").append(Html.hidden("id", r.get("id"))).append(Html.hidden("status", "Approved"))
                     .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Approve</button></form>")
                     .append("<form method=\"post\" action=\"/leave/status\" style=\"display:inline\">").append(Html.hidden("id", r.get("id"))).append(Html.hidden("status", "Rejected"))
                     .append("<button class=\"btn btn-sm btn-danger\" type=\"submit\">Reject</button></form>");
                } else c.append("\u2014");
                c.append("</td>");
            }
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("leave", s, c.toString()));
    }

    private static void applyLeave(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        Session s = req.session;
        String from = req.p("fromDate"), to = req.p("toDate"), reason = req.p("reason").trim();
        if (!from.isBlank() && !to.isBlank() && !reason.isBlank()) {
            var row = db.t("leave_apps").newRow();
            row.put("id", db.newId("lv")); row.put("appliedBy", s.userId); row.put("appliedByName", s.name); row.put("role", s.role);
            row.put("fromDate", from); row.put("toDate", to); row.put("reason", reason); row.put("status", "Pending");
            db.t("leave_apps").add(row);
        }
        Res.redirect(ex, "/leave");
    }

    private static void setLeaveStatus(Req req, HttpExchange ex) throws IOException {
        var l = Main.db.t("leave_apps").find(req.p("id")).orElse(null);
        if (l != null) { l.put("status", req.p("status")); Main.db.t("leave_apps").save(); }
        Res.redirect(ex, "/leave");
    }

    // =========================== NOTICES ===========================
    private static void notices(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean canPost = s.role.equals("admin") || s.role.equals("teacher");
        var sorted = new ArrayList<>(db.t("notices").all());
        sorted.sort((a, b) -> b.get("date").compareTo(a.get("date")));

        StringBuilder c = new StringBuilder();
        if (canPost) {
            c.append("<div class=\"card\"><h2>Post a Notice</h2><form method=\"post\" action=\"/notices/save\"><div class=\"form-grid\">")
             .append(Html.textField("Title", "title", "", "text"))
             .append(Html.selectField("Category", "category", new String[]{"Academic", "Event", "Holiday", "General"}, "General"))
             .append("</div>").append(Html.textareaField("Body", "body", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Post Notice</button></form></div>");
        }
        c.append("<div class=\"grid grid-3\">");
        if (sorted.isEmpty()) c.append("<p class=\"small\">No notices yet.</p>");
        for (var n : sorted) {
            c.append("<div class=\"card card-accent-info\"><div class=\"section-head\"><h3 style=\"margin:0\">").append(Html.esc(n.get("title")))
             .append("</h3>").append(Html.pill(n.get("category"))).append("</div><p class=\"small\">").append(Html.fmtDate(n.get("date")))
             .append("</p><p>").append(Html.esc(n.get("body"))).append("</p></div>");
        }
        c.append("</div>");
        Res.html(ex, 200, Html.page("notices", s, c.toString()));
    }

    private static void postNotice(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String title = req.p("title").trim(), body = req.p("body").trim();
        if (!title.isBlank() && !body.isBlank()) {
            var row = db.t("notices").newRow();
            row.put("id", db.newId("nt")); row.put("title", title); row.put("category", req.p("category")); row.put("date", Db.today()); row.put("body", body);
            db.t("notices").add(row);
        }
        Res.redirect(ex, "/notices");
    }

    // =========================== CERTIFICATES ===========================
    private static void certificates(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean isAdmin = s.role.equals("admin");
        var rows = isAdmin ? db.t("certificates").all() : db.t("certificates").filter(r -> r.get("requestedBy").equals(s.userId));

        StringBuilder c = new StringBuilder();
        if (!isAdmin) {
            c.append("<div class=\"card\"><h2>Request a Certificate</h2><form method=\"post\" action=\"/certificates/request\"><div class=\"form-grid\">")
             .append(Html.selectField("Type", "type", new String[]{"Bonafide", "Transfer Certificate", "Character", "Sports"}, "Bonafide"))
             .append(Html.textField("Reason", "reason", "", "text")).append("</div><button class=\"btn btn-primary\" type=\"submit\">Submit Request</button></form></div>");
        }
        c.append("<div class=\"card\"><h2>").append(isAdmin ? "All Requests" : "My Requests").append("</h2>")
         .append("<div class=\"table-wrap\"><table><thead><tr><th>Requested By</th><th>Type</th><th>Reason</th><th>Status</th><th>Action</th></tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"5\">No requests.</td></tr>");
        for (var r : rows) {
            c.append("<tr><td>").append(Html.esc(r.get("requestedByName"))).append("</td><td>").append(Html.esc(r.get("type")))
             .append("</td><td>").append(Html.esc(r.get("reason"))).append("</td><td>").append(Html.pill(r.get("status"))).append("</td><td class=\"btn-row\">");
            if (isAdmin && r.get("status").equals("Pending"))
                c.append("<form method=\"post\" action=\"/certificates/issue\" style=\"display:inline\">").append(Html.hidden("id", r.get("id")))
                 .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Mark Issued</button></form>");
            if (r.get("status").equals("Issued"))
                c.append("<a class=\"btn btn-sm btn-outline\" href=\"/certificates/print?id=").append(r.get("id")).append("\" target=\"_blank\">\uD83D\uDDA8 Print</a>");
            c.append("</td></tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("certificates", s, c.toString()));
    }

    private static void requestCertificate(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        Session s = req.session;
        String reason = req.p("reason").trim();
        if (!reason.isBlank()) {
            var row = db.t("certificates").newRow();
            row.put("id", db.newId("cert")); row.put("type", req.p("type")); row.put("reason", reason);
            row.put("requestedBy", s.userId); row.put("requestedByName", s.name); row.put("status", "Pending"); row.put("issueDate", "");
            db.t("certificates").add(row);
        }
        Res.redirect(ex, "/certificates");
    }

    private static void issueCertificate(Req req, HttpExchange ex) throws IOException {
        var r = Main.db.t("certificates").find(req.p("id")).orElse(null);
        if (r != null) { r.put("status", "Issued"); r.put("issueDate", Db.today()); Main.db.t("certificates").save(); }
        Res.redirect(ex, "/certificates");
    }

    private static void printCertificate(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var r = db.t("certificates").find(req.p("id")).orElse(null);
        if (r == null) { Res.redirect(ex, "/certificates"); return; }
        var student = db.t("students").filter(st -> st.get("name").equals(r.get("requestedByName")));
        // Try to resolve via the requester's user record for a more reliable link
        var user = db.t("users").find(r.get("requestedBy")).orElse(null);
        LinkedHashMap<String, String> stRow = null;
        if (user != null && !user.getOrDefault("studentId", "").isBlank()) stRow = db.t("students").find(user.get("studentId")).orElse(null);
        if (stRow == null && !student.isEmpty()) stRow = student.get(0);

        String body = "<div class=\"certificate\"><h1>" + Html.esc(Db.SCHOOL_NAME) + "</h1>" +
                "<p class=\"small\">" + Html.esc(Db.SCHOOL_ADDRESS) + " \u00B7 " + Html.esc(Db.SCHOOL_AFFIL) + "</p>" +
                "<h2>" + Html.esc(r.get("type")) + " Certificate</h2>" +
                "<div class=\"cert-body\">This is to certify that <b>" + Html.esc(stRow != null ? stRow.get("name") : r.get("requestedByName")) + "</b>, " +
                (stRow != null ? "a student of Class " + Html.esc(stRow.get("className")) + "-" + Html.esc(stRow.get("section")) + ", Admission No. " + Html.esc(stRow.get("admissionNo")) + ", " : "") +
                "is a bonafide record holder of this institution. This certificate is issued for the purpose of: <b>" + Html.esc(r.get("reason")) + "</b>." +
                "<br/><br/>Issue Date: " + Html.fmtDate(r.get("issueDate").isBlank() ? Db.today() : r.get("issueDate")) + "</div>" +
                "<div class=\"sign-row\"><div>_____________________<br/>Class Teacher</div><div>_____________________<br/>Principal</div></div></div>";
        Res.html(ex, 200, Html.printPage("Certificate", body));
    }

    // =========================== LIBRARY ===========================
    private static void library(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean canManage = s.role.equals("admin") || s.role.equals("teacher");
        var books = db.t("books").all();

        StringBuilder c = new StringBuilder("<div class=\"section-head\"><h2>Library \u2014 Books</h2></div>");
        if (canManage) {
            c.append("<div class=\"card\"><h3>Add Book</h3><form method=\"post\" action=\"/library/save\"><div class=\"form-grid\">")
             .append(Html.textField("Title", "title", "", "text")).append(Html.textField("Author", "author", "", "text"))
             .append(Html.textField("ISBN", "isbn", "", "text")).append(Html.textField("Total Copies", "copiesTotal", "1", "number"))
             .append(Html.textField("Available Copies", "copiesAvailable", "1", "number"))
             .append("</div><button class=\"btn btn-primary\" type=\"submit\">Save Book</button></form></div>");
        }
        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Title</th><th>Author</th><th>ISBN</th><th>Total</th><th>Available</th>")
         .append(canManage ? "<th>Actions</th>" : "").append("</tr></thead><tbody>");
        for (var b : books) {
            c.append("<tr><td>").append(Html.esc(b.get("title"))).append("</td><td>").append(Html.esc(b.get("author")))
             .append("</td><td>").append(Html.esc(b.get("isbn"))).append("</td><td>").append(Html.esc(b.get("copiesTotal")))
             .append("</td><td>").append(Html.esc(b.get("copiesAvailable"))).append("</td>");
            if (canManage) c.append("<td><form method=\"post\" action=\"/library/delete\">").append(Html.hidden("id", b.get("id")))
                    .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td>");
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");

        if (canManage) {
            c.append("<div class=\"card\"><h2>Issue a Book</h2><form method=\"post\" action=\"/library/issue\"><div class=\"form-grid\">");
            c.append("<div class=\"field\"><label>Book</label><select name=\"bookId\">");
            for (var b : books) if (Util.inum(b.get("copiesAvailable")) > 0) c.append("<option value=\"").append(b.get("id")).append("\">").append(Html.esc(b.get("title"))).append(" (").append(b.get("copiesAvailable")).append(" available)</option>");
            c.append("</select></div><div class=\"field\"><label>Student</label><select name=\"studentId\">");
            for (var st : db.t("students").all()) c.append("<option value=\"").append(st.get("id")).append("\">").append(Html.esc(st.get("name"))).append("</option>");
            c.append("</select></div>").append(Html.textField("Due Date", "dueDate", Db.todayPlus(14), "date"));
            c.append("</div><button class=\"btn btn-primary\" type=\"submit\">Issue Book</button></form></div>");
        }

        c.append("<div class=\"card\"><h2>Issue Records</h2><div class=\"table-wrap\"><table><thead><tr><th>Book</th><th>Student</th><th>Issue Date</th><th>Due Date</th><th>Status</th>")
         .append(canManage ? "<th>Action</th>" : "").append("</tr></thead><tbody>");
        var issues = db.t("library_issues").all();
        if (issues.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"6\">No issue records.</td></tr>");
        for (var li : issues) {
            c.append("<tr><td>").append(Html.esc(li.get("bookTitle"))).append("</td><td>").append(Html.esc(li.get("studentName")))
             .append("</td><td>").append(Html.fmtDate(li.get("issueDate"))).append("</td><td>").append(Html.fmtDate(li.get("dueDate")))
             .append("</td><td>").append(Html.pill(li.get("status"))).append("</td>");
            if (canManage) {
                c.append("<td>");
                if (li.get("status").equals("Issued")) c.append("<form method=\"post\" action=\"/library/return\">").append(Html.hidden("id", li.get("id")))
                        .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Mark Returned</button></form>");
                else c.append("\u2014");
                c.append("</td>");
            }
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("library", s, c.toString()));
    }

    private static void saveBook(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("books").newRow();
        row.put("id", db.newId("bk")); row.put("title", req.p("title")); row.put("author", req.p("author")); row.put("isbn", req.p("isbn"));
        row.put("copiesTotal", req.p("copiesTotal")); row.put("copiesAvailable", req.p("copiesAvailable"));
        db.t("books").add(row);
        Res.redirect(ex, "/library");
    }
    private static void deleteBook(Req req, HttpExchange ex) throws IOException { Main.db.t("books").deleteById(req.p("id")); Res.redirect(ex, "/library"); }

    private static void issueBook(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var book = db.t("books").find(req.p("bookId")).orElse(null);
        var student = db.t("students").find(req.p("studentId")).orElse(null);
        String dueDate = req.p("dueDate");
        if (book != null && student != null && Util.inum(book.get("copiesAvailable")) > 0 && !dueDate.isBlank()) {
            book.put("copiesAvailable", String.valueOf(Util.inum(book.get("copiesAvailable")) - 1));
            var row = db.t("library_issues").newRow();
            row.put("id", db.newId("li")); row.put("bookId", book.get("id")); row.put("bookTitle", book.get("title"));
            row.put("studentId", student.get("id")); row.put("studentName", student.get("name"));
            row.put("issueDate", Db.today()); row.put("dueDate", dueDate); row.put("status", "Issued"); row.put("returnDate", "");
            db.t("library_issues").rows.add(row);
            db.t("books").save();
            db.t("library_issues").save();
        }
        Res.redirect(ex, "/library");
    }

    private static void returnBook(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var li = db.t("library_issues").find(req.p("id")).orElse(null);
        if (li != null && li.get("status").equals("Issued")) {
            li.put("status", "Returned"); li.put("returnDate", Db.today());
            var book = db.t("books").find(li.get("bookId")).orElse(null);
            if (book != null) {
                int total = Util.inum(book.get("copiesTotal"));
                int avail = Util.inum(book.get("copiesAvailable")) + 1;
                book.put("copiesAvailable", String.valueOf(Math.min(total, avail)));
                db.t("books").save();
            }
            db.t("library_issues").save();
        }
        Res.redirect(ex, "/library");
    }

    // =========================== GALLERY ===========================
    // Image "upload" is by URL reference (no multipart binary upload) to keep the app dependency-free.
    private static void gallery(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean canUpload = s.role.equals("admin") || s.role.equals("teacher");
        StringBuilder c = new StringBuilder();
        if (canUpload) {
            c.append("<div class=\"card\"><h2>Add Photo</h2><form method=\"post\" action=\"/gallery/save\"><div class=\"form-grid\">")
             .append(Html.textField("Caption", "caption", "", "text")).append(Html.textField("Album", "album", "", "text"))
             .append("</div>").append(Html.textField("Image URL", "imageUrl", "", "text"))
             .append("<p class=\"hint\">Paste a link to an image hosted elsewhere (this app has no binary file upload, to stay dependency-free).</p>")
             .append("<button class=\"btn btn-primary\" type=\"submit\">Add to Gallery</button></form></div>");
        }
        c.append("<div class=\"card\"><h2>Gallery</h2><div class=\"gallery-grid\">");
        var items = new ArrayList<>(db.t("gallery").all());
        Collections.reverse(items);
        if (items.isEmpty()) c.append("<p class=\"small\">No photos yet.</p>");
        for (var g : items) c.append("<div class=\"gallery-item\"><img src=\"").append(Html.esc(g.get("imageUrl"))).append("\" alt=\"").append(Html.esc(g.get("caption")))
                .append("\"/><div class=\"cap\"><b>").append(Html.esc(g.get("caption"))).append("</b><br/>").append(Html.esc(g.get("album"))).append("</div></div>");
        c.append("</div></div>");
        Res.html(ex, 200, Html.page("gallery", s, c.toString()));
    }
    private static void saveGalleryItem(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String url = req.p("imageUrl").trim();
        if (!url.isBlank()) {
            var row = db.t("gallery").newRow();
            row.put("id", db.newId("gal")); row.put("caption", req.p("caption", "Untitled")); row.put("album", req.p("album", "General")); row.put("imageUrl", url);
            db.t("gallery").add(row);
        }
        Res.redirect(ex, "/gallery");
    }
    private static void deleteGalleryItem(Req req, HttpExchange ex) throws IOException { Main.db.t("gallery").deleteById(req.p("id")); Res.redirect(ex, "/gallery"); }

    // =========================== EVENTS ===========================
    private static void events(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        StringBuilder c = new StringBuilder("<div class=\"grid grid-3\">");
        var evs = db.t("events").all();
        if (evs.isEmpty()) c.append("<p class=\"small\">No upcoming events.</p>");
        for (var e : evs) {
            boolean registered = !db.t("event_regs").filter(r -> r.get("eventId").equals(e.get("id")) && r.get("userId").equals(s.userId)).isEmpty();
            c.append("<div class=\"card card-accent-teal\"><h3>").append(Html.esc(e.get("title"))).append("</h3><p class=\"small\">")
             .append(Html.esc(e.get("club"))).append(" \u00B7 ").append(Html.fmtDate(e.get("date"))).append(" \u00B7 ").append(Html.esc(e.get("venue"))).append("</p>");
            if (registered) c.append("<span class=\"pill pill-success\">Registered \u2713</span>");
            else c.append("<form method=\"post\" action=\"/events/register\">").append(Html.hidden("eventId", e.get("id")))
                    .append("<button class=\"btn btn-accent btn-sm\" type=\"submit\">Register</button></form>");
            c.append("</div>");
        }
        c.append("</div>");
        Res.html(ex, 200, Html.page("events", s, c.toString()));
    }
    private static void registerEvent(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("event_regs").newRow();
        row.put("id", db.newId("reg")); row.put("eventId", req.p("eventId")); row.put("userId", req.session.userId);
        db.t("event_regs").add(row);
        Res.redirect(ex, "/events");
    }

    // =========================== REMINDERS (private) ===========================
    private static void reminders(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        var rows = Main.db.t("reminders").filter(r -> r.get("ownerId").equals(s.userId));
        StringBuilder c = new StringBuilder("<div class=\"card\"><h3>Add Reminder</h3><form method=\"post\" action=\"/reminders/save\"><div class=\"form-grid\">")
                .append(Html.textField("Title", "title", "", "text")).append(Html.textField("Date", "date", "", "date")).append("</div>")
                .append(Html.textareaField("Note", "note", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");
        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Title</th><th>Date</th><th>Note</th><th>Actions</th></tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"4\">No reminders yet.</td></tr>");
        for (var r : rows) c.append("<tr><td>").append(Html.esc(r.get("title"))).append("</td><td>").append(Html.fmtDate(r.get("date")))
                .append("</td><td>").append(Html.esc(r.get("note"))).append("</td><td><form method=\"post\" action=\"/reminders/delete\">")
                .append(Html.hidden("id", r.get("id"))).append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td></tr>");
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("reminders", s, c.toString()));
    }
    private static void saveReminder(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("reminders").newRow();
        row.put("id", db.newId("rem")); row.put("ownerId", req.session.userId); row.put("title", req.p("title")); row.put("date", req.p("date")); row.put("note", req.p("note"));
        db.t("reminders").add(row);
        Res.redirect(ex, "/reminders");
    }
    private static void deleteReminder(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var r = db.t("reminders").find(req.p("id")).orElse(null);
        if (r != null && r.get("ownerId").equals(req.session.userId)) db.t("reminders").deleteById(req.p("id"));
        Res.redirect(ex, "/reminders");
    }

    // =========================== TICKETS ===========================
    private static void tickets(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean isAdmin = s.role.equals("admin");
        var rows = isAdmin ? db.t("tickets").all() : db.t("tickets").filter(t -> t.get("raisedBy").equals(s.userId));
        StringBuilder c = new StringBuilder("<div class=\"card\"><h2>Raise a Ticket</h2><form method=\"post\" action=\"/tickets/raise\"><div class=\"form-grid\">")
                .append(Html.textField("Subject", "subject", "", "text"))
                .append(Html.selectField("Category", "category", new String[]{"Technical", "Academic", "Administrative", "Other"}, "Technical"))
                .append("</div>").append(Html.textareaField("Description", "description", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Submit Ticket</button></form></div>");
        c.append("<div class=\"card\"><h2>").append(isAdmin ? "All Tickets" : "My Tickets").append("</h2><div class=\"table-wrap\"><table><thead><tr><th>Subject</th><th>Category</th><th>Raised By</th><th>Status</th>")
         .append(isAdmin ? "<th>Action</th>" : "").append("</tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"5\">No tickets.</td></tr>");
        for (var t : rows) {
            c.append("<tr><td>").append(Html.esc(t.get("subject"))).append("</td><td>").append(Html.esc(t.get("category")))
             .append("</td><td>").append(Html.esc(t.get("raisedByName"))).append("</td><td>").append(Html.pill(t.get("status"))).append("</td>");
            if (isAdmin) {
                c.append("<td>");
                if (t.get("status").equals("Open")) c.append("<form method=\"post\" action=\"/tickets/resolve\">").append(Html.hidden("id", t.get("id")))
                        .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Mark Resolved</button></form>");
                else c.append("\u2014");
                c.append("</td>");
            }
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("tickets", s, c.toString()));
    }
    private static void raiseTicket(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        Session s = req.session;
        String subject = req.p("subject").trim(), description = req.p("description").trim();
        if (!subject.isBlank() && !description.isBlank()) {
            var row = db.t("tickets").newRow();
            row.put("id", db.newId("tk")); row.put("subject", subject); row.put("category", req.p("category")); row.put("description", description);
            row.put("raisedBy", s.userId); row.put("raisedByName", s.name); row.put("status", "Open");
            db.t("tickets").add(row);
        }
        Res.redirect(ex, "/tickets");
    }
    private static void resolveTicket(Req req, HttpExchange ex) throws IOException {
        var t = Main.db.t("tickets").find(req.p("id")).orElse(null);
        if (t != null) { t.put("status", "Resolved"); Main.db.t("tickets").save(); }
        Res.redirect(ex, "/tickets");
    }

    // =========================== FEEDBACK ===========================
    private static void feedback(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean isAdmin = s.role.equals("admin");
        var rows = isAdmin ? db.t("feedback").all() : db.t("feedback").filter(f -> f.get("submittedBy").equals(s.userId));
        StringBuilder c = new StringBuilder("<div class=\"card\"><h2>Submit Feedback</h2><form method=\"post\" action=\"/feedback/submit\">")
                .append(Html.selectField("Category", "category", new String[]{"Academics", "Facilities", "Staff", "General"}, "General"))
                .append(Html.textareaField("Your Feedback", "text", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Submit</button></form></div>");
        c.append("<div class=\"card\"><h2>").append(isAdmin ? "All Feedback" : "My Feedback").append("</h2><div class=\"table-wrap\"><table><thead><tr><th>Category</th><th>Text</th><th>By</th><th>Date</th><th>Status</th>")
         .append(isAdmin ? "<th>Action</th>" : "").append("</tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"6\">No feedback yet.</td></tr>");
        for (var f : rows) {
            c.append("<tr><td>").append(Html.esc(f.get("category"))).append("</td><td>").append(Html.esc(f.get("text")))
             .append("</td><td>").append(Html.esc(f.get("submittedByName"))).append("</td><td>").append(Html.fmtDate(f.get("date")))
             .append("</td><td>").append(Html.pill(f.get("status"))).append("</td>");
            if (isAdmin) {
                c.append("<td>");
                if (f.get("status").equals("Pending")) c.append("<form method=\"post\" action=\"/feedback/complete\">").append(Html.hidden("id", f.get("id")))
                        .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Mark Completed</button></form>");
                else c.append("\u2014");
                c.append("</td>");
            }
            c.append("</tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("feedback", s, c.toString()));
    }
    private static void submitFeedback(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        Session s = req.session;
        String text = req.p("text").trim();
        if (!text.isBlank()) {
            var row = db.t("feedback").newRow();
            row.put("id", db.newId("fb")); row.put("category", req.p("category")); row.put("text", text);
            row.put("submittedBy", s.userId); row.put("submittedByName", s.name); row.put("date", Db.today()); row.put("status", "Pending");
            db.t("feedback").add(row);
        }
        Res.redirect(ex, "/feedback");
    }
    private static void completeFeedback(Req req, HttpExchange ex) throws IOException {
        var f = Main.db.t("feedback").find(req.p("id")).orElse(null);
        if (f != null) { f.put("status", "Completed"); Main.db.t("feedback").save(); }
        Res.redirect(ex, "/feedback");
    }

    // =========================== HOSTEL ===========================
    private static void hostel(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        var h = db.t("hostel").all().isEmpty() ? null : db.t("hostel").all().get(0);
        boolean canEdit = s.role.equals("admin");
        StringBuilder c = new StringBuilder("<div class=\"card\"><h2>Hostel Information</h2>");
        if (h != null) {
            c.append("<div class=\"grid grid-4\">")
             .append(Html.statCard("Block", h.get("block"), "")).append(Html.statCard("Room No.", h.get("room"), ""))
             .append(Html.statCard("Bed Type", h.get("bedType"), "")).append(Html.statCard("Warden", h.get("warden"), ""))
             .append("</div>");
        }
        c.append("<p class=\"small\" style=\"margin-top:12px\">For hostel leave requests, please use the <a href=\"/leave\">Leave Applications</a> module.</p>");
        if (canEdit && h != null) {
            c.append("<div class=\"divider\"></div><h3>Edit Hostel Info</h3><form method=\"post\" action=\"/hostel/save\"><div class=\"form-grid\">")
             .append(Html.hidden("id", h.get("id")))
             .append(Html.textField("Block", "block", h.get("block"), "text")).append(Html.textField("Room", "room", h.get("room"), "text"))
             .append(Html.textField("Warden", "warden", h.get("warden"), "text")).append(Html.textField("Bed Type", "bedType", h.get("bedType"), "text"))
             .append("</div><button class=\"btn btn-primary\" type=\"submit\">Save</button></form>");
        }
        c.append("</div>");
        Res.html(ex, 200, Html.page("hostel", s, c.toString()));
    }
    private static void saveHostel(Req req, HttpExchange ex) throws IOException {
        var h = Main.db.t("hostel").find(req.p("id")).orElse(null);
        if (h != null) {
            h.put("block", req.p("block")); h.put("room", req.p("room")); h.put("warden", req.p("warden")); h.put("bedType", req.p("bedType"));
            Main.db.t("hostel").save();
        }
        Res.redirect(ex, "/hostel");
    }

    // =========================== ID CARDS ===========================
    // No JS/external library is used, so this is a printable card with the identifier printed as text
    // rather than a scannable QR image (see README for a note on this limitation).
    private static void idCards(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        boolean isStaff = s.role.equals("admin") || s.role.equals("teacher");
        String sid = isStaff ? req.p("studentId", db.t("students").all().isEmpty() ? "" : db.t("students").all().get(0).get("id")) : s.studentId;
        var student = db.t("students").find(sid).orElse(null);

        StringBuilder c = new StringBuilder();
        if (isStaff) {
            c.append("<div class=\"card no-print\"><form method=\"get\" style=\"display:flex;gap:8px;align-items:center\"><div class=\"field\" style=\"max-width:320px\"><label>Select Student</label><select name=\"studentId\">");
            for (var st : db.t("students").all()) c.append("<option value=\"").append(st.get("id")).append("\"").append(st.get("id").equals(sid) ? " selected" : "").append(">").append(Html.esc(st.get("name"))).append(" (").append(Html.esc(st.get("admissionNo"))).append(")</option>");
            c.append("</select></div><button class=\"btn btn-outline btn-sm\" type=\"submit\">Load</button></form></div>");
        }
        if (student == null) {
            c.append("<p class=\"small\">No student record linked to this account.</p>");
        } else {
            String idCode = "ID-" + student.get("admissionNo") + "-" + student.get("id");
            c.append("<div class=\"card\"><div class=\"id-card\"><div class=\"head\"><b>").append(Html.esc(Db.SCHOOL_NAME)).append("</b><div class=\"small\">").append(Html.esc(Db.SCHOOL_ADDRESS)).append("</div></div>")
             .append("<div class=\"body-row\"><div class=\"photo-ph\">\uD83E\uDDD1</div><div class=\"info\">")
             .append("<div><b>").append(Html.esc(student.get("name"))).append("</b></div>")
             .append("<div>Class: ").append(Html.esc(student.get("className"))).append("-").append(Html.esc(student.get("section"))).append("</div>")
             .append("<div>Admission No: ").append(Html.esc(student.get("admissionNo"))).append("</div>")
             .append("<div>DOB: ").append(Html.fmtDate(student.get("dob"))).append("</div>")
             .append("<div>House: ").append(student.get("house").isBlank() ? "\u2014" : Html.esc(student.get("house"))).append("</div>")
             .append("</div></div><div class=\"qr-wrap\"><div class=\"small\" style=\"font-family:var(--font-mono);letter-spacing:1px\">").append(Html.esc(idCode)).append("</div></div></div>")
             .append("<p class=\"small no-print\" style=\"margin-top:10px\">\u2139 This build has no scannable barcode/QR image (no external libraries or JavaScript are used) \u2014 the unique ID is printed as text instead.</p></div>");
        }
        Res.html(ex, 200, Html.page("idcards", s, c.toString()));
    }
}

