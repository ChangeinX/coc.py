package com.clanboards;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a label for categorizing clans or players.
 *
 * Labels are metadata tags used to categorize and filter clans or players
 * based on various criteria such as clan type, requirements, language,
 * or other organizational attributes.
 *
 * Thread-safety: This class is immutable and thread-safe.
 *
 * @see CocClient#getClanLabels()
 * @see CocClient#getPlayerLabels()
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Label {
    private long id;
    private String name;

    /**
     * Returns the label's unique identifier.
     *
     * Label IDs are consistent across API calls and can be used
     * for filtering and categorization purposes.
     *
     * @return label identifier
     */
    public long getId() { return id; }
    /**
     * Returns the label's display name.
     *
     * Label names describe the categorization criteria and are
     * typically descriptive (e.g., "Competitive", "Casual", "International").
     *
     * @return label display name
     */
    public String getName() { return name; }
}

