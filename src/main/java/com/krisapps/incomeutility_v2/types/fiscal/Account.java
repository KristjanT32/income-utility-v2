package com.krisapps.incomeutility_v2.types.fiscal;

import java.util.Arrays;
import java.util.UUID;

public class Account {

    private final UUID id;
    private String name = "";
    private double initialBalance = 0.0d;
    private CurrencyConfig currencyConfig;
    private Account.Type type;
    private boolean isDefault;

    public Account() {
        this.id = UUID.randomUUID();
        this.currencyConfig = CurrencyConfig.DEFAULT;
    }

    public Account(CurrencyConfig currencyConfig) {
        this.id = UUID.randomUUID();
        this.currencyConfig = currencyConfig;
    }

    public Account(UUID id, String name, double initialBalance, CurrencyConfig currencyConfig, Account.Type type, boolean isDefault) {
        this.id = id;
        this.name = name;
        this.initialBalance = initialBalance;
        this.currencyConfig = currencyConfig;
        this.type = type;
        this.isDefault = isDefault;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getInitialBalance() {
        return initialBalance;
    }

    public void setInitialBalance(double initialBalance) {
        this.initialBalance = initialBalance;
    }

    public UUID getId() {
        return id;
    }

    public Account.Type getType() {
        return type;
    }

    public void setType(Account.Type type) {
        this.type = type;
    }

    public CurrencyConfig getCurrencyConfig() {
        return currencyConfig;
    }

    public void setCurrencyConfig(CurrencyConfig currencyConfig) {
        this.currencyConfig = currencyConfig;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Account)) {
            return false;
        } else {
            return ((Account) obj).getId().equals(this.id);
        }
    }

    @Override
    public String toString() {
        return String.format("Account(name = %s, balance = %s, type = %s, uuid = %s)", this.name, this.initialBalance, this.type.name(), this.id.toString());
    }

    public enum Type {
        CASH("Cash"),
        BANK_ACCOUNT("Bank account"),
        SAVINGS("Savings"),
        OTHER("Other"),


        UNKNOWN("Unknown type");

        private final String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public static Account.Type ofDisplayName(String displayName) {
            return Arrays.stream(values()).filter(t -> t.displayName.equals(displayName)).findFirst().orElse(UNKNOWN);
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
