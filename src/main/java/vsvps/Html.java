package vsvps;

import java.util.*;

public class Html {

    public static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    public static String money(String amountStr) {
        double amount;
        try { amount = Double.parseDouble(amountStr); } catch (Exception e) { amount = 0; }
        return "\u20B9" + String.format("%,.0f", amount);
    }

    public static String fmtDate(String iso) {
        if (iso == null || iso.isBlank()) return "\u2014";
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(iso);
            return d.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
        } catch (Exception e) { return iso; }
    }

    public static String pill(String status) {
        String cls = switch (status == null ? "" : status) {
            case "Present", "Paid", "Approved", "Resolved", "Completed", "Converted", "Issued", "Returned" -> "pill-success";
            case "Absent", "Due", "Rejected", "Open" -> "pill-danger";
            case "Pending", "New", "Followed Up" -> "pill-gold";
            default -> "pill-info";
        };
        return "<span class=\"pill " + cls + "\">" + esc(status) + "</span>";
    }

    // ---------------------------------------------------------------
    // Nav menu config per role: {view, icon, label}
    // ---------------------------------------------------------------
    public static final Map<String, String[][]> MENUS = new LinkedHashMap<>();
    static {
        MENUS.put("admin", new String[][]{
            {"home", "\uD83C\uDFE0", "Home"}, {"students", "\uD83C\uDF93", "Students"}, {"teachers", "\uD83E\uDDD1\u200D\uD83C\uDFEB", "Teachers"},
            {"classes", "\uD83C\uDFF7", "Classes & Houses"}, {"subjects", "\uD83D\uDCD8", "Subjects"}, {"faculty", "\uD83D\uDC65", "Faculty Info"},
            {"admissions", "\uD83D\uDCDD", "Admissions"}, {"attendance", "\u2705", "Attendance"}, {"timetable", "\uD83D\uDDD3", "Timetable"},
            {"homework", "\uD83D\uDCDA", "Homework"}, {"exams", "\uD83D\uDCDD", "Exams & Marks"}, {"fees", "\uD83D\uDCB0", "Fees"},
            {"ledger", "\uD83D\uDCD2", "Ledger"}, {"leave", "\uD83C\uDFD6", "Leave"}, {"notices", "\uD83D\uDCE2", "Notices"},
            {"idcards", "\uD83E\uDEAA", "ID Cards"}, {"certificates", "\uD83D\uDCDC", "Certificates"}, {"library", "\uD83D\uDCD6", "Library"},
            {"gallery", "\uD83D\uDDBC", "Gallery"}, {"events", "\uD83C\uDF89", "Events"}, {"reminders", "\u23F0", "Reminders"},
            {"tickets", "\uD83C\uDFAB", "Support Tickets"}, {"feedback", "\uD83D\uDCAC", "Feedback"}, {"hostel", "\uD83C\uDFE0", "Hostel Info"},
            {"logins", "\uD83D\uDD10", "Manage Logins"}, {"profile", "\u2699", "Profile"},
        });
        MENUS.put("teacher", new String[][]{
            {"home", "\uD83C\uDFE0", "Home"}, {"attendance", "\u2705", "Attendance"}, {"timetable", "\uD83D\uDDD3", "Timetable"},
            {"homework", "\uD83D\uDCDA", "Homework"}, {"exams", "\uD83D\uDCDD", "Exams & Marks"}, {"faculty", "\uD83D\uDC65", "Faculty Info"},
            {"students", "\uD83C\uDF93", "Students"}, {"leave", "\uD83C\uDFD6", "Leave"}, {"notices", "\uD83D\uDCE2", "Notices"},
            {"idcards", "\uD83E\uDEAA", "ID Cards"}, {"library", "\uD83D\uDCD6", "Library"}, {"gallery", "\uD83D\uDDBC", "Gallery"},
            {"events", "\uD83C\uDF89", "Events"}, {"reminders", "\u23F0", "Reminders"}, {"tickets", "\uD83C\uDFAB", "Support Tickets"},
            {"feedback", "\uD83D\uDCAC", "Feedback"}, {"profile", "\u2699", "Profile"},
        });
        MENUS.put("accountant", new String[][]{
            {"home", "\uD83C\uDFE0", "Home"}, {"fees", "\uD83D\uDCB0", "Fees"}, {"ledger", "\uD83D\uDCD2", "Ledger"},
            {"faculty", "\uD83D\uDC65", "Faculty Info"}, {"leave", "\uD83C\uDFD6", "Leave"}, {"notices", "\uD83D\uDCE2", "Notices"},
            {"tickets", "\uD83C\uDFAB", "Support Tickets"}, {"feedback", "\uD83D\uDCAC", "Feedback"}, {"profile", "\u2699", "Profile"},
        });
        MENUS.put("parent", new String[][]{
            {"home", "\uD83C\uDFE0", "Home"}, {"attendance", "\u2705", "Attendance"}, {"timetable", "\uD83D\uDDD3", "Timetable"},
            {"homework", "\uD83D\uDCDA", "Homework"}, {"exams", "\uD83D\uDCDD", "Exams & Marks"}, {"fees", "\uD83D\uDCB0", "Fees"},
            {"leave", "\uD83C\uDFD6", "Leave"}, {"notices", "\uD83D\uDCE2", "Notices"}, {"certificates", "\uD83D\uDCDC", "Certificates"},
            {"library", "\uD83D\uDCD6", "Library"}, {"gallery", "\uD83D\uDDBC", "Gallery"}, {"events", "\uD83C\uDF89", "Events"},
            {"reminders", "\u23F0", "Reminders"}, {"tickets", "\uD83C\uDFAB", "Support Tickets"}, {"feedback", "\uD83D\uDCAC", "Feedback"},
            {"hostel", "\uD83C\uDFE0", "Hostel Info"}, {"profile", "\u2699", "Profile"},
        });
        MENUS.put("student", new String[][]{
            {"home", "\uD83C\uDFE0", "Home"}, {"attendance", "\u2705", "Attendance"}, {"timetable", "\uD83D\uDDD3", "Timetable"},
            {"homework", "\uD83D\uDCDA", "Homework"}, {"exams", "\uD83D\uDCDD", "Exams & Marks"}, {"fees", "\uD83D\uDCB0", "Fees"},
            {"leave", "\uD83C\uDFD6", "Leave"}, {"notices", "\uD83D\uDCE2", "Notices"}, {"certificates", "\uD83D\uDCDC", "Certificates"},
            {"idcards", "\uD83E\uDEAA", "ID Card"}, {"library", "\uD83D\uDCD6", "Library"}, {"gallery", "\uD83D\uDDBC", "Gallery"},
            {"events", "\uD83C\uDF89", "Events"}, {"reminders", "\u23F0", "Reminders"}, {"tickets", "\uD83C\uDFAB", "Support Tickets"},
            {"feedback", "\uD83D\uDCAC", "Feedback"}, {"hostel", "\uD83C\uDFE0", "Hostel Info"}, {"profile", "\u2699", "Profile"},
        });
    }

    public static final Map<String, String> TITLES = new HashMap<>();
    static {
        TITLES.put("home", "Home"); TITLES.put("students", "Students"); TITLES.put("teachers", "Teachers");
        TITLES.put("classes", "Classes & Houses"); TITLES.put("subjects", "Subjects"); TITLES.put("faculty", "Faculty Info");
        TITLES.put("admissions", "Admissions"); TITLES.put("attendance", "Attendance"); TITLES.put("timetable", "Timetable");
        TITLES.put("homework", "Homework"); TITLES.put("exams", "Exams & Marks"); TITLES.put("fees", "Fees");
        TITLES.put("ledger", "Income / Expense Ledger"); TITLES.put("leave", "Leave Applications"); TITLES.put("notices", "Notices");
        TITLES.put("idcards", "ID Cards"); TITLES.put("certificates", "Certificates"); TITLES.put("library", "Library");
        TITLES.put("gallery", "Gallery"); TITLES.put("events", "Events"); TITLES.put("reminders", "Reminders");
        TITLES.put("tickets", "Support Tickets"); TITLES.put("feedback", "Feedback"); TITLES.put("hostel", "Hostel Info");
        TITLES.put("logins", "Manage Logins"); TITLES.put("profile", "Profile / Settings");
    }

    public static final Map<String, String> ICONS = new HashMap<>();
    static {
        ICONS.put("home", "\uD83C\uDFE0"); ICONS.put("students", "\uD83C\uDF93"); ICONS.put("teachers", "\uD83E\uDDD1\u200D\uD83C\uDFEB");
        ICONS.put("classes", "\uD83C\uDFF7"); ICONS.put("subjects", "\uD83D\uDCD8"); ICONS.put("faculty", "\uD83D\uDC65");
        ICONS.put("admissions", "\uD83D\uDCDD"); ICONS.put("attendance", "\u2705"); ICONS.put("timetable", "\uD83D\uDDD3");
        ICONS.put("homework", "\uD83D\uDCDA"); ICONS.put("exams", "\uD83D\uDCDD"); ICONS.put("fees", "\uD83D\uDCB0");
        ICONS.put("ledger", "\uD83D\uDCD2"); ICONS.put("leave", "\uD83C\uDFD6"); ICONS.put("notices", "\uD83D\uDCE2");
        ICONS.put("idcards", "\uD83E\uDEAA"); ICONS.put("certificates", "\uD83D\uDCDC"); ICONS.put("library", "\uD83D\uDCD6");
        ICONS.put("gallery", "\uD83D\uDDBC"); ICONS.put("events", "\uD83C\uDF89"); ICONS.put("reminders", "\u23F0");
        ICONS.put("tickets", "\uD83C\uDFAB"); ICONS.put("feedback", "\uD83D\uDCAC"); ICONS.put("hostel", "\uD83C\uDFE0");
        ICONS.put("logins", "\uD83D\uDD10"); ICONS.put("profile", "\u2699");
    }

    public static String page(String view, Session session, String contentHtml) {
        String[][] menu = MENUS.getOrDefault(session.role, MENUS.get("student"));
        StringBuilder nav = new StringBuilder();
        for (String[] item : menu) {
            boolean active = item[0].equals(view);
            nav.append("<a class=\"nav-item").append(active ? " active" : "").append("\" href=\"/").append(item[0]).append("\">")
               .append("<span class=\"ic\">").append(item[1]).append("</span><span>").append(esc(item[2])).append("</span></a>");
        }
        String title = TITLES.getOrDefault(view, "VSVPS Connect");
        String icon = ICONS.getOrDefault(view, "\uD83D\uDCC4");
        return "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"/>" +
               "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>" +
               "<title>VSVPS Connect \u2014 " + esc(title) + "</title>" +
               "<link rel=\"stylesheet\" href=\"/css/style.css?v=16\"/></head><body>" +
               "<div class=\"app-shell\">" +
               "<aside class=\"sidebar\" id=\"sidebar\">" +
               "<div class=\"sidebar-brand\"><img src=\"/images/logo-v2.png\" alt=\"VSVPS logo\" class=\"logo-dot\"/><div><div class=\"name\">VSVPS Connect</div><div class=\"sub\">School ERP</div></div></div>" +
               "<nav class=\"sidebar-nav\">" + nav + "</nav>" +
               "<div class=\"sidebar-foot\"><form method=\"post\" action=\"/logout\"><button class=\"btn btn-outline\" style=\"width:100%;justify-content:center\" type=\"submit\">\uD83D\uDEAA Log Out</button></form></div>" +
               "</aside>" +
               "<div class=\"main\">" +
               "<header class=\"topbar no-print\"><div class=\"left\"><div class=\"title\"><span class=\"topbar-icon\">" + icon + "</span>" + esc(title) + "</div></div>" +
               "<div class=\"right\"><div class=\"user-chip\"><span>" + esc(session.name) + "</span><span class=\"role-pill\">" + esc(session.role) + "</span></div></div></header>" +
               "<main id=\"content\">" + contentHtml + "</main>" +
               "</div></div></body></html>";
    }

    public static String errorPage(String heading, String detail, String backHref) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>Error \u2014 VSVPS Connect</title>" +
               "<link rel=\"stylesheet\" href=\"/css/style.css?v=16\"/></head><body style=\"background:var(--bg)\">" +
               "<div style=\"max-width:640px;margin:60px auto;padding:0 20px\">" +
               "<div class=\"card error-card\"><div class=\"icon\">\u26A0\uFE0F</div><h2>" + esc(heading) + "</h2>" +
               "<pre>" + esc(detail) + "</pre>" +
               "<div class=\"btn-row\" style=\"justify-content:center\"><a class=\"btn btn-primary\" href=\"" + (backHref != null ? backHref : "/home") + "\">\u2190 Back to Home</a></div>" +
               "</div></div></body></html>";
    }

    // ---------------------------------------------------------------
    // Small reusable component builders
    // ---------------------------------------------------------------
    public static String card(String innerHtml) { return "<div class=\"card\">" + innerHtml + "</div>"; }

    public static String statCard(String label, String value, String accentClass) {
        return statCard(label, value, accentClass, null);
    }

    public static String statCard(String label, String value, String accentClass, String icon) {
        String iconHtml = icon != null ? "<div class=\"stat-card-icon\">" + icon + "</div>" : "";
        return "<div class=\"card stat-card " + accentClass + "\"><div><div class=\"label\">" + esc(label) + "</div><div class=\"value\">" + value + "</div></div>" + iconHtml + "</div>";
    }

    public static String textField(String label, String name, String value, String type) {
        return "<div class=\"field\"><label>" + esc(label) + "</label><input type=\"" + type + "\" name=\"" + name + "\" value=\"" + esc(value) + "\"/></div>";
    }

    public static String textareaField(String label, String name, String value) {
        return "<div class=\"field\" style=\"grid-column:1/-1\"><label>" + esc(label) + "</label><textarea name=\"" + name + "\">" + esc(value) + "</textarea></div>";
    }

    public static String selectField(String label, String name, String[] options, String selected) {
        StringBuilder sb = new StringBuilder("<div class=\"field\"><label>" + esc(label) + "</label><select name=\"" + name + "\">");
        for (String o : options) sb.append("<option value=\"").append(esc(o)).append("\"").append(o.equals(selected) ? " selected" : "").append(">").append(esc(o)).append("</option>");
        sb.append("</select></div>");
        return sb.toString();
    }

    public static String checkboxField(String label, String name, boolean checked) {
        return "<div class=\"field checkbox-row\"><input type=\"checkbox\" name=\"" + name + "\" " + (checked ? "checked" : "") + "/><label style=\"margin:0\">" + esc(label) + "</label></div>";
    }

    public static String hidden(String name, String value) {
        return "<input type=\"hidden\" name=\"" + name + "\" value=\"" + esc(value) + "\"/>";
    }

    /** A bare print-friendly page (no sidebar/topbar) for receipts, certificates, ID cards, report cards.
     *  No JavaScript is used anywhere in this app, so we can't auto-trigger window.print(); instead we
     *  give the person a plain instruction to use their browser's own Print command. */
    public static String printPage(String title, String innerHtml) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"/><title>" + esc(title) + "</title>" +
               "<link rel=\"stylesheet\" href=\"/css/style.css?v=16\"/></head><body style=\"background:#fff;padding:24px\">" +
               innerHtml +
               "<div class=\"no-print\" style=\"margin-top:20px;padding:12px 16px;background:var(--info-bg);border-radius:8px;color:var(--info);font-size:13px;font-weight:600\">" +
               "\uD83D\uDDA8 To print this page, use your browser's Print command (Ctrl+P on Windows/Linux, \u2318+P on Mac), then choose \"Save as PDF\" if you'd like a file instead." +
               "</div>" +
               "<div class=\"no-print\" style=\"margin-top:10px\"><a class=\"btn btn-outline\" href=\"/home\">\u2190 Back to Home</a></div>" +
               "</body></html>";
    }
}
