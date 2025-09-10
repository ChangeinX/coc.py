package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanWar {
    private String state; // notInWar, preparation, inWar, warEnded
    private int teamSize;
    private Integer attacksPerMember;
    private WarSide clan;
    private WarSide opponent;

    public String getState() { return state; }
    public int getTeamSize() { return teamSize; }
    public Integer getAttacksPerMember() { return attacksPerMember; }
    public WarSide getClan() { return clan; }
    public WarSide getOpponent() { return opponent; }
}

