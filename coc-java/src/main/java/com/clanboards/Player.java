package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a Clash of Clans player with core profile information.
 *
 * This immutable data class contains essential player properties including
 * identification, display information, and progression metrics. Additional
 * player data may be available through extended API endpoints.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#getPlayer(String)
 * @see CocClient#verifyPlayerToken(String, String)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
    private String tag;
    private String name;
    private int townHallLevel;

    /**
     * Returns the player's unique identifier tag.
     *
     * Player tags are unique identifiers in the format "#XXXXXXXX" where X
     * represents alphanumeric characters. Tags are case-insensitive and
     * globally unique across all players.
     *
     * @return player tag (e.g., "#2PP", "#9UL2LJRJ")
     */
    public String getTag() { return tag; }
    /**
     * Returns the player's display name.
     *
     * Player names are chosen by the player and may contain spaces,
     * special characters, and Unicode text. Names are not guaranteed
     * to be unique and can be changed by the player.
     *
     * @return player display name
     */
    public String getName() { return name; }
    /**
     * Returns the player's town hall level.
     *
     * Town hall level represents the player's main progression milestone
     * in the home village. Higher levels unlock new buildings, troops,
     * and game features. Current maximum is typically 16+.
     *
     * @return town hall level (1-16+)
     */
    public int getTownHallLevel() { return townHallLevel; }
}

