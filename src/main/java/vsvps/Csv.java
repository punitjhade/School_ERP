package vsvps;

import java.util.*;

/**
 * Minimal RFC4180-style CSV reader/writer. No external dependencies.
 * Compatible with Python's built-in csv module (same quoting rules),
 * so data files can be shared between the Java app and the Python tools.
 */
public class Csv {

    public static List<List<String>> parse(String content) {
        List<List<String>> rows = new ArrayList<>();
        if (content == null || content.isEmpty()) return rows;
        List<String> row = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int len = content.length();
        while (i < len) {
            char c = content.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && content.charAt(i + 1) == '"') { field.append('"'); i += 2; continue; }
                    inQuotes = false; i++; continue;
                } else {
                    field.append(c); i++; continue;
                }
            } else {
                if (c == '"') { inQuotes = true; i++; continue; }
                if (c == ',') { row.add(field.toString()); field.setLength(0); i++; continue; }
                if (c == '\r') { i++; continue; }
                if (c == '\n') {
                    row.add(field.toString()); field.setLength(0);
                    rows.add(row); row = new ArrayList<>();
                    i++; continue;
                }
                field.append(c); i++;
            }
        }
        // trailing field/row
        if (field.length() > 0 || !row.isEmpty()) {
            row.add(field.toString());
            rows.add(row);
        }
        // drop fully-empty trailing rows
        rows.removeIf(r -> r.size() == 1 && r.get(0).isEmpty());
        return rows;
    }

    public static String formatField(String s) {
        if (s == null) s = "";
        boolean needsQuote = s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r");
        if (!needsQuote) return s;
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    public static String format(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        for (List<String> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(formatField(row.get(i)));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
