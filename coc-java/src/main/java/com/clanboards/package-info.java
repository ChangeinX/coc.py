/**
 * Core public API for the Clash of Clans Java client.
 *
 * <p>Key types:
 * - {@link com.clanboards.CocClient} – main entry point for endpoints
 * - Data models like {@link com.clanboards.Clan}, {@link com.clanboards.Player}
 * - Exceptions: {@link com.clanboards.exceptions.NotFoundException},
 *   {@link com.clanboards.exceptions.PrivateWarLogException}
 *
 * <p>Design highlights:
 * - Pluggable HTTP via {@link com.clanboards.http.HttpTransport}
 * - Token rotation ({@link com.clanboards.token.TokenRotator}) and rate limiting
 *   ({@link com.clanboards.throttle.RateLimiter})
 * - Tag normalization utilities in {@link com.clanboards.util.TagUtil}
 */
package com.clanboards;

