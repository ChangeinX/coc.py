package com.clanboards.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** Simple immutable HTTP request model used by CocClient's pluggable transport. */
public final class HttpRequest {
    public enum Method { GET, POST, PUT, DELETE }

    private final Method method;
    private final String url;
    private final Map<String, String> headers;
    private final byte[] body;

    private HttpRequest(Method method, String url, Map<String, String> headers, byte[] body) {
        this.method = method;
        this.url = url;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
    }

    public Method getMethod() { return method; }
    public String getUrl() { return url; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody() { return body; }

    public static Builder newBuilder() { return new Builder(); }

    public static final class Builder {
        private Method method;
        private String url;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body = new byte[0];

        public Builder method(Method method) { this.method = method; return this; }
        public Builder url(String url) { this.url = url; return this; }
        public Builder header(String name, String value) { this.headers.put(name, value); return this; }
        public Builder body(byte[] body) { this.body = body; return this; }

        public HttpRequest build() { return new HttpRequest(method, url, headers, body); }
    }
}

