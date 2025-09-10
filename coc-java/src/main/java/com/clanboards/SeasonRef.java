package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SeasonRef {
    private String id; // e.g., "2025-09"
    public String getId() { return id; }
}

