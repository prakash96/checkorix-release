package com.checkorix.handler;
import com.sun.net.httpserver.*;
import java.io.*;

public class StaticHandler implements HttpHandler {

	private static final String BASE = "/checkorix-release";
	
    @Override
    public void handle(HttpExchange exchange) throws IOException {

        String path = exchange.getRequestURI().getPath();

        // 1. Default route
        if (path.equals(BASE)) {
            path =  "/resources/static/index.html";
        } else {
            path = "/resources/static" + path.replace(BASE, "");
        }

        InputStream is = getClass().getResourceAsStream(path);

        // 2. React SPA fallback (VERY IMPORTANT)
        if (is == null) {
            is = getClass().getResourceAsStream("/resources/static/index.html");
        }

        if (is == null) {
            send(exchange, 404, "Not found");
            return;
        }

        byte[] data = is.readAllBytes();

        exchange.getResponseHeaders().add("Content-Type", getContentType(path));
        exchange.sendResponseHeaders(200, data.length);

        OutputStream os = exchange.getResponseBody();
        os.write(data);
        os.close();
    }

    private String getContentType(String path) {

        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".svg")) return "image/svg+xml";

        return "text/plain";
    }

    private void send(HttpExchange ex, int code, String msg) throws IOException {
        ex.sendResponseHeaders(code, msg.length());
        ex.getResponseBody().write(msg.getBytes());
        ex.close();
    }
}