package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a geographical location for rankings and leaderboards.
 *
 * Locations include both specific countries and the global location.
 * They are used to filter rankings and determine regional leaderboards
 * for clans and players across different game modes.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#searchLocations(Integer)
 * @see CocClient#getLocation(int)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Location {
    private int id;
    private String name;
    private Boolean isCountry;
    private String countryCode;

    /**
     * Returns the location's unique identifier.
     *
     * Location IDs are consistent across API calls and can be used
     * to retrieve location-specific rankings and data.
     *
     * @return location identifier
     */
    public int getId() { return id; }
    /**
     * Returns the location's display name.
     *
     * Location names are typically country names or "Global" for
     * worldwide rankings.
     *
     * @return location display name (e.g., "United States", "Global")
     */
    public String getName() { return name; }
    /**
     * Returns whether this location represents a specific country.
     *
     * False indicates a regional grouping or the global location.
     * True indicates a specific country with associated country code.
     *
     * @return true if this is a country-specific location
     */
    public Boolean getIsCountry() { return isCountry; }
    /**
     * Returns the ISO country code for this location.
     *
     * Only available for country-specific locations. Returns null
     * for non-country locations such as "Global".
     *
     * @return ISO country code (e.g., "US", "GB") or null
     */
    public String getCountryCode() { return countryCode; }
}

