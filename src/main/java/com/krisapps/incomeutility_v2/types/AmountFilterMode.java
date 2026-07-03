package com.krisapps.incomeutility_v2.types;

public enum AmountFilterMode {

    NONE("Any amount (*)"),
    LESS_THAN("Less than (<)"),
    LESS_THAN_OR_EQUAL_TO("Less than or equal to (<=)"),
    EQUAL_TO("Equal to (=)"),
    BETWEEN("Between (both sides inclusive)"),
    GREATER_THAN("Greater than (>)"),
    GREATER_THAN_OR_EQUAL_TO("Greater than (>=)");
    private String displayName;

    AmountFilterMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
