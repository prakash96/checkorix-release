package com.checkorix.handler.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.checkorix.utils.DbUtil;
import com.checkorix.utils.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TailHandler implements HttpHandler {

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
            String table = req.get("table");
            String column = req.get("timestampColumn");
            String lastSeenStr = req.get("lastSeen");

            // 🔒 basic validation (prevent SQL injection on identifiers)
            if (!valid(table) || !valid(column)) {
                Utils.write(ex, List.of());
                return;
            }

            DbUtil.DbType dbType = DbUtil.detect(url);

            try (Connection conn = DbUtil.getConnection(url, user, pass)) {

                String sql;
                PreparedStatement ps;

                // 🟢 INITIAL LOAD (latest 1000 rows)
                if (lastSeenStr == null || lastSeenStr.isEmpty()) {

                    sql = buildInitialQuery(dbType, table, column, 1000);

                    ps = conn.prepareStatement(sql);

                } else {
                    // 🟢 INCREMENTAL LOAD

                    sql = "SELECT * FROM " + table +
                          " WHERE " + column + " > ?" +
                          " ORDER BY " + column + " ASC";

                    ps = conn.prepareStatement(sql);

                    // ⚠️ timestamp format: "yyyy-MM-dd HH:mm:ss"
                    ps.setTimestamp(1, Timestamp.valueOf(lastSeenStr));
                }

                ResultSet rs = ps.executeQuery();

                List<Map<String, Object>> rows = new ArrayList<>();

                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();

                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }

                    rows.add(row);
                }

                Utils.write(ex, rows);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.write(ex, List.of());
        }
    }

    // 🚀 DB-SPECIFIC INITIAL QUERY
    private String buildInitialQuery(DbUtil.DbType type, String table, String column, int limit) {

        switch (type) {

            case MYSQL:
            case POSTGRES:
                return "SELECT * FROM " + table +
                       " ORDER BY " + column + " DESC LIMIT " + limit;

            case ORACLE:
                return "SELECT * FROM " + table +
                       " ORDER BY " + column + " DESC FETCH FIRST " + limit + " ROWS ONLY";

            case SQLSERVER:
                return "SELECT TOP " + limit + " * FROM " + table +
                       " ORDER BY " + column + " DESC";

            default:
                throw new RuntimeException("Unsupported DB type");
        }
    }

    // 🔒 allow only safe identifiers
    private boolean valid(String s) {
        return s != null && s.matches("[a-zA-Z0-9_]+");
    }
}