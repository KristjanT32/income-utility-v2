package com.krisapps.incomeutility_v2.subutilities;

public enum SubUtilityType {
    PRICER("Pricer"),
    MONEY_IN_MONEY_OUT("Money In, Money Out"),
    ALL("All utilities")
    ;

    private String displayName;
    private SubUtilityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return this.displayName;
    }
}
