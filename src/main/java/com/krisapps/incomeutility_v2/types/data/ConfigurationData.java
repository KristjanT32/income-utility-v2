package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.util.DataManager;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

public class ConfigurationData {
    private UUID lastActiveAccountId;
    private String databaseLocation;
    private CurrencyConfig pricerCurrencyConfiguration;

    public ConfigurationData() {
        this.lastActiveAccountId = null;
        this.databaseLocation = Path.of(DataManager.getDataDirectory() + File.separator + "data.db").toString();
        this.pricerCurrencyConfiguration = CurrencyConfig.DEFAULT;
    }

    public UUID getLastActiveAccountId() {
        return lastActiveAccountId;
    }

    public void setLastActiveAccountId(UUID lastActiveAccountId) {
        this.lastActiveAccountId = lastActiveAccountId;
    }

    public Path getDatabaseLocation() {
        return Path.of(databaseLocation);
    }

    public void setDatabaseLocation(Path databaseLocation) {
        this.databaseLocation = databaseLocation.toString();
    }

    public void setDatabaseLocation(String databaseLocation) {
        this.databaseLocation = databaseLocation;
    }

    public CurrencyConfig getPricerCurrencyConfiguration() {
        return pricerCurrencyConfiguration;
    }

    public void setPricerCurrencyConfiguration(CurrencyConfig pricerCurrencyConfiguration) {
        this.pricerCurrencyConfiguration = pricerCurrencyConfiguration;
    }
}
