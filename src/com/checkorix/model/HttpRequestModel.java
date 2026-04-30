package com.checkorix.model;

import java.util.Map;

public class HttpRequestModel {

    public String method;
    public String url;
    public Map<String, String> headers;
    public String body;
    
    public HttpRequestModel() {
    	
    }
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public Map<String, String> getHeaders() {
		return headers;
	}
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	public String getBody() {
		return body;
	}
	public void setBody(String body) {
		this.body = body;
	}
    
    
}