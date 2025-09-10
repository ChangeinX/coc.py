/**
 * HTTP abstractions decoupling transport concerns from the client.
 *
 * <p>Tests inject fakes of {@link com.clanboards.http.HttpTransport} to avoid
 * network I/O. Production uses {@link com.clanboards.http.DefaultHttpTransport}.
 */
package com.clanboards.http;

