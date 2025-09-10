package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RankedPlayer {
    private String tag;
    private String name;
    @JsonProperty("expLevel")
    private int expLevel;
    private Integer trophies; // home village
    @JsonProperty("builderBaseTrophies")
    private Integer builderBaseTrophies;

    private int rank;
    @JsonProperty("previousRank")
    private int previousRank;

    public String getTag() { return tag; }
    public String getName() { return name; }
    public int getExpLevel() { return expLevel; }
    public Integer getTrophies() { return trophies; }
    public Integer getBuilderBaseTrophies() { return builderBaseTrophies; }
    public int getRank() { return rank; }
    public int getPreviousRank() { return previousRank; }
}

