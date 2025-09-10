package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CwlClan {
    private String tag;
    private String name;

    public String getTag() { return tag; }
    public String getName() { return name; }
}

