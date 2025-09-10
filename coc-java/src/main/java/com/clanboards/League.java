package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class League {
    private int id;
    private String name;

    public int getId() { return id; }
    public String getName() { return name; }
}

