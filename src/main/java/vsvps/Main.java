package vsvps;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.*;

public class Main {

    public static Db db;
    public static final SessionStore sessions = new SessionStore();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        Path dataDir = Paths.get(System.getProperty("vsvps.dataDir", "data"));
        Files.createDirectories(dataDir);
        db = new Db(dataDir);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", Main::route);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("=======================================================");
        System.out.println(" VSVPS Connect School ERP running at http://localhost:" + port);
        System.out.println(" Data directory: " + dataDir.toAbsolutePath());
        System.out.println(" Press Ctrl+C to stop.");
        System.out.println("=======================================================");
    }

    private static void route(HttpExchange ex) {
        try {
            Req req = new Req(ex);
            String path = req.path;

            // Static assets
            if (path.startsWith("/css/") || path.startsWith("/images/")) {
                Static.serve(ex, path);
                return;
            }

            // Resolve session (if any)
            String sid = req.cookies.get("SID");
            Session session = sessions.get(sid);
            req.session = session;

            // Public routes
            if (path.equals("/")) {
                Res.redirect(ex, session != null ? "/home" : "/login");
                return;
            }
            if (path.equals("/login")) {
                Auth.login(req, ex);
                return;
            }
            if (path.equals("/logout")) {
                Auth.logout(req, ex);
                return;
            }

            // Everything else requires auth
            if (session == null) {
                Res.redirect(ex, "/login");
                return;
            }

            boolean handled = Academics.dispatch(req, ex, path)
                    || Finance.dispatch(req, ex, path)
                    || Services.dispatch(req, ex, path)
                    || Core.dispatch(req, ex, path);

            if (!handled) {
                Res.notFound(ex);
            }
        } catch (Util.AccessDenied denied) {
            try {
                Res.html(ex, 403, Html.errorPage("Access Restricted", denied.getMessage(), "/home"));
            } catch (IOException io) {
                /* connection likely already broken; nothing more to do */ }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Res.html(ex, 500, Html.errorPage("Something went wrong loading this page",
                        e.toString() + "\n\n" + stackToString(e), "/home"));
            } catch (IOException io) {
                /* connection likely already broken; nothing more to do */ }
        }
    }

    private static String stackToString(Exception e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement el : e.getStackTrace()) {
            sb.append("  at ").append(el).append("\n");
        }
        return sb.toString();
    }
}
