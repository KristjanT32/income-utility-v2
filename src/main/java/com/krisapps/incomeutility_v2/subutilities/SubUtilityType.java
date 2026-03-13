package com.krisapps.incomeutility_v2.subutilities;

public enum SubUtilityType {
    PRICER("Pricer"),
    MONEY_IN_MONEY_OUT("Money In, Money Out"),
    ALL("All utilities");

    private final String displayName;

    SubUtilityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
