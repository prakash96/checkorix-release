package com.checkorix.handler.db;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.checkorix.utils.DbUtil;
import com.checkorix.utils.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TablesHandler implements HttpHandler {

    public void handle(HttpExchange ex) throws IOException {

        try {
        	
        	ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        	ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        	ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

			// ✅ Handle preflight request
			if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
				ex.sendResponseHeaders(204, -1); // no body
				return;
			}
            @SuppressWarnings("unchecked")
			Map<String, String> req = (Map<String, String>)Utils.read(ex, Map.class);

            try (Connection conn = DbUtil.getConnection(
                    req.get("jdbcUrl"),
                    req.get("username"),
                    req.get("password"))) {

                DatabaseMetaData meta = conn.getMetaData();
                ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"});

                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }

                Utils.write(ex, tables);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.write(ex, List.of());
        }
    }
}