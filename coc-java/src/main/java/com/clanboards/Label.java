package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Label {
    private long id;
    private String name;

    public long getId() { return id; }
    public String getName() { return name; }
}

