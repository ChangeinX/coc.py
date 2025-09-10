package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GoldPassSeason {
    private String startTime; // ISO 8601
    private String endTime;   // ISO 8601

    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
}

