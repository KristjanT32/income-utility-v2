package com.krisapps.incomeutility_v2.exceptions;

import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;

public class OperationNotPermittedException extends RuntimeException {
    public OperationNotPermittedException(Account source, Transaction.Type transactionType, String message) {
        super(String.format("Operation '%s' is not allowed on '%s': %s", source.getName(), transactionType.getDisplayName(), message));
    }
}
