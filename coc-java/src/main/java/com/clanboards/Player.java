package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Player {
    private String tag;
    private String name;
    private int townHallLevel;

    public String getTag() { return tag; }
    public String getName() { return name; }
    public int getTownHallLevel() { return townHallLevel; }
}

