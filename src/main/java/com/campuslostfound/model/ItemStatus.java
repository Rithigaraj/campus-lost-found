package com.campuslostfound.model;

/**
 * Lifecycle status of a lost or found report.
 */
public enum ItemStatus {
    OPEN,
    MATCHED,
    RETURNED,
    CLOSED;

    /**
     * Parses user input into an ItemStatus, accepting the name
     * case-insensitively. Returns null when the input is invalid.
     */
    public static ItemStatus fromString(String value) {
        if (value == null) {
            return null;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
