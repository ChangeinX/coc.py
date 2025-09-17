package com.clanboards.util;

/**
 * Utility methods for handling and normalizing Clash of Clans player and clan tags.
 *
 * This utility class provides methods to clean, correct, and format tags according
 * to the game's standards. Tags are unique identifiers that may be entered by users
 * in various formats and need to be normalized for API consumption.
 *
 * All methods are static and thread-safe.
 *
 * @see CocClient#getClan(String)
 * @see CocClient#getPlayer(String)
 */
public final class TagUtil {
    private TagUtil() {}

    /**
     * Corrects a potentially malformed tag to standard in-game format.
     *
     * Applies the following transformations in order:
     * <ul>
     * <li>Trims leading and trailing whitespace</li>
     * <li>Converts all letters to uppercase</li>
     * <li>Replaces letter 'O' with digit '0' (common user error)</li>
     * <li>Removes all characters except A-Z and 0-9</li>
     * <li>Ensures the tag starts with '#'</li>
     * </ul>
     *
     * Examples:
     * <pre>
     * correctTag(" 2pp ") → "#2PP"
     * correctTag("9ul2ljrj") → "#9UL2LJRJ"
     * correctTag("#2PO0") → "#2P00"
     * </pre>
     *
     * @param tag the input tag in any format
     * @return normalized tag with '#' prefix, or the original tag if null/empty
     */
    public static String correctTag(String tag) {
        if (tag == null || tag.isEmpty()) return tag;
        String cleaned = tag.toUpperCase().replace('O', '0').replaceAll("[^A-Z0-9]+", "");
        return "#" + cleaned;
    }

    /**
     * Encodes a tag for safe use in URL path segments.
     *
     * Converts the '#' character to its percent-encoded form '%23' to ensure
     * the tag can be safely included in URL paths without being interpreted
     * as a fragment identifier.
     *
     * @param tag the tag to encode (typically starting with '#')
     * @return URL-safe tag with '#' replaced by '%23'
     */
    public static String encodeForPath(String tag) {
        return tag.replace("#", "%23");
    }
}

