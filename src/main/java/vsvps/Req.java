package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Wraps an HttpExchange with parsed query/form params, cookies, and the current session. */
public class Req {
    public final HttpExchange exchange;
    public final String method;
    public final String path;
    public final Map<String, String> params = new LinkedHashMap<>();
    /** Multi-valued form fields (e.g. repeated checkboxes with the same name), keyed by field name. */
    public final Map<String, List<String>> rawMulti = new LinkedHashMap<>();
    public final Map<String, String> cookies = new HashMap<>();
    public Session session; // set by the router after cookie lookup

    public Req(HttpExchange exchange) throws IOException {
        this.exchange = exchange;
        this.method = exchange.getRequestMethod();
        String rawPath = exchange.getRequestURI().getPath();
        this.path = rawPath;

        String query = exchange.getRequestURI().getRawQuery();
        parseUrlEncoded(query, params, rawMulti);

        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String part : cookieHeader.split(";")) {
                String[] kv = part.trim().split("=", 2);
                if (kv.length == 2) cookies.put(kv[0].trim(), kv[1].trim());
            }
        }

        if ("POST".equalsIgnoreCase(method)) {
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType != null && contentType.contains("application/x-www-form-urlencoded")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                parseUrlEncoded(body, params, rawMulti);
            }
        }
    }

    private static void parseUrlEncoded(String data, Map<String, String> into, Map<String, List<String>> multiInto) {
        if (data == null || data.isEmpty()) return;
        for (String pair : data.split("&")) {
            if (pair.isEmpty()) continue;
            String[] kv = pair.split("=", 2);
            try {
                String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                String val = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                into.put(key, val);
                multiInto.computeIfAbsent(key, k -> new ArrayList<>()).add(val);
            } catch (Exception ignored) { /* skip malformed pair */ }
        }
    }

    public String p(String key) { return params.getOrDefault(key, ""); }
    public String p(String key, String def) { return params.getOrDefault(key, def); }
    public boolean checked(String key) { return "on".equals(params.get(key)) || "true".equals(params.get(key)); }

    /** Splits a multi-valued form field submitted as key0,key1,key2... into a List, up to `max` slots. */
    public List<String> multi(String prefix, int max) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            String v = params.get(prefix + i);
            if (v != null) out.add(v);
        }
        return out;
    }
}
