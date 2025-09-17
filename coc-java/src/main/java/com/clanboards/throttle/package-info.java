/**
 * Rate limiting utilities for managing API request throughput.
 *
 * <p>This package provides thread-safe rate limiting implementations that help
 * respect API rate limits while maximizing throughput. The rate limiter uses
 * a sliding window algorithm to provide fair distribution of requests over time.
 *
 * <h2>Core Components</h2>
 * <ul>
 * <li>{@link com.clanboards.throttle.RateLimiter} - Sliding window rate limiter</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <p>The rate limiter is automatically configured by {@link com.clanboards.CocClient}
 * based on the number of tokens and desired per-token rate. It can also be used
 * independently for other rate limiting scenarios.
 *
 * <pre>{@code
 * // Create a rate limiter allowing 10 requests per second
 * RateLimiter limiter = new RateLimiter(10, 1000);
 *
 * // Acquire permit before making request
 * limiter.acquire();
 * makeApiCall();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>All rate limiting implementations are thread-safe and designed for
 * high-concurrency scenarios.
 *
 * @see com.clanboards.throttle.RateLimiter
 * @see com.clanboards.CocClient
 */
package com.clanboards.throttle;

