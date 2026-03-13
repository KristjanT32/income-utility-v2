package com.krisapps.incomeutility_v2.exceptions;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.transaction.RejectionReason;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;

public class TransactionNotPermittedException extends RuntimeException {
    private final TransactionType type;
    private final RejectionReason reason;
    private final Account affectedAccount;

    public TransactionNotPermittedException(TransactionType type, RejectionReason reason, Account account, String message) {
        this.type = type;
        this.reason = reason;
        this.affectedAccount = account;
        super(message);
    }

    public TransactionType getType() {
        return type;
    }

    public RejectionReason getReason() {
        return reason;
    }

    public Account getAffectedAccount() {
        return affectedAccount;
    }
}
