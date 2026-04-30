package com.checkorix.handler;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpResponse;

import com.checkorix.model.HttpRequestModel;
import com.checkorix.service.HttpExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ExecuteHandler implements HttpHandler {

	private final ObjectMapper mapper = new ObjectMapper();

	@Override
	public void handle(HttpExchange exchange) {

		try {

			exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
			exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
			exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

			// ✅ Handle preflight request
			if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
				exchange.sendResponseHeaders(204, -1); // no body
				return;
			}

			InputStream is = exchange.getRequestBody();

			HttpRequestModel req = mapper.readValue(is, HttpRequestModel.class);

			HttpResponse<byte[]> response = HttpExecutor.execute(req);

			response.headers().map().forEach((key, values) -> {

				// skip problematic headers
				if (key.equalsIgnoreCase("content-length") || key.equalsIgnoreCase("transfer-encoding")
						|| key.equalsIgnoreCase("connection")) {
					return;
				}

				exchange.getResponseHeaders().put(key, values);
			});

			// ✅ Send status + body
			exchange.sendResponseHeaders(response.statusCode(), response.body().length);

			try (OutputStream os = exchange.getResponseBody()) {
				os.write(response.body());
			}

		} catch (Exception e) {

			try {
				String err = "{\"error\":\"" + e.getMessage() + "\"}";
				exchange.sendResponseHeaders(500, err.length());
				exchange.getResponseBody().write(err.getBytes());
				exchange.close();
			} catch (Exception ignored) {
			}
		}
	}
}