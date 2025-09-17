package com.clanboards.token;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe round-robin token provider for API key rotation.
 *
 * This class manages multiple API tokens and distributes requests across them
 * using a round-robin algorithm. This helps distribute load and avoid hitting
 * per-token rate limits when using multiple API keys.
 *
 * The rotation is atomic and thread-safe, making it suitable for concurrent
 * use in multi-threaded applications.
 *
 * Thread-safety: This class is thread-safe and designed for concurrent access.
 *
 * @see CocClient#loginWithTokens(java.util.List, int)
 */
public final class TokenRotator {
    private final List<String> tokens;
    private final AtomicInteger idx = new AtomicInteger(0);

    /**
     * Creates a token rotator with the specified list of API tokens.
     *
     * The tokens are copied to an immutable list to prevent external modification.
     * At least one token must be provided.
     *
     * @param tokens list of API tokens to rotate through (must not be null or empty)
     * @throws IllegalArgumentException if tokens is null or empty
     */
    public TokenRotator(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) throw new IllegalArgumentException("tokens must not be empty");
        this.tokens = List.copyOf(tokens);
    }

    /**
     * Returns the next token in the rotation sequence.
     *
     * Uses atomic operations to ensure thread-safe rotation across all available
     * tokens. The rotation wraps around when all tokens have been used.
     *
     * @return next API token in the round-robin sequence
     */
    public String next() {
        int i = Math.floorMod(idx.getAndIncrement(), tokens.size());
        return tokens.get(i);
    }

    /**
     * Returns an immutable list of all configured tokens.
     *
     * This method is primarily intended for diagnostic purposes
     * and configuration validation.
     *
     * @return immutable list of all API tokens
     */
    public List<String> getAll() { return tokens; }
}

