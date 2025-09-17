package com.clanboards.throttle;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Thread-safe sliding-window rate limiter for API request throttling.
 *
 * This implementation uses a sliding window algorithm to enforce rate limits,
 * allowing a maximum number of requests within a specified time window.
 * Requests that would exceed the limit are blocked until capacity becomes available.
 *
 * The limiter is designed for high-throughput scenarios and provides fair
 * request distribution over time. It's particularly useful for respecting
 * API rate limits while maximizing throughput.
 *
 * Thread-safety: This class is thread-safe and designed for concurrent use.
 *
 * @see CocClient
 */
public final class RateLimiter {
    private final int maxRequests;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    /**
     * Creates a rate limiter with the specified capacity and time window.
     *
     * @param maxRequestsPerWindow maximum number of requests allowed within the time window (must be > 0)
     * @param windowMillis time window duration in milliseconds (must be > 0)
     * @throws IllegalArgumentException if either parameter is not positive
     */
    public RateLimiter(int maxRequestsPerWindow, long windowMillis) {
        if (maxRequestsPerWindow <= 0) throw new IllegalArgumentException("maxRequestsPerWindow must be > 0");
        if (windowMillis <= 0) throw new IllegalArgumentException("windowMillis must be > 0");
        this.maxRequests = maxRequestsPerWindow;
        this.windowNanos = windowMillis * 1_000_000L;
    }

    /**
     * Acquires a permit to make a request, blocking if necessary.
     *
     * This method blocks the calling thread until a request slot becomes available
     * within the rate limit. Once a slot is acquired, the request is recorded
     * and the method returns immediately.
     *
     * The method uses efficient sleeping to avoid busy-waiting while respecting
     * the sliding window constraints.
     *
     * @throws RuntimeException if the thread is interrupted while waiting
     */
    public void acquire() {
        while (true) {
            long now = System.nanoTime();
            // Drop expired
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowNanos) {
                timestamps.removeFirst();
            }
            if (timestamps.size() < maxRequests) {
                timestamps.addLast(now);
                return;
            }
            // Sleep a tiny amount to avoid busy spin; compute minimal wait until next expiry
            long oldest = timestamps.peekFirst();
            long remainingNanos = windowNanos - (now - oldest);
            long sleepMillis = Math.max(1, remainingNanos / 1_000_000L);
            try {
                Thread.sleep(sleepMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while throttling", e);
            }
        }
    }
}

