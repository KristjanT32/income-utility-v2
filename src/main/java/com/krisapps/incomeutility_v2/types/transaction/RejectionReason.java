package com.krisapps.incomeutility_v2.types.transaction;

public enum RejectionReason {
    SOURCE_NOT_FOUND("The source account for this operation couldn't be found."),
    TARGET_NOT_FOUND("The target account for this operation couldn't be found."),
    INSUFFICIENT_BALANCE("There is not enough funds to complete the operation."),
    INVALID_AMOUNT("The specified amount for the transaction is invalid.");

    private final String description;

    RejectionReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
