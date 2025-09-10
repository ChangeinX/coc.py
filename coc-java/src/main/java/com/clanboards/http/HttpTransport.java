package com.clanboards.http;

/**
 * Pluggable HTTP transport used by CocClient to perform requests.
 * Tests can supply a fake implementation to avoid network I/O.
 */
public interface HttpTransport {
    HttpResponse execute(HttpRequest request);
}

