package com.krisapps.incomeutility_v2.types.fiscal.cashew;

import com.krisapps.incomeutility_v2.exceptions.OperationNotPermittedException;
import com.krisapps.incomeutility_v2.types.OperationType;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class CashewTransaction extends Transaction {

    private String cashewTransactionId = null;
    private String cashewSourceAccount = null;
    private String cashewTargetAccount = null;

    public CashewTransaction(TransactionType type, double amount, UUID account, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, String cashewTransactionId) {
        super(type, amount, account, timestamp, category, customCategory, comment);
        this.cashewTransactionId = cashewTransactionId;
    }

    public CashewTransaction(TransactionType type, double amount, UUID from, UUID to, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, String cashewTransactionId) {
        super(type, amount, from, to, timestamp, category, customCategory, comment);
        this.cashewTransactionId = cashewTransactionId;
    }

    public CashewTransaction(String cashewTransactionId) {
        super();
        this.cashewTransactionId = cashewTransactionId;
    }

    public CashewTransaction() {
        super();
    }

    public String getCashewTransactionId() {
        return cashewTransactionId;
    }

    public void setCashewTransactionId(String cashewTransactionId) {
        this.cashewTransactionId = cashewTransactionId;
    }

    public String getCashewSourceAccount() {
        return cashewSourceAccount;
    }

    public void setCashewSourceAccount(String cashewSourceAccount) {
        this.cashewSourceAccount = cashewSourceAccount;
    }

    public String getCashewTargetAccount() {
        return cashewTargetAccount;
    }

    public void setCashewTargetAccount(String cashewTargetAccount) {
        this.cashewTargetAccount = cashewTargetAccount;
    }
}
