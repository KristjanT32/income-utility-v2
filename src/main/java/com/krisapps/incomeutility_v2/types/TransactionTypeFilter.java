package com.krisapps.incomeutility_v2.types;

import com.krisapps.incomeutility_v2.types.transaction.TransactionType;

public enum TransactionTypeFilter {
    DEPOSIT("Deposits"),
    WITHDRAWAL("Withdrawals"),
    TRANSFER("Transfers"),
    ANY("All types of transactions");

    private final String displayName;

    TransactionTypeFilter(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TransactionType asType() {
        return switch (this) {
            case DEPOSIT -> TransactionType.DEPOSIT;
            case WITHDRAWAL -> TransactionType.WITHDRAWAL;
            case TRANSFER -> TransactionType.TRANSFER;
            case ANY -> null;
        };
    }
}
