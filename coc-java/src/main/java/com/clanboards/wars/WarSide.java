package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WarSide {
    private String tag;
    private String name;
    private int stars;
    @JsonProperty("destructionPercentage")
    private double destructionPercentage;

    public String getTag() { return tag; }
    public String getName() { return name; }
    public int getStars() { return stars; }
    public double getDestructionPercentage() { return destructionPercentage; }
}

