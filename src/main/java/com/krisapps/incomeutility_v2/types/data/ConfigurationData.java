package com.krisapps.incomeutility_v2.types.data;

import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.util.DataManager;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

public class ConfigurationData {
    private UUID lastActiveAccountId;
    private String databaseLocation;
    private CurrencyConfig pricerCurrencyConfiguration;
    private String logFileLocation;

    public ConfigurationData() {
        this.lastActiveAccountId = null;
        this.databaseLocation = Path.of(DataManager.getDataDirectory() + File.separator + "data.db").toString();
        this.pricerCurrencyConfiguration = CurrencyConfig.DEFAULT;
        this.logFileLocation = Paths.get(DataManager.getDataDirectory() + File.separator + "utility.log").toString();
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

    public Path getLogFileLocation() {
        return Path.of(logFileLocation);
    }

    public void setLogFileLocation(Path logFileLocation) {
        this.logFileLocation = logFileLocation.toString();
    }
}
