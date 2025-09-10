package com.clanboards.auth;

import java.util.List;

/** Strategy interface to obtain API tokens from email/password. */
public interface Authenticator {
    /** Returns a single bearer token for the Clash of Clans API. */
    default String obtainToken(String email, String password) {
        List<String> tokens = obtainTokens(email, password, 1);
        return tokens.isEmpty() ? null : tokens.get(0);
    }

    /** Returns one or more bearer tokens (API keys) for the Clash of Clans API. */
    List<String> obtainTokens(String email, String password, int count);
}
