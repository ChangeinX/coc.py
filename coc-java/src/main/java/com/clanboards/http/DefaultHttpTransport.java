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

/** Default implementation based on Java 11 HttpClient with cookie support. */
public class DefaultHttpTransport implements HttpTransport {
    private final HttpClient client;

    public DefaultHttpTransport() {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .cookieHandler(cookieManager)
                .build();
    }

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

