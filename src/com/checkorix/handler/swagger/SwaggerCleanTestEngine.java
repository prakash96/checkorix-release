package com.checkorix.handler.swagger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class SwaggerCleanTestEngine implements HttpHandler {

	static ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
	static ObjectMapper jsonMapper = new ObjectMapper();

	public void handle(HttpExchange exchange) throws IOException {

		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
		exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
		exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

		// ✅ Handle preflight request
		if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
			exchange.sendResponseHeaders(204, -1); // no body
			return;
		}

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "Only POST supported");
            return;
        }

        String yaml = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonNode root = yamlMapper.readTree(yaml);

        List<Map<String, Object>> rows = generate(root);

        send(exchange, 200, jsonMapper.writeValueAsString(rows));
    }

    // ================= MAIN =================

    private static List<Map<String, Object>> generate(JsonNode root) {

        List<Map<String, Object>> rows = new ArrayList<>();

        JsonNode paths = root.path("paths");

        paths.fieldNames().forEachRemaining(path -> {

            JsonNode pathItem = paths.get(path);

            process(rows, root, path, "GET", pathItem.get("get"));
            process(rows, root, path, "POST", pathItem.get("post"));
            process(rows, root, path, "PUT", pathItem.get("put"));
            process(rows, root, path, "DELETE", pathItem.get("delete"));
        });

        return rows;
    }

    // ================= PROCESS =================

    private static void process(List<Map<String, Object>> rows,
                                JsonNode root,
                                String path,
                                String method,
                                JsonNode op) {

        if (op == null) return;

        String baseName = method + " " + path;

        JsonNode schema = op.path("requestBody")
                .path("content")
                .path("application/json")
                .path("schema");

        Map<String, Object> validPayload =
                SchemaEngine.generate(root, schema);

        Map<String, Object> headers = extractHeaders(op);

        // ================= POSITIVE =================
        rows.add(row(
                baseName + " - POSITIVE",
                validPayload,
                headers
        ));

        if (validPayload.isEmpty()) return;

        String key = validPayload.keySet().iterator().next();

        // ================= NEGATIVE: missing =================
        Map<String, Object> missing = deepCopy(validPayload);
        missing.remove(key);

        rows.add(row(
                baseName + " - NEG missing " + key,
                missing,
                headers
        ));

        // ================= NEGATIVE: invalid type =================
        Map<String, Object> invalid = deepCopy(validPayload);
        invalid.put(key, 999999);

        rows.add(row(
                baseName + " - NEG type " + key,
                invalid,
                headers
        ));

        // ================= NEGATIVE: null =================
        Map<String, Object> nulled = deepCopy(validPayload);
        nulled.put(key, null);

        rows.add(row(
                baseName + " - NEG null " + key,
                nulled,
                headers
        ));
    }

    // ================= HEADERS =================

    private static Map<String, Object> extractHeaders(JsonNode op) {

        Map<String, Object> headers = new HashMap<>();

        JsonNode params = op.get("parameters");
        if (params != null) {

            for (JsonNode p : params) {

                if ("header".equals(p.path("in").asText())) {
                    headers.put(
                            p.path("name").asText(),
                            sampleValue(p.path("schema"))
                    );
                }
            }
        }

        headers.putIfAbsent("Content-Type", "application/json");

        return headers;
    }

    // ================= ROW =================

    private static Map<String, Object> row(String name,
                                          Map<String, Object> payload,
                                          Map<String, Object> headers) {

        Map<String, Object> r = new HashMap<>();

        r.put("id", UUID.randomUUID().toString());
        r.put("name", name);
        r.put("inputPayload", payload);
        r.put("inputHeaders", headers);

        return r;
    }

    // ================= SAMPLE VALUE =================

    static Object sampleValue(JsonNode prop) {

        if (prop == null) return "value";

        JsonNode enumNode = prop.get("enum");
        if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
            return enumNode.get(0).asText();
        }

        String type = prop.path("type").asText();

        switch (type) {

            case "string":
                return "sample";

            case "integer":
                return 10;

            case "number":
                return 10.5;

            case "boolean":
                return true;

            default:
                return "value";
        }
    }

    // ================= UTIL =================

    private static Map<String, Object> deepCopy(Map<String, Object> input) {
        try {
            String json = jsonMapper.writeValueAsString(input);
            return jsonMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void send(HttpExchange ex, int code, String body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, body.getBytes().length);
        ex.getResponseBody().write(body.getBytes());
        ex.close();
    }
}