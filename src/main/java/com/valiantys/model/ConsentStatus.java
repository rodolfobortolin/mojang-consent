package com.valiantys.model;

/**
 * Represents the user's consent status.
 */
public class ConsentStatus {
    // Fields
    private String username;
    private boolean hasConsented;
    private long consentDate;

    /**
     * Default constructor.
     */
    public ConsentStatus() {
        // By default, user has not consented, and date is zero (epoch).
        this.hasConsented = false;
        this.consentDate = 0L;
    }

    /**
     * Convenience constructor.
     */
    public ConsentStatus(String username, boolean hasConsented, long consentDate) {
        this();
        this.username = username;
        this.hasConsented = hasConsented;
        this.consentDate = consentDate;
    }

    // Getters and setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isHasConsented() {
        return hasConsented;
    }

    public void setHasConsented(boolean hasConsented) {
        this.hasConsented = hasConsented;
    }

    public long getConsentDate() {
        return consentDate;
    }

    public void setConsentDate(long consentDate) {
        this.consentDate = consentDate;
    }
}
