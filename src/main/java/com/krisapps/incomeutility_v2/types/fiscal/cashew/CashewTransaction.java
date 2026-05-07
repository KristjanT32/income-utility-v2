package com.krisapps.incomeutility_v2.types.fiscal.cashew;

import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;

import java.time.LocalDateTime;
import java.util.UUID;

public class CashewTransaction extends Transaction {

    private String cashewTransactionId = null;
    private String cashewSourceAccount = null;
    private String cashewTargetAccount = null;

    public CashewTransaction(TransactionType type, double amount, UUID account, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, String cashewTransactionId) {
        super(type, amount, account, timestamp, category, customCategory, comment, null);
        this.cashewTransactionId = cashewTransactionId;
    }

    public CashewTransaction(TransactionType type, double amount, UUID from, UUID to, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, String cashewTransactionId) {
        super(type, amount, from, to, timestamp, category, customCategory, comment, null);
        this.cashewTransactionId = cashewTransactionId;
    }

    public CashewTransaction(TransactionType type, double amount, UUID from, UUID to, LocalDateTime timestamp, TransactionCategory category, String customCategory, String comment, UUID id, String cashewTransactionId, String cashewSourceAccount, String cashewTargetAccount) {
        super(type, amount, from, to, timestamp, category, customCategory, comment, id);
        this.cashewTransactionId = cashewTransactionId;
        this.cashewSourceAccount = cashewSourceAccount;
        this.cashewTargetAccount = cashewTargetAccount;
    }

    public CashewTransaction(String cashewTransactionId) {
        super();
        this.cashewTransactionId = cashewTransactionId;
    }

    public static CashewTransaction of(Transaction t, String cashewTransactionId, String cashewSourceAccount, String cashewTargetAccount) {
        return new CashewTransaction(
                t.getType(),
                t.getAmount(),
                t.getSourceAccountId(),
                t.getTargetAccountId(),
                t.getTimestamp(),
                t.getCategory(),
                t.getCustomCategory(),
                t.getComment(),
                t.getId(),
                cashewTransactionId,
                cashewSourceAccount,
                cashewTargetAccount
        );
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

    @Override
    public CashewTransaction copy() {
        return new CashewTransaction(
            this.getType(),
            this.getAmount(),
            this.getSourceAccountId(),
            this.getTargetAccountId(),
            this.getTimestamp(),
            this.getCategory(),
            this.getCustomCategory(),
            this.getComment(),
            this.getId(),
            this.cashewTransactionId,
            this.cashewSourceAccount,
            this.cashewTargetAccount
        );
    }
}
