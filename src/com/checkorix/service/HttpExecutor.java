package com.checkorix.service;

import com.checkorix.model.HttpRequestModel;

import java.net.URI;
import java.net.http.*;
import java.util.Map;

public class HttpExecutor {

    private static final HttpClient client = HttpClient.newHttpClient();

    public static HttpResponse<byte[]> execute(HttpRequestModel req) throws Exception {

        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(req.url));

        // headers
        if (req.headers != null) {
            for (Map.Entry<String, String> h : req.headers.entrySet()) {
                builder.header(h.getKey(), h.getValue());
            }
        }

        String method = req.method == null ? "GET" : req.method.toUpperCase();

        HttpRequest.BodyPublisher body =
                (req.body != null)
                        ? HttpRequest.BodyPublishers.ofString(req.body)
                        : HttpRequest.BodyPublishers.noBody();

        switch (method) {

            case "POST":
                builder.POST(body);
                break;

            case "PUT":
                builder.PUT(body);
                break;

            case "DELETE":
                builder.method("DELETE", body);
                break;

            default:
                builder.GET();
        }

        HttpResponse<byte[]> response =
                client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());

       
        return response;
    }
}