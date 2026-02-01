package com.krisapps.incomeutility_v2.types.fiscal;

import com.krisapps.incomeutility_v2.exceptions.OperationNotPermittedException;

import java.util.ArrayList;
import java.util.HashSet;

public class Account {

    private String name = "";
    private double balance = 0.0d;
    private HashSet<Transaction> transactions = new HashSet<>();

    private boolean isDefault;

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            this.balance += amount;
            return true;
        } else {
            throw new OperationNotPermittedException(this, Transaction.Type.MONEY_WITHDRAWN, "Insufficient balance");
        }
    }

    public boolean deposit(double amount) {
        if (amount > 0) {
            this.balance += amount;
            return true;
        } else {
            throw new IllegalArgumentException("Amount cannot be negative.");
        }
    }

    public boolean transfer(Account to, double amount) {
        return withdraw(amount) && to.deposit(amount);
    }

    public void apply(Transaction transaction) {
        if (!transaction.getSource().equals(this) && !transaction.getTarget().equals(this)) {
            throw new OperationNotPermittedException(this, transaction.getType(), "The specified account is neither the target nor the source of the transaction");
        }

        switch (transaction.getType()) {
            case MONEY_RECEIVED -> {
                if (transaction.getTarget().equals(this)) {
                    deposit(transaction.getAmount());
                }
            }
            case MONEY_WITHDRAWN -> {
                if (transaction.getSource().equals(this)) {
                    withdraw(transaction.getAmount());
                }
            }
            case MONEY_TRANSFERRED -> {
                if (transaction.getSource().equals(this)) {
                    withdraw(transaction.getAmount());
                } else if (transaction.getTarget().equals(this)) {
                    deposit(transaction.getAmount());
                }
            }
        }
        transactions.add(transaction);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Account)) {
            return false;
        } else {
            return ((Account)obj).getName().equals(this.name);
        }
    }
}
