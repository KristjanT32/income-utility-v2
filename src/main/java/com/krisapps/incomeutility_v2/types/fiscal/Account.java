package com.krisapps.incomeutility_v2.types.fiscal;

import com.krisapps.incomeutility_v2.util.DataManager;

import java.util.Arrays;
import java.util.UUID;

public class Account {
    
    public enum Type {
        CASH("Cash"),
        BANK_ACCOUNT("Bank account"),
        SAVINGS("Savings"),
        OTHER("Other"),


        UNKNOWN("Unknown type")
        ;

        private String displayName;

        Type(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static Account.Type ofDisplayName(String displayName) {
            return Arrays.stream(values()).filter(t -> t.displayName.equals(displayName)).findFirst().orElse(UNKNOWN);
        }
    }

    private String name = "";
    private double balance = 0.0d;
    private CurrencyConfig currencyConfig;
    private final UUID id;
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

    public String getName() {
        return name;
    }

    public double getBalance() {
        return balance;
    }

    public UUID getId() {
        return id;
    }

    public Account.Type getType() {
        return type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
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

    public String formatBalance() {
        return DataManager.Formatting.formatMoney(balance, currencyConfig.getCurrencySymbol(), currencyConfig.isCurrencySymbolPrefix());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Account)) {
            return false;
        } else {
            return ((Account)obj).getId().equals(this.id);
        }
    }

    @Override
    public String toString() {
        return String.format("Account(name = %s, balance = %s, type = %s, uuid = %s)", this.name, this.balance, this.type.name(), this.id.toString());
    }
}
