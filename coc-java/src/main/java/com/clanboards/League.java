package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a competitive league in Clash of Clans.
 *
 * Leagues are competitive tiers that players and clans can achieve
 * based on trophy counts. Different game modes have separate league
 * systems (main village, builder base, clan capital, war leagues).
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#searchLeagues(Integer)
 * @see CocClient#getLeague(int)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class League {
    private int id;
    private String name;

    /**
     * Returns the league's unique identifier.
     *
     * League IDs are consistent across API calls and can be used
     * to retrieve specific league information and seasonal data.
     *
     * @return league identifier
     */
    public int getId() { return id; }
    /**
     * Returns the league's display name.
     *
     * League names indicate the competitive tier and are typically
     * descriptive (e.g., "Bronze League III", "Champion League I").
     *
     * @return league display name
     */
    public String getName() { return name; }
}

