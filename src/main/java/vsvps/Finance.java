package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.util.*;

public class Finance {

    public static boolean dispatch(Req req, HttpExchange ex, String path) throws IOException {
        Session s = req.session;
        switch (path) {
            case "/fees": fees(req, ex); return true;
            case "/fees/bulkassign": Util.requireFinance(s); bulkAssignFee(req, ex); return true;
            case "/fees/markpaid": Util.requireFinance(s); markPaidCash(req, ex); return true;
            case "/fees/pay": demoPay(req, ex); return true;
            case "/fees/receipt": printReceipt(req, ex); return true;

            case "/ledger": Util.requireFinance(s); ledger(req, ex); return true;
            case "/ledger/save": Util.requireFinance(s); saveLedger(req, ex); return true;
            case "/ledger/delete": Util.requireFinance(s); deleteLedger(req, ex); return true;

            default: return false;
        }
    }

    // =========================== FEES ===========================
    private static void fees(Req req, HttpExchange ex) throws IOException {
        Session s = req.session;
        Db db = Main.db;
        StringBuilder c = new StringBuilder();

        if (s.role.equals("admin") || s.role.equals("accountant")) {
            c.append("<div class=\"card\"><div class=\"section-head\"><h2>\uD83D\uDCB0 Bulk-Assign Fee to a Class</h2></div>")
             .append("<form method=\"post\" action=\"/fees/bulkassign\"><div class=\"form-grid\">")
             .append(Html.textField("Class", "className", "", "text"))
             .append(Html.textField("Fee Head", "head", "", "text"))
             .append(Html.textField("Term", "term", "", "text"))
             .append(Html.textField("Amount", "amount", "", "number"))
             .append(Html.textField("Due Date", "dueDate", "", "date"))
             .append("</div><button class=\"btn btn-primary\" type=\"submit\">Assign to Class</button></form></div>");

            String filter = req.p("filter", "All");
            var allFees = db.t("fees").all();
            double totalDue = allFees.stream().filter(f -> f.get("status").equals("Due")).mapToDouble(f -> Util.num(f.get("amount"))).sum();
            double totalPaid = allFees.stream().filter(f -> f.get("status").equals("Paid")).mapToDouble(f -> Util.num(f.get("amount"))).sum();
            long defaulterCount = allFees.stream().filter(f -> f.get("status").equals("Due")).count();

            c.append("<div class=\"card\"><div class=\"section-head\"><h2>\uD83D\uDCB0 Fee Records</h2></div>");
            c.append("<form method=\"get\" style=\"display:flex;gap:8px;align-items:center;margin-bottom:12px\"><select name=\"filter\">")
             .append(opt("All", filter)).append(opt("Due", filter, "Defaulters")).append(opt("Paid", filter)).append(opt("Today", filter, "Day Book (Paid Today)"))
             .append("</select><button class=\"btn btn-outline btn-sm\" type=\"submit\">Filter</button></form>");
            c.append("<div class=\"grid grid-3\">")
             .append(Html.statCard("Total Due", Html.money(String.valueOf(totalDue)), "card-accent-danger"))
             .append(Html.statCard("Total Collected", Html.money(String.valueOf(totalPaid)), ""))
             .append(Html.statCard("Defaulter Count", String.valueOf(defaulterCount), "card-accent-danger"))
             .append("</div>");

            List<LinkedHashMap<String, String>> rows;
            String today = Db.today();
            switch (filter) {
                case "Due" -> rows = db.t("fees").filter(f -> f.get("status").equals("Due"));
                case "Paid" -> rows = db.t("fees").filter(f -> f.get("status").equals("Paid"));
                case "Today" -> rows = db.t("fees").filter(f -> f.get("status").equals("Paid") && f.get("paidDate").equals(today));
                default -> rows = allFees;
            }
            c.append("<div class=\"table-wrap\"><table><thead><tr><th>Student</th><th>Class</th><th>Head</th><th>Term</th><th>Amount</th><th>Due Date</th><th>Status</th><th>Action</th></tr></thead><tbody>");
            if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"8\">No records for this filter.</td></tr>");
            for (var f : rows) {
                c.append("<tr><td>").append(Html.esc(f.get("studentName"))).append("</td><td>").append(Html.esc(f.get("className")))
                 .append("</td><td>").append(Html.esc(f.get("head"))).append("</td><td>").append(Html.esc(f.get("term")))
                 .append("</td><td>").append(Html.money(f.get("amount"))).append("</td><td>").append(Html.fmtDate(f.get("dueDate")))
                 .append("</td><td>").append(Html.pill(f.get("status"))).append("</td><td>");
                if (f.get("status").equals("Due")) c.append("<form method=\"post\" action=\"/fees/markpaid\">").append(Html.hidden("id", f.get("id")))
                        .append("<button class=\"btn btn-sm btn-accent\" type=\"submit\">Mark Paid (Cash)</button></form>");
                else c.append("\u2014");
                c.append("</td></tr>");
            }
            c.append("</tbody></table></div></div>");
        } else {
            String sid = s.primaryStudentId();
            var mine = db.t("fees").filter(f -> f.get("studentId").equals(sid));
            var due = mine.stream().filter(f -> f.get("status").equals("Due")).toList();
            var paid = mine.stream().filter(f -> f.get("status").equals("Paid")).toList();

            c.append("<div class=\"card\"><h2>\uD83D\uDCB0 Dues</h2><div class=\"table-wrap\"><table><thead><tr><th>Head</th><th>Term</th><th>Amount</th><th>Due Date</th><th>Action</th></tr></thead><tbody>");
            if (due.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"5\">No dues \uD83C\uDF89</td></tr>");
            for (var f : due) c.append("<tr><td>").append(Html.esc(f.get("head"))).append("</td><td>").append(Html.esc(f.get("term")))
                    .append("</td><td>").append(Html.money(f.get("amount"))).append("</td><td>").append(Html.fmtDate(f.get("dueDate")))
                    .append("</td><td><form method=\"post\" action=\"/fees/pay\">").append(Html.hidden("id", f.get("id")))
                    .append("<button class=\"btn btn-accent btn-sm\" type=\"submit\">Pay Now (Demo)</button></form></td></tr>");
            c.append("</tbody></table></div></div>");

            c.append("<div class=\"card\"><h2>\uD83E\uDDFE Payment History</h2><div class=\"table-wrap\"><table><thead><tr><th>Receipt No.</th><th>Head</th><th>Amount</th><th>Paid Date</th><th></th></tr></thead><tbody>");
            if (paid.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"5\">No payments yet.</td></tr>");
            for (var f : paid) c.append("<tr><td>RCPT-").append(f.get("id").substring(Math.max(0, f.get("id").length() - 6)).toUpperCase()).append("</td><td>")
                    .append(Html.esc(f.get("head"))).append("</td><td>").append(Html.money(f.get("amount"))).append("</td><td>").append(Html.fmtDate(f.get("paidDate")))
                    .append("</td><td><a class=\"btn btn-outline btn-sm\" href=\"/fees/receipt?id=").append(f.get("id")).append("\" target=\"_blank\">\uD83D\uDDA8 Receipt</a></td></tr>");
            c.append("</tbody></table></div></div>");
        }
        Res.html(ex, 200, Html.page("fees", s, c.toString()));
    }

    private static String opt(String value, String current) { return opt(value, current, value); }
    private static String opt(String value, String current, String label) {
        return "<option value=\"" + value + "\"" + (value.equals(current) ? " selected" : "") + ">" + label + "</option>";
    }

    private static void bulkAssignFee(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        String cls = req.p("className").trim(), head = req.p("head").trim(), term = req.p("term").trim(), dueDate = req.p("dueDate");
        double amount = Util.num(req.p("amount"));
        if (!cls.isBlank() && !head.isBlank() && amount > 0 && !dueDate.isBlank()) {
            var roster = db.t("students").filter(st -> st.get("className").equals(cls));
            for (var st : roster) {
                var row = db.t("fees").newRow();
                row.put("id", db.newId("fee")); row.put("studentId", st.get("id")); row.put("studentName", st.get("name"));
                row.put("className", cls); row.put("head", head); row.put("term", term); row.put("amount", String.valueOf(amount));
                row.put("dueDate", dueDate); row.put("status", "Due"); row.put("paidDate", "");
                db.t("fees").rows.add(row);
            }
            db.t("fees").save();
        }
        Res.redirect(ex, "/fees");
    }

    private static void markPaidCash(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var f = db.t("fees").find(req.p("id")).orElse(null);
        if (f != null) {
            f.put("status", "Paid"); f.put("paidDate", Db.today());
            db.t("fees").save();
            var row = db.t("ledger").newRow();
            row.put("id", db.newId("led")); row.put("type", "Income"); row.put("category", f.get("head")); row.put("amount", f.get("amount"));
            row.put("date", Db.today()); row.put("note", f.get("studentName") + " - " + f.get("term") + " (cash)");
            db.t("ledger").add(row);
        }
        Res.redirect(ex, "/fees");
    }

    private static void demoPay(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var f = db.t("fees").find(req.p("id")).orElse(null);
        if (f != null && f.get("studentId").equals(req.session.primaryStudentId())) {
            f.put("status", "Paid"); f.put("paidDate", Db.today());
            db.t("fees").save();
            var row = db.t("ledger").newRow();
            row.put("id", db.newId("led")); row.put("type", "Income"); row.put("category", f.get("head")); row.put("amount", f.get("amount"));
            row.put("date", Db.today()); row.put("note", f.get("studentName") + " - " + f.get("term") + " (online demo)");
            db.t("ledger").add(row);
        }
        Res.redirect(ex, "/fees");
    }

    private static void printReceipt(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var f = db.t("fees").find(req.p("id")).orElse(null);
        if (f == null) { Res.redirect(ex, "/fees"); return; }
        var student = db.t("students").find(f.get("studentId")).orElse(null);
        String body = "<div class=\"card\" style=\"max-width:480px\"><h2>" + Html.esc(Db.SCHOOL_NAME) + "</h2>" +
                "<p class=\"small\">" + Html.esc(Db.SCHOOL_ADDRESS) + " \u00B7 " + Html.esc(Db.SCHOOL_AFFIL) + "</p><div class=\"divider\"></div>" +
                "<p><b>Receipt No:</b> RCPT-" + f.get("id").substring(Math.max(0, f.get("id").length() - 6)).toUpperCase() + "<br/>" +
                "<b>Student:</b> " + Html.esc(f.get("studentName")) + " (" + Html.esc(student != null ? student.get("admissionNo") : "") + ")<br/>" +
                "<b>Class:</b> " + Html.esc(f.get("className")) + "<br/>" +
                "<b>Fee Head:</b> " + Html.esc(f.get("head")) + " \u2014 " + Html.esc(f.get("term")) + "<br/>" +
                "<b>Amount Paid:</b> " + Html.money(f.get("amount")) + "<br/>" +
                "<b>Date:</b> " + Html.fmtDate(f.get("paidDate")) + "</p></div>";
        Res.html(ex, 200, Html.printPage("Fee Receipt", body));
    }

    // =========================== LEDGER ===========================
    private static void ledger(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var rows = db.t("ledger").all();
        double income = rows.stream().filter(l -> l.get("type").equals("Income")).mapToDouble(l -> Util.num(l.get("amount"))).sum();
        double expense = rows.stream().filter(l -> l.get("type").equals("Expense")).mapToDouble(l -> Util.num(l.get("amount"))).sum();

        StringBuilder c = new StringBuilder("<div class=\"grid grid-3\">")
                .append(Html.statCard("Total Income", Html.money(String.valueOf(income)), "card-accent-teal"))
                .append(Html.statCard("Total Expense", Html.money(String.valueOf(expense)), "card-accent-danger"))
                .append(Html.statCard("Net Balance", Html.money(String.valueOf(income - expense)), "card-accent-top"))
                .append("</div>");
        c.append("<div class=\"card\"><h3>Add Ledger Entry</h3><form method=\"post\" action=\"/ledger/save\"><div class=\"form-grid\">")
         .append(Html.selectField("Type", "type", new String[]{"Income", "Expense"}, "Income"))
         .append(Html.textField("Category", "category", "", "text"))
         .append(Html.textField("Amount", "amount", "", "number"))
         .append(Html.textField("Date", "date", Db.today(), "date"))
         .append("</div>").append(Html.textareaField("Note", "note", "")).append("<button class=\"btn btn-primary\" type=\"submit\">Save</button></form></div>");

        c.append("<div class=\"card\" style=\"padding:0\"><div class=\"table-wrap\"><table><thead><tr><th>Type</th><th>Category</th><th>Amount</th><th>Date</th><th>Note</th><th>Actions</th></tr></thead><tbody>");
        if (rows.isEmpty()) c.append("<tr class=\"empty-row\"><td colspan=\"6\">No entries yet.</td></tr>");
        for (var r : rows) {
            c.append("<tr><td><span class=\"pill ").append(r.get("type").equals("Income") ? "pill-success" : "pill-danger").append("\">").append(Html.esc(r.get("type"))).append("</span></td>")
             .append("<td>").append(Html.esc(r.get("category"))).append("</td><td>").append(Html.money(r.get("amount")))
             .append("</td><td>").append(Html.fmtDate(r.get("date"))).append("</td><td>").append(Html.esc(r.get("note")))
             .append("</td><td><form method=\"post\" action=\"/ledger/delete\">").append(Html.hidden("id", r.get("id")))
             .append("<button class=\"btn btn-danger btn-sm\" type=\"submit\">Delete</button></form></td></tr>");
        }
        c.append("</tbody></table></div></div>");
        Res.html(ex, 200, Html.page("ledger", req.session, c.toString()));
    }

    private static void saveLedger(Req req, HttpExchange ex) throws IOException {
        Db db = Main.db;
        var row = db.t("ledger").newRow();
        row.put("id", db.newId("led")); row.put("type", req.p("type")); row.put("category", req.p("category"));
        row.put("amount", req.p("amount")); row.put("date", req.p("date")); row.put("note", req.p("note"));
        db.t("ledger").add(row);
        Res.redirect(ex, "/ledger");
    }
    private static void deleteLedger(Req req, HttpExchange ex) throws IOException { Main.db.t("ledger").deleteById(req.p("id")); Res.redirect(ex, "/ledger"); }
}
