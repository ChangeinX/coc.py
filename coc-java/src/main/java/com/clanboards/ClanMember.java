package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a member of a Clash of Clans clan with profile and role information.
 *
 * This immutable data class contains essential member properties including
 * identification, clan role, experience level, and trophy counts across
 * different game modes. Additional member data may be available through
 * the raw JSON attachment when enabled.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#getMembers(String, Integer, String, String)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanMember {
    private String tag;
    private String name;
    private String role;

    @JsonProperty("expLevel")
    private int expLevel;

    private int trophies;

    @JsonProperty("builderBaseTrophies")
    private int builderBaseTrophies;

    @JsonIgnore
    private transient JsonNode rawJson;

    /**
     * Returns the member's unique player tag.
     *
     * @return player tag (e.g., "#2PP", "#9UL2LJRJ")
     * @see Player#getTag()
     */
    public String getTag() { return tag; }
    /**
     * Returns the member's display name.
     *
     * @return player display name
     * @see Player#getName()
     */
    public String getName() { return name; }
    /**
     * Returns the member's role within the clan.
     *
     * Clan roles determine permissions for member management, war participation,
     * and clan settings. Common roles include "member", "elder", "coLeader", and "leader".
     *
     * @return clan role designation
     */
    public String getRole() { return role; }
    /**
     * Returns the member's experience level.
     *
     * Experience level represents overall player progression and is gained
     * through various in-game activities including donations, attacks, and challenges.
     *
     * @return player experience level
     */
    public int getExpLevel() { return expLevel; }
    /**
     * Returns the member's main village trophy count.
     *
     * Trophies are earned through multiplayer battles in the home village
     * and determine league placement and matchmaking.
     *
     * @return current trophy count for main village
     */
    public int getTrophies() { return trophies; }
    /**
     * Returns the member's builder base trophy count.
     *
     * Builder base trophies are earned through builder base battles
     * and are tracked separately from main village trophies.
     *
     * @return current builder base trophy count
     */
    public int getBuilderBaseTrophies() { return builderBaseTrophies; }

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
     * Attaches raw JSON data to this clan member instance.
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
