package com.checkorix.handler.swagger;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.*;

public class SchemaEngine {

    private static final int MAX_DEPTH = 6;

    public static Map<String, Object> generate(JsonNode root, JsonNode schema) {
        return generate(root, schema, new HashSet<>(), 0);
    }

    private static Map<String, Object> generate(JsonNode root,
                                                 JsonNode schema,
                                                 Set<String> visited,
                                                 int depth) {

        Map<String, Object> result = new HashMap<>();

        if (schema == null || depth > MAX_DEPTH) return result;

        schema = resolveRef(root, schema);

        String nodeKey = schema.toString();
        if (visited.contains(nodeKey)) return result; // 🔥 circular protection
        visited.add(nodeKey);

        // ================= OBJECT =================
        if ("object".equals(schema.path("type").asText())) {

            JsonNode props = schema.path("properties");

            if (props != null) {
                props.fieldNames().forEachRemaining(f -> {
                    JsonNode child = resolveRef(root, props.get(f));
                    result.put(f, generateValue(root, child, visited, depth + 1));
                });
            }

            return result;
        }

        return result;
    }

    // ================= VALUE GENERATION =================

    private static Object generateValue(JsonNode root,
                                        JsonNode schema,
                                        Set<String> visited,
                                        int depth) {

        schema = resolveRef(root, schema);

        // ================= ONEOF =================
        if (schema.has("oneOf")) {
            JsonNode first = schema.get("oneOf").get(0);
            return generateValue(root, first, visited, depth + 1);
        }

        // ================= ANYOF =================
        if (schema.has("anyOf")) {
            JsonNode first = schema.get("anyOf").get(0);
            return generateValue(root, first, visited, depth + 1);
        }

        String type = schema.path("type").asText();

        // ================= ARRAY =================
        if ("array".equals(type)) {

            JsonNode items = schema.get("items");
            if (items == null) return Collections.emptyList();

            Object itemValue = generateValue(root, items, visited, depth + 1);

            return Arrays.asList(itemValue, itemValue);
        }

        // ================= OBJECT =================
        if ("object".equals(type) || schema.has("properties")) {
            return generate(root, schema, visited, depth + 1);
        }

        // ================= ENUM =================
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
            return enumNode.get(0).asText();
        }

        // ================= PRIMITIVES =================
        switch (type) {

            case "string":
                if ("email".equals(schema.path("format").asText())) return "test@example.com";
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

    // ================= REF RESOLVER =================

    private static JsonNode resolveRef(JsonNode root, JsonNode node) {

        if (node == null) return null;

        JsonNode ref = node.get("$ref");

        if (ref == null) return node;

        String refPath = ref.asText().replace("#/", "");

        String[] parts = refPath.split("/");

        JsonNode current = root;

        for (String p : parts) {
            current = current.get(p);
            if (current == null) return node;
        }

        return current;
    }
}