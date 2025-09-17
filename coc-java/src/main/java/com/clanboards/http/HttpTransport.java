package com.clanboards.http;

/**
 * Abstraction for HTTP transport used by CocClient to execute API requests.
 *
 * This interface allows for pluggable HTTP implementations, enabling easy testing
 * with mock transports and customization of HTTP behavior (timeouts, retries,
 * connection pooling, etc.). Implementations should be thread-safe for concurrent use.
 *
 * Thread-safety: Implementations must be thread-safe.
 *
 * @see DefaultHttpTransport
 * @see CocClient
 */
public interface HttpTransport {
    /**
     * Executes an HTTP request and returns the response.
     *
     * Implementations should handle all HTTP method types, properly set headers,
     * and manage timeouts and error conditions. Network errors should be wrapped
     * in RuntimeException for consistent error handling.
     *
     * @param request the HTTP request to execute
     * @return HTTP response with status code and body
     * @throws RuntimeException if the request fails due to network issues,
     *         timeouts, or other I/O problems
     */
    HttpResponse execute(HttpRequest request);
}

