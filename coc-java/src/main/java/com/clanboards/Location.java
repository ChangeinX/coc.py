package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    private int id;
    private String name;
    private Boolean isCountry;
    private String countryCode;

    public int getId() { return id; }
    public String getName() { return name; }
    public Boolean getIsCountry() { return isCountry; }
    public String getCountryCode() { return countryCode; }
}

