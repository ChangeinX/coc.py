/**
 * Authentication strategies for obtaining Clash of Clans API tokens.
 *
 * <p>This package provides authentication implementations that handle the
 * process of obtaining valid API tokens from the Clash of Clans developer
 * portal. The authentication process includes credential validation, IP
 * address detection, and API key lifecycle management.
 *
 * <h2>Core Components</h2>
 * <ul>
 * <li>{@link com.clanboards.auth.Authenticator} - Strategy interface for token acquisition</li>
 * <li>{@link com.clanboards.auth.DevSiteAuthenticator} - Production implementation using developer portal</li>
 * </ul>
 *
 * <h2>Authentication Workflow</h2>
 * <p>The {@link com.clanboards.auth.DevSiteAuthenticator} performs:
 * <ol>
 * <li>Login to developer.clashofclans.com with email/password</li>
 * <li>IP address detection from temporary token</li>
 * <li>Search for existing compatible API keys</li>
 * <li>Cleanup of mismatched keys when necessary</li>
 * <li>Creation of new keys as needed</li>
 * </ol>
 *
 * <h2>Thread Safety</h2>
 * <p>All authentication implementations are thread-safe and can be used
 * concurrently across multiple threads.
 *
 * @see com.clanboards.auth.Authenticator
 * @see com.clanboards.auth.DevSiteAuthenticator
 */
package com.clanboards.auth;

