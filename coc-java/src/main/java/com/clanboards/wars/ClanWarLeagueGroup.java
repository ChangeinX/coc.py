package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Clan War League group with participating clans and round structure.
 *
 * This class contains information about a CWL group including the current state,
 * season identifier, participating clans, and the round structure with war tags
 * for accessing individual league wars.
 *
 * The rounds structure is a list of lists, where each inner list contains
 * war tags for that round. Invalid war tags (like "#0") are automatically filtered out.
 *
 * Thread-safety: This class is not thread-safe due to mutable collections.
 *
 * @see CocClient#getClanWarLeagueGroup(String)
 * @see CocClient#getCwlWar(String)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanWarLeagueGroup {
    private String state;
    private String season;
    private List<List<String>> rounds = new ArrayList<>();
    private int numberOfRounds;
    private List<CwlClan> clans = new ArrayList<>();

    /**
     * Returns the current state of the CWL group.
     *
     * Common states include preparation phases and active war periods.
     *
     * @return CWL group state
     */
    public String getState() { return state; }
    /**
     * Returns the season identifier for this CWL group.
     *
     * Season identifiers typically follow a date format (e.g., "2023-11").
     *
     * @return CWL season identifier
     */
    public String getSeason() { return season; }
    /**
     * Returns the round structure with war tags for each round.
     *
     * Each inner list contains war tags that can be used with getCwlWar()
     * to retrieve detailed information about individual league wars.
     * Invalid war tags are automatically filtered out.
     *
     * @return list of rounds, each containing a list of war tags
     */
    public List<List<String>> getRounds() { return rounds; }
    /**
     * Returns the total number of rounds in this CWL group.
     *
     * @return number of rounds in the league
     */
    public int getNumberOfRounds() { return numberOfRounds; }
    /**
     * Returns the list of clans participating in this CWL group.
     *
     * @return list of participating clans with their league information
     */
    public List<CwlClan> getClans() { return clans; }

    /**
     * Sets the rounds structure, filtering out invalid war tags.
     *
     * This method automatically filters out rounds containing invalid
     * war tags such as "#0" which indicate placeholder entries.
     *
     * @param rounds list of rounds with war tags (may contain invalid entries)
     */
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

