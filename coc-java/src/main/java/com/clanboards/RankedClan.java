package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a clan with ranking information from leaderboards.
 *
 * This immutable data class extends basic clan information with competitive
 * ranking data including position, points across different game modes, and
 * historical rank changes. Used in location-based and global rankings.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#getLocationClanRankings(int, Integer, String, String)
 * @see CocClient#getLocationBuilderBaseClanRankings(int, Integer, String, String)
 * @see CocClient#getLocationCapitalClanRankings(int, Integer, String, String)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RankedClan {
    private String tag;
    private String name;
    @JsonProperty("clanLevel")
    private int level;
    @JsonProperty("members")
    private int memberCount;

    @JsonProperty("clanPoints")
    private Integer points; // home village
    @JsonProperty("clanBuilderBasePoints")
    private Integer builderBasePoints;
    @JsonProperty("clanCapitalPoints")
    private Integer capitalPoints;

    private int rank;
    @JsonProperty("previousRank")
    private int previousRank;

    @JsonIgnore
    private transient JsonNode rawJson;

    /**
     * Returns the clan's unique identifier tag.
     *
     * @return clan tag (e.g., "#2PP", "#9UL2LJRJ")
     * @see Clan#getTag()
     */
    public String getTag() { return tag; }
    /**
     * Returns the clan's display name.
     *
     * @return clan display name
     * @see Clan#getName()
     */
    public String getName() { return name; }
    /**
     * Returns the clan's experience level.
     *
     * @return clan level (typically 1-20+)
     * @see Clan#getLevel()
     */
    public int getLevel() { return level; }
    /**
     * Returns the current number of members in the clan.
     *
     * @return current member count (0-50)
     * @see Clan#getMemberCount()
     */
    public int getMemberCount() { return memberCount; }
    /**
     * Returns the clan's main village trophy points.
     *
     * Main village points are the primary ranking metric for clan leaderboards
     * and represent the sum of all members' trophy counts.
     *
     * @return clan trophy points or null if not applicable for this ranking type
     */
    public Integer getPoints() { return points; }
    /**
     * Returns the clan's builder base trophy points.
     *
     * Builder base points represent the sum of all members' builder base
     * trophy counts and are used for builder base clan rankings.
     *
     * @return clan builder base points or null if not applicable
     */
    public Integer getBuilderBasePoints() { return builderBasePoints; }
    /**
     * Returns the clan's capital trophy points.
     *
     * Capital points are earned through clan capital raids and represent
     * the clan's performance in cooperative capital battles.
     *
     * @return clan capital points or null if not applicable
     */
    public Integer getCapitalPoints() { return capitalPoints; }
    /**
     * Returns the clan's current ranking position.
     *
     * Rank represents the clan's position in the leaderboard where
     * 1 is the highest-ranked clan. Rankings are specific to the
     * location and ranking type (main village, builder base, or capital).
     *
     * @return current ranking position (1-based)
     */
    public int getRank() { return rank; }
    /**
     * Returns the clan's previous ranking position.
     *
     * Shows the clan's rank from the previous ranking period,
     * allowing comparison of rank changes over time.
     *
     * @return previous ranking position (1-based)
     */
    public int getPreviousRank() { return previousRank; }

    /**
     * Returns the raw JSON data from the API response.
     *
     * Available only when raw JSON attachment is enabled in CocClient.
     * Contains the complete API response data including fields not
     * mapped to specific properties.
     *
     * @return raw JSON node or null if raw attachment is disabled
     * @see CocClient#isRawAttributeEnabled()
     */
    public JsonNode getRawJson() { return rawJson; }

    /**
     * Attaches raw JSON data to this ranked clan instance.
     *
     * Package-private method used internally by CocClient when raw JSON
     * attachment is enabled. Not intended for external use.
     *
     * @param rawJson the raw JSON node from API response
     */
    void attachRawJson(JsonNode rawJson) {
        this.rawJson = rawJson;
    }
}
