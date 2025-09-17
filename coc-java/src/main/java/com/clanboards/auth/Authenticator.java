package com.clanboards.auth;

import java.util.List;

/**
 * Strategy interface for obtaining Clash of Clans API tokens from developer credentials.
 *
 * This interface abstracts the process of authenticating with the Clash of Clans
 * developer portal and obtaining API tokens. Implementations may use different
 * authentication methods or token sources.
 *
 * All methods should be thread-safe for concurrent use.
 *
 * @see DevSiteAuthenticator
 * @see CocClient#login(String, String)
 */
public interface Authenticator {
    /**
     * Obtains a single API token using the provided credentials.
     *
     * This convenience method is equivalent to calling obtainTokens(email, password, 1)
     * and returning the first token.
     *
     * @param email developer account email
     * @param password developer account password
     * @return API token for use with the Clash of Clans API, or null if authentication fails
     * @throws RuntimeException if authentication encounters an error
     */
    default String obtainToken(String email, String password) {
        List<String> tokens = obtainTokens(email, password, 1);
        return tokens.isEmpty() ? null : tokens.get(0);
    }

    /**
     * Obtains multiple API tokens using the provided credentials.
     *
     * Implementations should authenticate with the developer portal and return
     * the requested number of valid API tokens. Tokens may be newly created
     * or reused from existing valid keys.
     *
     * @param email developer account email
     * @param password developer account password
     * @param count number of tokens to obtain (must be > 0)
     * @return list of API tokens (may be fewer than requested if limits are reached)
     * @throws RuntimeException if authentication fails or encounters an error
     */
    List<String> obtainTokens(String email, String password, int count);
}
