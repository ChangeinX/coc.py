package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents one side (clan or opponent) in a clan war.
 *
 * This immutable data class contains war performance statistics for one
 * participating clan, including identification, total stars earned, and
 * destruction percentage achieved across all attacks.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see ClanWar
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WarSide {
    private String tag;
    private String name;
    private int stars;
    @JsonProperty("destructionPercentage")
    private double destructionPercentage;

    /**
     * Returns the clan tag for this war side.
     *
     * @return clan tag (e.g., "#2PP", "#9UL2LJRJ")
     */
    public String getTag() { return tag; }
    /**
     * Returns the clan name for this war side.
     *
     * @return clan display name
     */
    public String getName() { return name; }
    /**
     * Returns the total stars earned by this side.
     *
     * Stars are earned through successful attacks, with a maximum of
     * 3 stars per attack. Total stars determine war victory.
     *
     * @return total stars earned (0 to teamSize * 3)
     */
    public int getStars() { return stars; }
    /**
     * Returns the total destruction percentage achieved by this side.
     *
     * Destruction percentage represents the cumulative damage dealt
     * across all attacks. Used as a tiebreaker when star counts are equal.
     *
     * @return destruction percentage (0.0 to 100.0 * teamSize)
     */
    public double getDestructionPercentage() { return destructionPercentage; }
}

