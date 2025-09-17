package com.clanboards.http;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

/**
 * Default HTTP transport implementation using Java 11+ HttpClient.
 *
 * This implementation provides production-ready HTTP transport with automatic
 * cookie management, configurable timeouts, and proper error handling.
 * Supports all standard HTTP methods required by the Clash of Clans API.
 *
 * Features:
 * - Automatic cookie handling for session management
 * - 20-second connection timeout
 * - 30-second request timeout
 * - Thread-safe for concurrent use
 *
 * Thread-safety: This class is thread-safe and designed for concurrent use.
 *
 * @see HttpTransport
 */
public class DefaultHttpTransport implements HttpTransport {
    private final HttpClient client;

    /**
     * Creates a default HTTP transport with cookie support and timeouts.
     *
     * Configures the underlying HttpClient with:
     * - Universal cookie acceptance for session management
     * - 20-second connection timeout
     * - Automatic redirect following
     */
    public DefaultHttpTransport() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .cookieHandler(cookieManager)
                .build();
    }

    /**
     * Executes an HTTP request using the Java HttpClient.
     *
     * Converts the custom HttpRequest to a standard Java HttpRequest,
     * executes it with proper timeout handling, and converts the response
     * back to the custom HttpResponse format.
     *
     * @param request the HTTP request to execute
     * @return HTTP response with status code and body
     * @throws RuntimeException if the request is interrupted, times out,
     *         or encounters I/O errors
     */
    @Override
    public HttpResponse execute(HttpRequest request) {
        try {
            java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(request.getUrl()))
                    .timeout(Duration.ofSeconds(30));

            // Apply method and body
            switch (request.getMethod()) {
                case GET -> builder.GET();
                case POST -> builder.POST(BodyPublishers.ofByteArray(request.getBody()));
                case PUT -> builder.PUT(BodyPublishers.ofByteArray(request.getBody()));
                case DELETE -> builder.DELETE();
                default -> throw new IllegalArgumentException("Unsupported method: " + request.getMethod());
            }

            // Headers
            for (Map.Entry<String, String> h : request.getHeaders().entrySet()) {
                builder.header(h.getKey(), h.getValue());
            }

            var httpResp = client.send(builder.build(), BodyHandlers.ofByteArray());
            return new HttpResponse(httpResp.statusCode(), httpResp.body());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("HTTP interrupted", e);
        } catch (IOException e) {
            throw new RuntimeException("HTTP I/O error", e);
        }
    }
}

