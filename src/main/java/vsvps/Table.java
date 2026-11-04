package vsvps;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.function.Predicate;

/**
 * A generic, CSV-file-backed table of records. Each record is a
 * LinkedHashMap<String,String> keyed by column name (always has "id").
 * This is the whole "database engine" -- no external DB required.
 */
public class Table {
    public final String name;
    public final List<String> columns;
    public final List<LinkedHashMap<String, String>> rows = new ArrayList<>();
    private final Path file;

    public Table(String name, List<String> columns, Path dataDir) {
        this.name = name;
        this.columns = columns;
        this.file = dataDir.resolve(name + ".csv");
    }

    public void load() {
        rows.clear();
        try {
            if (!Files.exists(file)) return;
            List<List<String>> parsed = Csv.parse(Files.readString(file));
            if (parsed.isEmpty()) return;
            List<String> header = parsed.get(0);
            for (int i = 1; i < parsed.size(); i++) {
                List<String> line = parsed.get(i);
                LinkedHashMap<String, String> row = new LinkedHashMap<>();
                for (int c = 0; c < header.size(); c++) {
                    row.put(header.get(c), c < line.size() ? line.get(c) : "");
                }
                rows.add(row);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load table " + name, e);
        }
    }

    public synchronized void save() {
        try {
            List<List<String>> out = new ArrayList<>();
            out.add(columns);
            for (var r : rows) {
                List<String> line = new ArrayList<>();
                for (String c : columns) line.add(r.getOrDefault(c, ""));
                out.add(line);
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, Csv.format(out));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save table " + name, e);
        }
    }

    public Optional<LinkedHashMap<String, String>> find(String id) {
        if (id == null) return Optional.empty();
        return rows.stream().filter(r -> id.equals(r.get("id"))).findFirst();
    }

    public List<LinkedHashMap<String, String>> all() {
        return rows;
    }

    public List<LinkedHashMap<String, String>> filter(Predicate<LinkedHashMap<String, String>> p) {
        List<LinkedHashMap<String, String>> out = new ArrayList<>();
        for (var r : rows) if (p.test(r)) out.add(r);
        return out;
    }

    public LinkedHashMap<String, String> newRow() {
        LinkedHashMap<String, String> row = new LinkedHashMap<>();
        for (String c : columns) row.put(c, "");
        return row;
    }

    public void add(LinkedHashMap<String, String> row) {
        rows.add(row);
        save();
    }

    public boolean deleteById(String id) {
        boolean removed = rows.removeIf(r -> id.equals(r.get("id")));
        if (removed) save();
        return removed;
    }
}
