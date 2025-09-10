package com.clanboards.http;

/** Simple HTTP response model. */
public final class HttpResponse {
    private final int statusCode;
    private final byte[] body;

    public HttpResponse(int statusCode, byte[] body) {
        this.statusCode = statusCode;
        this.body = body;
    }

    public int getStatusCode() { return statusCode; }
    public byte[] getBody() { return body; }
}

