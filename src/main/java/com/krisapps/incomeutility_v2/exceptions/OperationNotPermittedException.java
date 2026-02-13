package com.krisapps.incomeutility_v2.exceptions;

import com.krisapps.incomeutility_v2.types.OperationType;

public class OperationNotPermittedException extends RuntimeException {
    private OperationType type;

    public OperationNotPermittedException(OperationType type, String message) {
        this.type = type;
        super(message);
    }

    public OperationType getOperation() {
        return type;
    }
}
