/**
 * Core public API for the Clash of Clans Java client library.
 *
 * <p>This package provides a comprehensive Java client for interacting with the
 * official Clash of Clans API. The library is designed for production use with
 * features including automatic token rotation, rate limiting, and robust error handling.
 *
 * <h2>Quick Start</h2>
 * <pre>{@code
 * // Create client with HTTP transport and authenticator
 * HttpTransport transport = new DefaultHttpTransport();
 * Authenticator auth = new DevSiteAuthenticator(transport);
 * CocClient client = new CocClient(transport, auth);
 *
 * // Login with developer credentials
 * client.login("email@example.com", "password");
 *
 * // Fetch clan information
 * Clan clan = client.getClan("#2PP");
 * System.out.println(clan.getName());
 * }</pre>
 *
 * <h2>Key Components</h2>
 * <ul>
 * <li>{@link com.clanboards.CocClient} - Main API client and entry point</li>
 * <li>{@link com.clanboards.Clan}, {@link com.clanboards.Player} - Core data models</li>
 * <li>{@link com.clanboards.http.HttpTransport} - Pluggable HTTP layer</li>
 * <li>{@link com.clanboards.auth.Authenticator} - Token acquisition strategy</li>
 * <li>{@link com.clanboards.util.TagUtil} - Tag normalization utilities</li>
 * </ul>
 *
 * <h2>Error Handling</h2>
 * <p>The library uses specific exceptions for common API scenarios:
 * <ul>
 * <li>{@link com.clanboards.exceptions.NotFoundException} - Resource not found (HTTP 404)</li>
 * <li>{@link com.clanboards.exceptions.PrivateWarLogException} - Private war data (HTTP 403)</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>The client and all data model classes are thread-safe and designed for
 * concurrent use in multi-threaded applications.
 *
 * @see com.clanboards.CocClient
 * @see <a href="https://developer.clashofclans.com/">Clash of Clans API Documentation</a>
 * @since 0.1.0
 */
package com.clanboards;

