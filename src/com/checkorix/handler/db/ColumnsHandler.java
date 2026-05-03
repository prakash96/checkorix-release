package com.checkorix.handler.db;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.checkorix.utils.DbUtil;
import com.checkorix.utils.Utils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ColumnsHandler implements HttpHandler {

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

            try (Connection conn = DbUtil.getConnection(
                    req.get("jdbcUrl"),
                    req.get("username"),
                    req.get("password"))) {

                DatabaseMetaData meta = conn.getMetaData();
                ResultSet rs = meta.getColumns(null, null, req.get("table"), "%");

                List<String> cols = new ArrayList<>();

                while (rs.next()) {
                    int type = rs.getInt("DATA_TYPE");

                    if (type == Types.TIMESTAMP || type == Types.DATE) {
                        cols.add(rs.getString("COLUMN_NAME"));
                    }
                }

                Utils.write(ex, cols);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Utils.write(ex, List.of());
        }
    }
}