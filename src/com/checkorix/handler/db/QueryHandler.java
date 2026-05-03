package com.checkorix.handler.db;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.checkorix.utils.DbUtil;
import com.checkorix.utils.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class QueryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {

    	ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    	ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
    	ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

		// ✅ Handle preflight request
		if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
			ex.sendResponseHeaders(204, -1); // no body
			return;
		}
        try {
            Map<String, String> req = Utils.read(ex, Map.class);

            String url = req.get("jdbcUrl");
            String user = req.get("username");
            String pass = req.get("password");
            String query = req.get("query");

            // 🔒 Basic validation
            if (query == null || query.trim().isEmpty()) {
                Utils.write(ex, error("Query is empty"));
                return;
            }

            // 🔒 Allow ONLY SELECT queries (important)
            String normalized = query.trim().toLowerCase();
            if (!normalized.startsWith("select")) {
                Utils.write(ex, error("Only SELECT queries are allowed"));
                return;
            }

            try (Connection conn = DbUtil.getConnection(url, user, pass);
                 PreparedStatement ps = conn.prepareStatement(query)) {

                // ⏱ Optional timeout (seconds)
                ps.setQueryTimeout(10);

                ResultSet rs = ps.executeQuery();

                List<Map<String, Object>> rows = new ArrayList<>();

                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= colCount; i++) {
                        String colName = meta.getColumnLabel(i);
                        Object value = rs.getObject(i);

                        row.put(colName, value);
                    }

                    rows.add(row);
                }

                Utils.write(ex, rows);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.write(ex, error(e.getMessage()));
        }
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> err = new HashMap<>();
        err.put("error", msg);
        return err;
    }
}