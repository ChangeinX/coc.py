package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanWarLeagueGroup {
    private String state;
    private String season;
    private List<List<String>> rounds = new ArrayList<>();
    private int numberOfRounds;
    private List<CwlClan> clans = new ArrayList<>();

    public String getState() { return state; }
    public String getSeason() { return season; }
    public List<List<String>> getRounds() { return rounds; }
    public int getNumberOfRounds() { return numberOfRounds; }
    public List<CwlClan> getClans() { return clans; }

    // Custom setter-like method to filter rounds during mapping if needed
    public void setRounds(List<List<String>> rounds) {
        this.rounds = new ArrayList<>();
        if (rounds != null) {
            for (List<String> r : rounds) {
                if (r != null && !r.isEmpty() && !"#0".equals(r.get(0))) {
                    this.rounds.add(r);
                }
            }
        }
    }

    public void setNumberOfRounds(int numberOfRounds) { this.numberOfRounds = numberOfRounds; }
    public void setState(String state) { this.state = state; }
    public void setSeason(String season) { this.season = season; }
    public void setClans(List<CwlClan> clans) { this.clans = clans; }
}

