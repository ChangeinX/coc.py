/**
 * HTTP transport abstractions for pluggable networking implementations.
 *
 * <p>This package provides a clean abstraction layer for HTTP communication,
 * allowing the client to work with different transport implementations. This
 * design enables easy testing with mock transports and customization of
 * HTTP behavior for different environments.
 *
 * <h2>Core Components</h2>
 * <ul>
 * <li>{@link com.clanboards.http.HttpTransport} - Main transport interface</li>
 * <li>{@link com.clanboards.http.DefaultHttpTransport} - Production implementation using Java 11+ HttpClient</li>
 * <li>{@link com.clanboards.http.HttpRequest} - Immutable request model</li>
 * <li>{@link com.clanboards.http.HttpResponse} - Immutable response model</li>
 * </ul>
 *
 * <h2>Usage Pattern</h2>
 * <pre>{@code
 * // Production usage
 * HttpTransport transport = new DefaultHttpTransport();
 *
 * // Testing with mock
 * HttpTransport mockTransport = new FakeHttpTransport();
 * }</pre>
 *
 * <p>All implementations must be thread-safe for concurrent use.
 *
 * @see com.clanboards.http.HttpTransport
 * @see com.clanboards.http.DefaultHttpTransport
 */
package com.clanboards.http;

