package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Minimal Clan model for initial feature parity with get_clan. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Clan {
    private String tag;
    private String name;

    @JsonProperty("clanLevel")
    private int level;

    @JsonProperty("members")
    private int memberCount;

    public String getTag() { return tag; }
    public String getName() { return name; }
    public int getLevel() { return level; }
    public int getMemberCount() { return memberCount; }
}

