package com.clanboards.exceptions;

/**
 * Exception thrown when attempting to access private war information.
 *
 * This exception is thrown in response to HTTP 403 errors from war-related
 * endpoints, indicating that the clan's war log or war information is set
 * to private and cannot be accessed through the public API.
 *
 * Common scenarios:
 * <ul>
 * <li>Clan has set their war log to private in game settings</li>
 * <li>Current war information is restricted</li>
 * <li>Clan War League data is not publicly accessible</li>
 * </ul>
 *
 * Applications should handle this exception gracefully and inform users
 * that the requested war data is not publicly available.
 *
 * @see CocClient#getWarLog(String, Integer)
 * @see CocClient#getCurrentWar(String)
 * @see CocClient#getClanWarLeagueGroup(String)
 */
public class PrivateWarLogException extends RuntimeException {
    /**
     * Creates a new PrivateWarLogException with the specified detail message.
     *
     * @param message detail message describing the private war access attempt
     */
    public PrivateWarLogException(String message) { super(message); }
}

