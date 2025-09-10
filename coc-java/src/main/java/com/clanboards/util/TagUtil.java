package com.clanboards.util;

/** Utility methods for handling Clash of Clans tags. */
public final class TagUtil {
    private TagUtil() {}

    /**
     * Attempt to correct a malformed tag to in-game format.
     * - Trims whitespace
     * - Uppercases letters
     * - Replaces letter 'O' with zero '0'
     * - Removes all characters not A-Z or 0-9
     * - Ensures leading '#'
     */
    public static String correctTag(String tag) {
        if (tag == null || tag.isEmpty()) return tag;
        String cleaned = tag.toUpperCase().replace('O', '0').replaceAll("[^A-Z0-9]+", "");
        return "#" + cleaned;
    }

    /** Encode a tag for use in a URL path segment (percent-encode '#'). */
    public static String encodeForPath(String tag) {
        return tag.replace("#", "%23");
    }
}

