package com.krisapps.incomeutility_v2.types.fiscal;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public class Transaction {
    public enum Type {
        MONEY_RECEIVED("Deposit"),
        MONEY_WITHDRAWN("Withdrawal"),
        MONEY_TRANSFERRED("Transfer"),
        ;

        private final String displayName;
        private Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public Transaction(Type type, double amount, UUID account, Date timestamp) {
        this.type = type;
        this.amount = amount;
        if (type == Type.MONEY_RECEIVED || type == Type.MONEY_WITHDRAWN) {
            this.targetAccountId = account;
        }
        this.id = java.util.UUID.randomUUID();
        this.timestamp = timestamp;
    }

    public Transaction(Type type, double amount, UUID from, UUID to, Date timestamp) {
        this.type = type;
        this.amount = amount;
        this.sourceAccountId = from;
        this.targetAccountId = to;
        this.id = java.util.UUID.randomUUID();
        this.timestamp = timestamp;
    }

    private Transaction.Type type;
    private double amount;
    private UUID sourceAccountId;
    private UUID targetAccountId;

    private Date timestamp;
    private final java.util.UUID id;

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public UUID getTargetAccountId() {
        return targetAccountId;
    }

    public UUID getId() {
        return id;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public void setTargetAccountId(UUID targetAccountId) {
        this.targetAccountId = targetAccountId;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRelated(Account account) {
        return this.sourceAccountId == account.getId() || this.targetAccountId == account.getId();
    }

    public boolean isRelated(UUID accountId) {
        return this.sourceAccountId == accountId || this.targetAccountId == accountId;
    }
}
