package vsvps;

import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class Res {

    public static void html(HttpExchange ex, int status, String body) throws IOException {
        send(ex, status, "text/html; charset=utf-8", body.getBytes(StandardCharsets.UTF_8));
    }

    public static void redirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    public static void setCookie(HttpExchange ex, String name, String value, int maxAgeSeconds) {
        String cookie = name + "=" + value + "; Path=/; HttpOnly" + (maxAgeSeconds >= 0 ? "; Max-Age=" + maxAgeSeconds : "");
        ex.getResponseHeaders().add("Set-Cookie", cookie);
    }

    public static void download(HttpExchange ex, String filename, String contentType, byte[] data) throws IOException {
        ex.getResponseHeaders().add("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        send(ex, 200, contentType, data);
    }

    public static void notFound(HttpExchange ex) throws IOException {
        html(ex, 404, Html.errorPage("Page Not Found", "The page you're looking for doesn't exist.", null));
    }

    static void send(HttpExchange ex, int status, String contentType, byte[] data) throws IOException {
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, data.length == 0 ? -1 : data.length);
        if (data.length > 0) {
            try (OutputStream os = ex.getResponseBody()) { os.write(data); }
        } else {
            ex.close();
        }
    }
}
