package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.nio.file.*;

public class Static {
    private static final Path ROOT = Paths.get(System.getProperty("vsvps.webroot", "webroot"));

    public static void serve(HttpExchange ex, String path) throws IOException {
        // prevent path traversal
        String rel = path.replaceFirst("^/", "");
        Path file = ROOT.resolve(rel).normalize();
        if (!file.startsWith(ROOT) || !Files.exists(file) || Files.isDirectory(file)) {
            Res.html(ex, 404, "Not found");
            return;
        }
        String contentType = contentTypeFor(path);
        byte[] data = Files.readAllBytes(file);
        ex.getResponseHeaders().set("Content-Type", contentType);
        if (path.toLowerCase().endsWith(".css")) {
            // Never cache CSS -- always serve the latest version so style edits show up immediately.
            ex.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");
        } else {
            ex.getResponseHeaders().set("Cache-Control", "public, max-age=86400");
        }
        ex.sendResponseHeaders(200, data.length);
        try (var os = ex.getResponseBody()) { os.write(data); }
    }

    private static String contentTypeFor(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".css")) return "text/css; charset=utf-8";
        if (p.endsWith(".png")) return "image/png";
        if (p.endsWith(".jpg") || p.endsWith(".jpeg")) return "image/jpeg";
        if (p.endsWith(".gif")) return "image/gif";
        if (p.endsWith(".svg")) return "image/svg+xml";
        if (p.endsWith(".webp")) return "image/webp";
        if (p.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
