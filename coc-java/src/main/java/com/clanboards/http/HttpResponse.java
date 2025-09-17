package com.clanboards.http;

/**
 * Immutable HTTP response model containing status code and response body.
 *
 * This class represents the result of an HTTP request execution, providing
 * access to the response status code and body data. Instances are created
 * by HttpTransport implementations and consumed by CocClient.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see HttpTransport#execute(HttpRequest)
 * @see HttpRequest
 */
public final class HttpResponse {
    private final int statusCode;
    private final byte[] body;

    /**
     * Creates an HTTP response with the specified status code and body.
     *
     * @param statusCode HTTP status code (e.g., 200, 404, 500)
     * @param body response body bytes (null will be stored as provided)
     */
    public HttpResponse(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    /**
     * Returns the HTTP status code for this response.
     *
     * Status codes follow standard HTTP conventions:
     * - 2xx: Success
     * - 4xx: Client errors (bad request, not found, etc.)
     * - 5xx: Server errors
     *
     * @return HTTP status code
     */
    public int getStatusCode() { return statusCode; }
    /**
     * Returns the response body as a byte array.
     *
     * The body contains the raw response data from the server,
     * typically JSON for API responses. May be null or empty
     * for certain response types.
     *
     * @return response body bytes (may be null)
     */
    public byte[] getBody() { return body; }
}

