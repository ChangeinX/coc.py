package com.clanboards.throttle;

import java.util.ArrayDeque;
import java.util.Deque;

/** Simple sliding-window rate limiter allowing N requests per given time window. */
public final class RateLimiter {
    private final int maxRequests;
    private final long windowNanos;
    private final Deque<Long> timestamps = new ArrayDeque<>();

    public RateLimiter(int maxRequestsPerWindow, long windowMillis) {
        if (maxRequestsPerWindow <= 0) throw new IllegalArgumentException("maxRequestsPerWindow must be > 0");
        if (windowMillis <= 0) throw new IllegalArgumentException("windowMillis must be > 0");
        this.maxRequests = maxRequestsPerWindow;
        this.windowNanos = windowMillis * 1_000_000L;
    }

    /** Blocks until a slot is available, then records this request. */
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

