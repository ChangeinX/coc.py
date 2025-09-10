package com.clanboards.token;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/** Round-robin token provider. */
public final class TokenRotator {
    private final List<String> tokens;
    private final AtomicInteger idx = new AtomicInteger(0);

    public TokenRotator(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) throw new IllegalArgumentException("tokens must not be empty");
        this.tokens = List.copyOf(tokens);
    }

    public String next() {
        int i = Math.floorMod(idx.getAndIncrement(), tokens.size());
        return tokens.get(i);
    }

    public List<String> getAll() { return tokens; }
}

