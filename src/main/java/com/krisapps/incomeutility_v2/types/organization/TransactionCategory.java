package com.krisapps.incomeutility_v2.types.organization;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

public enum TransactionCategory {
    GROCERIES,
    SNACKS,
    RENT,
    ELECTRICITY,
    WATER,
    HEATING,
    SUBSCRIPTIONS,
    ENTERTAINMENT,
    REST_AND_RELAXATION,
    LOAN,
    WITHDRAWAL,
    DEPOSIT,
    TRANSFER,
    MISCELLANEOUS,
    CUSTOM,
    UNKNOWN
    ;

    public static TransactionCategory of(Transaction.Type type) {
        return switch (type) {
            case DEPOSIT -> DEPOSIT;
            case WITHDRAWAL -> WITHDRAWAL;
            case TRANSFER -> TRANSFER;
            case null -> UNKNOWN;
        };
    }
}
