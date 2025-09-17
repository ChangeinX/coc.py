package com.clanboards.http;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable HTTP request model used by the pluggable transport system.
 *
 * This class represents an HTTP request with method, URL, headers, and body.
 * Instances are created using the builder pattern for clean construction.
 * All instances are immutable and thread-safe.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see HttpTransport#execute(HttpRequest)
 * @see HttpResponse
 */
public final class HttpRequest {
    /**
     * Supported HTTP methods for API requests.
     */
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

    /**
     * Returns the HTTP method for this request.
     *
     * @return HTTP method (GET, POST, PUT, DELETE)
     */
    public Method getMethod() { return method; }
    /**
     * Returns the complete URL for this request.
     *
     * @return request URL including protocol, host, path, and query parameters
     */
    public String getUrl() { return url; }
    /**
     * Returns an unmodifiable map of HTTP headers.
     *
     * @return immutable map of header name-value pairs
     */
    public Map<String, String> getHeaders() { return headers; }
    /**
     * Returns the request body as a byte array.
     *
     * For GET and DELETE requests, this is typically an empty array.
     * For POST and PUT requests, this contains the request payload.
     *
     * @return request body bytes (never null)
     */
    public byte[] getBody() { return body; }

    /**
     * Creates a new builder for constructing HTTP requests.
     *
     * @return new builder instance
     */
    public static Builder newBuilder() { return new Builder(); }

    /**
     * Builder for constructing immutable HttpRequest instances.
     *
     * Provides a fluent interface for setting request properties.
     * Thread-safety: Builder instances are not thread-safe.
     */
    public static final class Builder {
        private Method method;
        private String url;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body = new byte[0];

        /**
         * Sets the HTTP method for the request.
         *
         * @param method HTTP method to use
         * @return this builder for method chaining
         */
        public Builder method(Method method) { this.method = method; return this; }
        /**
         * Sets the URL for the request.
         *
         * @param url complete request URL
         * @return this builder for method chaining
         */
        public Builder url(String url) { this.url = url; return this; }
        /**
         * Adds an HTTP header to the request.
         *
         * @param name header name
         * @param value header value
         * @return this builder for method chaining
         */
        public Builder header(String name, String value) { this.headers.put(name, value); return this; }
        /**
         * Sets the request body.
         *
         * @param body request body bytes (null will be converted to empty array)
         * @return this builder for method chaining
         */
        public Builder body(byte[] body) { this.body = body; return this; }

        /**
         * Builds an immutable HttpRequest instance.
         *
         * @return new HttpRequest with configured properties
         */
        public HttpRequest build() { return new HttpRequest(method, url, headers, body); }
    }
}

