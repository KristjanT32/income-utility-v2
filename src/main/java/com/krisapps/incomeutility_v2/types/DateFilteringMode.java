package com.krisapps.incomeutility_v2.types;

public enum DateFilteringMode {
    /**
     * No filtering
     */
    NONE("All transactions (no filtering)"),

    /**
     * Everything within a range (both sides inclusive)
     */
    RANGE("All transactions between (inclusive)"),

    /**
     * Everything before the supplied date (inclusive)
     */
    ALL_BEFORE("All transactions before (inclusive)"),

    /**
     * Everything after the supplied date (inclusive)
     */
    ALL_AFTER("All transactions after (inclusive)"),

    /**
     * Everything on the supplied date
     */
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
