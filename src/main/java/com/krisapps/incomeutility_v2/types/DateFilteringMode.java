package com.krisapps.incomeutility_v2.types;

public enum DateFilteringMode {
    NONE("All transactions (no filtering)"),
    RANGE("All transactions between (inclusive)"),
    ALL_BEFORE("All transactions before (inclusive)"),
    ALL_AFTER("All transactions after (inclusive)"),
    ALL_ON("All transactions on");
    private final String displayName;

    DateFilteringMode(String displayName) {
        this.displayName = displayName;
    }

    public static DateFilteringMode ofDisplayName(String displayName) {
        for (DateFilteringMode mode : values()) {
            if (mode.getDisplayName().equals(displayName)) {
                return mode;
            }
        }
        return NONE;
    }

    public String getDisplayName() {
        return displayName;
    }
}
