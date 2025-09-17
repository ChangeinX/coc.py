package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a Clash of Clans clan with essential information.
 *
 * This immutable data class contains core clan properties including identification,
 * descriptive information, and membership details. Additional clan data may be
 * available through the raw JSON attachment when enabled.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#getClan(String)
 * @see CocClient#searchClans(String, Integer)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Clan {
    private String tag;
    private String name;

    @JsonProperty("clanLevel")
    private int level;

    @JsonProperty("members")
    private int memberCount;

    @JsonIgnore
    private transient JsonNode rawJson;

    /**
     * Returns the clan's unique identifier tag.
     *
     * Clan tags are unique identifiers in the format "#XXXXXXXX" where X
     * represents alphanumeric characters. Tags are case-insensitive.
     *
     * @return clan tag (e.g., "#2PP", "#9UL2LJRJ")
     */
    public String getTag() { return tag; }
    /**
     * Returns the clan's display name.
     *
     * Clan names are player-chosen identifiers that may contain spaces,
     * special characters, and Unicode text. Names are not guaranteed to be unique.
     *
     * @return clan display name
     */
    public String getName() { return name; }
    /**
     * Returns the clan's experience level.
     *
     * Clan level increases through member donations and activities.
     * Higher levels unlock perks and bonuses for clan members.
     *
     * @return clan level (typically 1-20+)
     */
    public int getLevel() { return level; }
    /**
     * Returns the current number of members in the clan.
     *
     * Member count reflects active clan participants and is limited
     * by the maximum clan capacity (typically 50 members).
     *
     * @return current member count (0-50)
     */
    public int getMemberCount() { return memberCount; }

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
     * Attaches raw JSON data to this clan instance.
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
