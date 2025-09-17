package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

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

    public String getTag() { return tag; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public int getExpLevel() { return expLevel; }
    public int getTrophies() { return trophies; }
    public int getBuilderBaseTrophies() { return builderBaseTrophies; }

    public JsonNode getRawJson() { return rawJson; }

    void attachRawJson(JsonNode rawJson) {
        this.rawJson = rawJson;
    }
}
