package com.krisapps.incomeutility_v2.types;

public enum SearchMode {
    AND("Matches transactions which match ALL search criteria."),
    OR("Matches transactions which match SOME search criteria.");
    private final String description;

    SearchMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
