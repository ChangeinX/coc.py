package com.clanboards.wars;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a clan war with state information and participating sides.
 *
 * This immutable data class contains essential war information including
 * the current state, team configuration, and details about both the clan
 * and opponent sides. Used for both regular clan wars and CWL wars.
 *
 * War states follow the game progression:
 * <ul>
 * <li>"notInWar" - Clan is not currently in a war</li>
 * <li>"preparation" - War day preparation phase</li>
 * <li>"inWar" - Active war day with attacks ongoing</li>
 * <li>"warEnded" - War has concluded</li>
 * </ul>
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#getCurrentWar(String)
 * @see CocClient#getCwlWar(String)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClanWar {
    private String state; // notInWar, preparation, inWar, warEnded
    private int teamSize;
    private Integer attacksPerMember;
    private WarSide clan;
    private WarSide opponent;

    /**
     * Returns the current state of the war.
     *
     * @return war state ("notInWar", "preparation", "inWar", "warEnded")
     */
    public String getState() { return state; }
    /**
     * Returns the team size for this war.
     *
     * Team size determines how many players from each clan participate
     * in the war. Common sizes are 5, 10, 15, 20, 25, 30, 40, and 50.
     *
     * @return number of players per side
     */
    public int getTeamSize() { return teamSize; }
    /**
     * Returns the number of attacks each member can make.
     *
     * Typically 2 attacks per member for regular wars, but may vary
     * for special events or CWL wars.
     *
     * @return attacks allowed per member, or null if not specified
     */
    public Integer getAttacksPerMember() { return attacksPerMember; }
    /**
     * Returns information about the clan's side in the war.
     *
     * @return clan war side with statistics and member details
     */
    public WarSide getClan() { return clan; }
    /**
     * Returns information about the opponent's side in the war.
     *
     * @return opponent war side with statistics and member details
     */
    public WarSide getOpponent() { return opponent; }
}

