package com.krisapps.incomeutility_v2.types.fiscal;

public class CurrencyConfig {
    public static CurrencyConfig DEFAULT = new CurrencyConfig("€", false);
    private String currencySymbol;
    private boolean currencySymbolPrefix = false;

    public CurrencyConfig(String currencySymbol, boolean currencySymbolPrefix) {
        this.currencySymbol = currencySymbol;
        this.currencySymbolPrefix = currencySymbolPrefix;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }

    public boolean isCurrencySymbolPrefix() {
        return currencySymbolPrefix;
    }

    public void setCurrencySymbolPrefix(boolean currencySymbolPrefix) {
        this.currencySymbolPrefix = currencySymbolPrefix;
    }
}
