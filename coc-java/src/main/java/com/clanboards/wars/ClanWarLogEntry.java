package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanWarLogEntry {
    private String result; // win/lose/tie
    private int teamSize;
    private Integer attacksPerMember;
    private String endTime;
    private WarSide clan;
    private WarSide opponent;

    public String getResult() { return result; }
    public int getTeamSize() { return teamSize; }
    public Integer getAttacksPerMember() { return attacksPerMember; }
    public String getEndTime() { return endTime; }
    public WarSide getClan() { return clan; }
    public WarSide getOpponent() { return opponent; }
}

