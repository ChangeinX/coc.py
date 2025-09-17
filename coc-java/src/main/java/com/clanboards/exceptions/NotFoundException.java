package com.clanboards.exceptions;

/**
 * Exception thrown when a requested resource cannot be found.
 *
 * This exception is typically thrown in response to HTTP 404 errors from the
 * Clash of Clans API, indicating that the requested clan, player, war, or other
 * resource does not exist or is not accessible.
 *
 * Common scenarios:
 * <ul>
 * <li>Clan tag does not exist or is malformed</li>
 * <li>Player tag does not exist or is malformed</li>
 * <li>War tag is invalid or war has ended</li>
 * <li>Location or league ID is invalid</li>
 * </ul>
 *
 * @see CocClient
 */
public class NotFoundException extends RuntimeException {
    /**
     * Creates a new NotFoundException with the specified detail message.
     *
     * @param message detail message describing what resource was not found
     */
    public NotFoundException(String message) { super(message); }
}

