/**
 * Data models for clan wars, war logs, and Clan War League functionality.
 *
 * <p>This package contains immutable data models representing various aspects
 * of clan warfare in Clash of Clans, including regular clan wars, war logs,
 * and the Clan War League (CWL) competitive system.
 *
 * <h2>War Types</h2>
 * <ul>
 * <li><strong>Regular Wars</strong> - Standard clan vs clan battles</li>
 * <li><strong>Clan War League</strong> - Seasonal tournament-style competition</li>
 * </ul>
 *
 * <h2>Core Models</h2>
 * <ul>
 * <li>{@link com.clanboards.wars.ClanWar} - Active or completed war information</li>
 * <li>{@link com.clanboards.wars.WarSide} - One side's performance in a war</li>
 * <li>{@link com.clanboards.wars.ClanWarLogEntry} - Historical war record</li>
 * <li>{@link com.clanboards.wars.ClanWarLeagueGroup} - CWL group structure and rounds</li>
 * <li>{@link com.clanboards.wars.CwlClan} - Clan information within CWL context</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * <p>All data models in this package are immutable and thread-safe.
 *
 * @see com.clanboards.CocClient#getCurrentWar(String)
 * @see com.clanboards.CocClient#getWarLog(String, Integer)
 * @see com.clanboards.CocClient#getClanWarLeagueGroup(String)
 */
package com.clanboards.wars;

