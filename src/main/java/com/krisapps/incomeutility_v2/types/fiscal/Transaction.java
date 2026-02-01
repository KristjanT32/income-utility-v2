package com.krisapps.incomeutility_v2.types.fiscal;

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

    public Transaction(Type type, double amount, Account account) {
        this.type = type;
        this.amount = amount;
        if (type == Type.MONEY_RECEIVED) {
            this.target = account;
        } else if (type == Type.MONEY_WITHDRAWN) {
            this.source = account;
        }
        this.id = UUID.randomUUID();
    }

    public Transaction(Type type, double amount, Account from, Account to) {
        this.type = type;
        this.amount = amount;
        this.source = from;
        this.target = to;
        this.id = UUID.randomUUID();
    }

    private Transaction.Type type;
    private double amount;
    private Account source;
    private Account target;

    private Date timestamp;
    private UUID id;

    public Type getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public Account getSource() {
        return source;
    }

    public Account getTarget() {
        return target;
    }

}
