package com.checkorix.utils;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

public class Utils {

    static ObjectMapper mapper = new ObjectMapper();

    public static <T> T read(HttpExchange ex, Class<T> clazz) throws IOException {
        return mapper.readValue(ex.getRequestBody(), clazz);
    }

    public static void write(HttpExchange ex, Object obj) throws IOException {
        byte[] resp = mapper.writeValueAsBytes(obj);

        ex.getResponseHeaders().add("Content-Type", "application/json");

        ex.sendResponseHeaders(200, resp.length);
        ex.getResponseBody().write(resp);
        ex.close();
    }
    
    public static void write(HttpExchange ex, Object obj, int statusCode) throws IOException {
        byte[] resp = mapper.writeValueAsBytes(obj);

        ex.getResponseHeaders().add("Content-Type", "application/json");

        ex.sendResponseHeaders(statusCode, resp.length);
        ex.getResponseBody().write(resp);
        ex.close();
    }
}