package com.checkorix.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.checkorix.model.HttpRequestModel;

public class HttpExecutor {

	// 1. Trust all certificates
    
	static HttpClient client;
    static {
    	TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        };
    	
        try {
        	SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new SecureRandom());
			 // 2. Disable hostname verification
	        SSLParameters sslParams = new SSLParameters();
	        sslParams.setEndpointIdentificationAlgorithm(null);
	        
	        // 3. Build HttpClient
	        client = HttpClient.newBuilder()
	                .sslContext(sslContext)
	                .sslParameters(sslParams)
	                .build();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

       
    }
    

   
   
    
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