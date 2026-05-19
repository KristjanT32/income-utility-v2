package com.krisapps.incomeutility_v2.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.stream.JsonReader;
import com.krisapps.incomeutility_v2.types.data.ConfigurationData;
import com.krisapps.incomeutility_v2.types.data.LegacyData;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTimeTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.TransactionDeserializer;
import com.krisapps.incomeutility_v2.util.services.MigrationService;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.util.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.logging.Level;

@SuppressWarnings("ConstantConditions")
public class DataManager {

    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .registerTypeAdapter(Transaction.class, new TransactionDeserializer())
            .create();
    private static DataManager instance;
    private final File configFile = new File(System.getProperty("user.home") + File.separator + "IncomeUtility v2" + File.separator + "config.json");
    private Path databaseFilePath = null;
    private boolean isSaving = false;

    private static final Path DATA_DIRECTORY_PATH = Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2");
    private ConfigurationData configurationData;
    private Connection currentConnection;

    private DataManager() {
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public static Path getDataDirectory() {
        return DATA_DIRECTORY_PATH;
    }

    public static void log(String msg) {
        if (msg.toLowerCase().contains("failed") || msg.toLowerCase().contains("error") || msg.toLowerCase().contains("fail") || msg.toLowerCase().contains("couldn't") || msg.toLowerCase().contains("could not")) {
            System.out.println(String.format("[%s IncomeUtility/ERROR]: ", Formatting.formatDate(Date.from(Instant.now()), true)) + msg);
        } else {
            System.out.println(String.format("[%s IncomeUtility/INFO]: ", Formatting.formatDate(Date.from(Instant.now()), true)) + msg);
        }
    }

    public static void log(String msg, Level level) {
        System.out.println(String.format("[%s IncomeUtility/%s]: ", Formatting.formatDate(Date.from(Instant.now()), true), level.getName()) + msg);
    }

    public void initialize() {
        loadConfigurationData();
        databaseFilePath = configurationData.getDatabaseLocation();

        if (currentConnection != null) {
            log("DataManager#initialize has been called after initialization - a new DB connection will not be opened.", Level.WARNING);
        } else {
            currentConnection = getDatabaseConnection();

            try {
                Statement stmt = currentConnection.createStatement();
                ResultSet rs = stmt.executeQuery("PRAGMA user_version");
                int version = rs.next() ? rs.getInt(1) : 0;

                if (version == 0) {
                    initializeDatabase(currentConnection);
                }

            } catch (SQLException e) {
                log("Failed to check database state.");
            }

        }
    }

    private void initializeDatabase(Connection currentConnection) {
        try {
            Statement statement = currentConnection.createStatement();

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "accounts" (
                    	"id"	INTEGER NOT NULL UNIQUE,
                    	"uuid"	TEXT NOT NULL UNIQUE,
                    	"name"	TEXT NOT NULL DEFAULT 'New Account',
                    	"initialBalance"	REAL NOT NULL DEFAULT 0.0,
                    	"type"	TEXT NOT NULL,
                    	"isDefault"	INTEGER NOT NULL DEFAULT 0,
                    	"currencySymbol"	TEXT NOT NULL DEFAULT '€',
                    	"currencyIsPrefixed"	INTEGER NOT NULL DEFAULT 0,
                    	PRIMARY KEY("id" AUTOINCREMENT)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "transaction_categories" (
                    	"id"	INTEGER NOT NULL UNIQUE,
                    	"displayName"	TEXT NOT NULL DEFAULT 'New Category' UNIQUE,
                    	PRIMARY KEY("id" AUTOINCREMENT)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "transactions" (
                    	"id"	INTEGER NOT NULL,
                    	"uuid"	TEXT NOT NULL UNIQUE,
                    	"cashewTransactionId"	TEXT,
                    	"type"	TEXT NOT NULL,
                    	"amount"	REAL NOT NULL,
                    	"sourceAccountId"	TEXT,
                    	"targetAccountId"	TEXT,
                    	"cashewSourceAccountId"	TEXT,
                    	"cashewTargetAccountId"	TEXT,
                    	"category"	TEXT NOT NULL,
                    	"customCategoryId"	INTEGER,
                    	"comment"	TEXT,
                    	"timestamp"	TEXT NOT NULL,
                    	PRIMARY KEY("id" AUTOINCREMENT),
                    	CONSTRAINT "category_fk" FOREIGN KEY("customCategoryId") REFERENCES "transaction_categories"("id")
                    )
                    """);
            statement.execute("PRAGMA user_version = 1");
            log("Database successfully initialized!");
        } catch (SQLException e) {
            log("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void firstTimeFileSetup() {
        log("No files found, initializing first-time setup.");

        try {
            log("Creating a data directory at: " + DATA_DIRECTORY_PATH);
            Files.createDirectory(DATA_DIRECTORY_PATH);
        } catch (IOException e) {
            log("Failed to create data directory: " + e.getMessage());
        }

        createConfigurationFile();
        log("Files successfully created.");
    }

    private void createConfigurationFile() {
        try {
            if (!configFile.exists()) {
                configFile.createNewFile();
                ConfigurationData config = new ConfigurationData();
                saveConfigurationData(config);
            }
        } catch (IOException e) {
            log("Could not create a new data file - " + e.getMessage());
        }
    }

    public void saveConfigurationData(ConfigurationData data) {
        isSaving = true;

        if (!configFile.exists()) {
            createConfigurationFile();
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile, false), StandardCharsets.UTF_16));

            writer.write(gson.toJson(data));
            writer.close();
        } catch (IOException e) {
            log("Data saving failed - " + e.getMessage());
        }
        isSaving = false;
    }

    public void saveCurrentConfigurationData() {
        if (configurationData == null) {
            return;
        }
        saveConfigurationData(configurationData);
    }

    public boolean isSaving() {
        return isSaving;
    }

    /**
     * Loads configuration data from the disk.
     * @return The loaded data.
     */
    public ConfigurationData getConfigurationData() {

        if (configurationData != null) {
            return configurationData;
        } else {
            if (!configFile.exists()) {
                firstTimeFileSetup();
            }

            InputStreamReader inputStreamReader;
            try {
                inputStreamReader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_16);
                JsonReader reader = new JsonReader(inputStreamReader);
                ConfigurationData output = gson.fromJson(reader, ConfigurationData.class);
                if (output == null) {
                    output = new ConfigurationData();
                }
                return output;
            } catch (IOException e) {
                log("Failed to retrieve data from data file: " + e.getMessage());
                return new ConfigurationData();
            }
        }
    }

    public LegacyData getLegacyData(Path path) {
        InputStreamReader inputStreamReader;
        try {
            inputStreamReader = new InputStreamReader(new FileInputStream(path.toFile()), StandardCharsets.UTF_16);
            JsonReader reader = new JsonReader(inputStreamReader);
            LegacyData output = gson.fromJson(reader, LegacyData.class);
            if (output == null) {
                output = new LegacyData();
            }
            return output;
        } catch (IOException e) {
            log("Failed to retrieve data from data file: " + e.getMessage());
            return new LegacyData();
        }
    }

    private void loadConfigurationData() {
        configurationData = getConfigurationData();
    }

    private Connection getDatabaseConnection() {
        try {
            log("Database connection ready!");
            return DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath);
        } catch (SQLException e) {
            Platform.runLater(() -> PopupManager.showPopup("Database initialization error!", "The following error was encountered when initializing the database:\n" + e.getMessage(), Alert.AlertType.ERROR));
        }
        return null;
    }

    //<editor-fold desc="Migration">

    public void migrateJSONDataToSQL() {
        MigrationService service = MigrationService.initialize(currentConnection);

        try {
            service.migrateFromDefaultFile();
            MigrationService.shutdown();
        } catch (FileNotFoundException e) {
            PopupManager.showPopup("Migration unavailable!", "Automatic migration from the default data file is not available right now, as data.json couldn't be found.", Alert.AlertType.ERROR);
        }
    }
    //</editor-fold>


    //<editor-fold desc="Data access">

    public static Transaction mapResultSetToTransaction(ResultSet row) throws SQLException {
        UUID uuid = UUID.fromString(row.getString("uuid"));
        TransactionType type = TransactionType.valueOf(row.getString("type"));
        String cashewTransactionId = row.getString("cashewTransactionId");

        Transaction t = null;
        switch (type) {
            case DEPOSIT, WITHDRAWAL -> t = new Transaction(
                    type,
                    row.getDouble("amount"),
                    UUID.fromString(row.getString("targetAccountId")),
                    Timestamp.valueOf(row.getString("timestamp")).toLocalDateTime(),
                    TransactionCategory.valueOf(row.getString("category")),
                    row.getString("displayName"),
                    row.getString("comment"),
                    uuid
            );
            case TRANSFER -> t = new Transaction(
                    type,
                    row.getDouble("amount"),
                    UUID.fromString(row.getString("sourceAccountId")),
                    UUID.fromString(row.getString("targetAccountId")),
                    Timestamp.valueOf(row.getString("timestamp")).toLocalDateTime(),
                    TransactionCategory.valueOf(row.getString("category")),
                    row.getString("displayName"),
                    row.getString("comment"),
                    uuid
            );
        }
        if (cashewTransactionId != null) {
            t = CashewTransaction.of(
                    t,
                    cashewTransactionId,
                    row.getString("cashewSourceAccountId"),
                    row.getString("cashewTargetAccountId")
            );
        }
        return t;
    }

    public HashSet<Account> getAccounts() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM accounts;");

            ResultSet response = stmt.executeQuery();
            HashSet<Account> out = new HashSet<>();
            while (response.next()) {
                out.add(new Account(
                        UUID.fromString(response.getString("uuid")),
                        response.getString("name"),
                        response.getDouble("initialBalance"),
                        new CurrencyConfig(
                                response.getString("currencySymbol"),
                                response.getBoolean("currencyIsPrefixed")
                        ),
                        Account.Type.valueOf(response.getString("type")),
                        response.getBoolean("isDefault")
                ));
            }
            return out;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying accounts. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return new HashSet<>();
    }

    public Optional<Account> getAccount(UUID accountId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        if (accountId == null) {
            return Optional.empty();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM accounts WHERE uuid = ?;");
            stmt.setString(1, accountId.toString());


            ResultSet response = stmt.executeQuery();
            if (response.next()) {
                return Optional.of(new Account(
                        UUID.fromString(response.getString("uuid")),
                        response.getString("name"),
                        response.getDouble("initialBalance"),
                        new CurrencyConfig(
                                response.getString("currencySymbol"),
                                response.getBoolean("currencyIsPrefixed")
                        ),
                        Account.Type.valueOf(response.getString("type")),
                        response.getBoolean("isDefault")
                ));
            } else {
                return Optional.empty();
            }


        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public HashMap<UUID, ? extends Transaction> getAllTransactions() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id;");

            ResultSet response = stmt.executeQuery();
            HashMap<UUID, Transaction> out = new HashMap<>();
            while (response.next()) {
                Transaction t = mapResultSetToTransaction(response);
                out.put(t.getId(), t);
            }
            return out;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return new HashMap<>();
    }

    public List<Transaction> getTransactions(Account account) {
        return getTransactions(account.getId());
    }

    public List<Transaction> getTransactions(UUID accountId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id WHERE (sourceAccountId = ? OR targetAccountId = ?);");
            stmt.setString(1, accountId.toString());
            stmt.setString(2, accountId.toString());

            ResultSet response = stmt.executeQuery();
            List<Transaction> out = new ArrayList<>();
            while (response.next()) {
                Transaction t = mapResultSetToTransaction(response);
                out.add(t);
            }
            return out;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public Optional<Transaction> getTransaction(UUID transactionId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        if (transactionId == null) {
            return Optional.empty();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id WHERE uuid = ?;");
            stmt.setString(1, transactionId.toString());

            ResultSet response = stmt.executeQuery();
            if (response.next()) {
                return Optional.of(mapResultSetToTransaction(response));
            } else {
                return Optional.empty();
            }


        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public ArrayList<String> getCustomTransactionCategories() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT displayName FROM transaction_categories;");

            ResultSet response = stmt.executeQuery();
            ArrayList<String> categories = new ArrayList<>();
            while (response.next()) {
                categories.add(response.getString("displayName"));
            }

            return categories;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return new ArrayList<>();

    }

    public ArrayList<Pair<Integer, String>> getCustomTransactionCategoryEntries() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement stmt = currentConnection.prepareStatement("SELECT id, displayName FROM transaction_categories;");

            ResultSet response = stmt.executeQuery();
            ArrayList<Pair<Integer, String>> categories = new ArrayList<>();
            while (response.next()) {
                categories.add(new Pair<>(
                        response.getInt("id"),
                        response.getString("displayName")
                ));
            }

            return categories;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying custom transaction category entries. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public boolean customTransactionCategoryExists(String category) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "SELECT COUNT(*) FROM transaction_categories WHERE (displayName = ? or displayName LIKE ?);"
            );
            statement.setString(1, category);
            statement.setString(2, category);

            ResultSet set = statement.executeQuery();

            return set.getInt(1) > 0;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean customTransactionCategoryExists(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "SELECT COUNT(*) FROM transaction_categories WHERE id = ?;"
            );
            statement.setInt(1, id);

            ResultSet set = statement.executeQuery();

            return set.getInt(1) > 0;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean transactionExists(Account source, Transaction t) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "SELECT COUNT(*) FROM transactions WHERE (sourceAccountId = ? OR targetAccountId = ?) AND uuid = ?"
            );
            statement.setString(1, source.getId().toString());
            statement.setString(2, source.getId().toString());
            statement.setString(3, t.getId().toString());

            ResultSet set = statement.executeQuery();

            return set.getInt(1) > 0;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean importedTransactionExists(Account source, CashewTransaction t) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "SELECT COUNT(*) FROM transactions WHERE (sourceAccountId = ? OR targetAccountId = ?) AND cashewTransactionId = ?"
            );
            statement.setString(1, source.getId().toString());
            statement.setString(2, source.getId().toString());
            statement.setString(3, t.getCashewTransactionId());

            ResultSet set = statement.executeQuery();

            return set.getInt(1) > 0;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean accountExists(Account account) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "SELECT COUNT(*) FROM accounts WHERE uuid = ?;"
            );
            statement.setString(1, account.getId().toString());

            ResultSet set = statement.executeQuery();

            return set.getInt(1) > 0;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public Optional<UUID> getLastActiveAccount() {
        ConfigurationData d = getConfigurationData();
        return Optional.ofNullable(d.getLastActiveAccountId());
    }

    //</editor-fold>

    //<editor-fold desc="Data modification">
    public void addAccount(Account account) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "INSERT INTO accounts (uuid, name, initialBalance, type, isDefault, currencySymbol, currencyIsPrefixed) VALUES (?, ?, ?, ?, ?, ?, ?);"
            );

            statement.setString(1, account.getId().toString());
            statement.setString(2, account.getName());
            statement.setDouble(3, account.getInitialBalance());
            statement.setString(4, account.getType().name());
            statement.setBoolean(5, account.isDefault());
            statement.setString(6, account.getCurrencyConfig().getCurrencySymbol());
            statement.setBoolean(7, account.getCurrencyConfig().isCurrencySymbolPrefix());

            statement.execute();

        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void updateAccount(UUID accountId, Account data) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "UPDATE accounts SET uuid = ?, name = ?,initialBalance = ?, type = ?, isDefault = ?, currencySymbol = ?, currencyIsPrefixed = ? WHERE uuid = ?;"
            );
            statement.setString(1, accountId.toString());
            statement.setString(2, data.getName());
            statement.setDouble(3, data.getInitialBalance());
            statement.setString(4, data.getType().name());
            statement.setBoolean(5, data.isDefault());
            statement.setString(6, data.getCurrencyConfig().getCurrencySymbol());
            statement.setBoolean(7, data.getCurrencyConfig().isCurrencySymbolPrefix());
            statement.setString(8, accountId.toString());

            statement.execute();

        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while updating account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void deleteAccount(UUID accountId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "DELETE FROM accounts WHERE uuid = ?;"
            );
            statement.setString(1, accountId.toString());

            statement.execute();

        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while deleting account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public int addCustomTransactionCategory(String category) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {

            if (!customTransactionCategoryExists(category)) {
                PreparedStatement statement = currentConnection.prepareStatement(
                        "INSERT INTO transaction_categories (displayName) VALUES (?);"
                );

                statement.setString(1, category);
                statement.execute();
            }

            PreparedStatement idQuery = currentConnection.prepareStatement("SELECT id FROM transaction_categories WHERE displayName = ? OR displayName LIKE ?;");
            idQuery.setString(1, category);
            idQuery.setString(2, category);

            return idQuery.executeQuery().getInt(1);
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            return -1;
        }
    }

    public void updateCustomTransactionCategory(int id, String newValue) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            if (customTransactionCategoryExists(id)) {
                PreparedStatement statement = currentConnection.prepareStatement(
                        "UPDATE transaction_categories SET displayName = ? WHERE id = ?;"
                );

                statement.setString(1, newValue);
                statement.setInt(2, id);
                statement.execute();
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while updating custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void removeCustomTransactionCategory(String category) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "DELETE FROM transaction_categories WHERE displayName = ? OR displayName LIKE ?;"
            );
            statement.setString(1, category);
            statement.setString(2, category);

            statement.execute();

        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void removeCustomTransactionCategory(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "DELETE FROM transaction_categories WHERE id = ?;"
            );
            statement.setInt(1, id);
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while trying to delete a custom transaction category. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void replaceCustomTransactionInTransactions(int oldId, int newId) throws InvalidParameterException {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        if (!customTransactionCategoryExists(newId)) {
            throw new InvalidParameterException("Invalid replacement ID - " + newId + " does not correspond to any custom transaction categories!");
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement("UPDATE transactions SET customCategoryId = ? WHERE customCategoryId = ?;");
            statement.setInt(1, newId);
            statement.setInt(2, oldId);
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to update data!", "An SQL error was encountered while patching transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void addTransaction(Transaction transaction) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        int customCategoryId = 0;
        if (transaction.getCategory().equals(TransactionCategory.CUSTOM)) {
            customCategoryId = addCustomTransactionCategory(transaction.getCustomCategory());
        }

        try {
            if (CashewTransaction.isImported(transaction)) {
                CashewTransaction cashew = (CashewTransaction) transaction;
                PreparedStatement statement = currentConnection.prepareStatement(
                        "INSERT INTO transactions (uuid, cashewTransactionId, type, amount, sourceAccountId, targetAccountId, cashewSourceAccountId, cashewTargetAccountId, category, customCategoryId, comment, timestamp)" +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );
                statement.setString(1, cashew.getId().toString());
                statement.setString(2, cashew.getCashewTransactionId());
                statement.setString(3, cashew.getType().name());
                statement.setDouble(4, cashew.getAmount());
                statement.setString(5, cashew.getSourceAccountId() != null ? cashew.getSourceAccountId().toString() : null);
                statement.setString(6, cashew.getTargetAccountId() != null ? cashew.getTargetAccountId().toString() : null);
                statement.setString(7, cashew.getCashewSourceAccount());
                statement.setString(8, cashew.getCashewTargetAccount());
                statement.setString(9, cashew.getCategory().name());
                statement.setInt(10, customCategoryId == -1 ? 0 : customCategoryId);
                statement.setString(11, cashew.getComment());
                statement.setString(12, Timestamp.valueOf(cashew.getTimestamp()).toString());
                statement.execute();
            } else {
                PreparedStatement statement = currentConnection.prepareStatement(
                        "INSERT INTO transactions (uuid, type, amount, sourceAccountId, targetAccountId, category, customCategoryId, comment, timestamp)" +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)"
                );

                statement.setString(1, transaction.getId().toString());
                statement.setString(2, transaction.getType().name());
                statement.setDouble(3, transaction.getAmount());
                statement.setString(4, transaction.getSourceAccountId() != null ? transaction.getSourceAccountId().toString() : null);
                statement.setString(5, transaction.getTargetAccountId() != null ? transaction.getTargetAccountId().toString() : null);
                statement.setString(6, transaction.getCategory().name());
                statement.setInt(7, customCategoryId == -1 ? 0 : customCategoryId);
                statement.setString(8, transaction.getComment());
                statement.setString(9, Timestamp.valueOf(transaction.getTimestamp()).toString());
                statement.execute();
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void updateTransaction(UUID transactionId, Transaction data) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        int customCategoryId = 0;
        if (data.getCategory().equals(TransactionCategory.CUSTOM)) {
            customCategoryId = addCustomTransactionCategory(data.getCustomCategory());
        }

        try {
            if (CashewTransaction.isImported(data)) {
                CashewTransaction cashew = (CashewTransaction) data;
                PreparedStatement statement = currentConnection.prepareStatement(
                        "UPDATE transactions SET uuid = ?, cashewTransactionId = ?, type = ?, amount = ?, sourceAccountId = ?, targetAccountId = ?, cashewSourceAccountId = ?, cashewTargetAccountId = ?, category = ?, customCategoryId = ?, comment = ?, timestamp = ? WHERE uuid = ?;"
                );
                statement.setString(1, cashew.getId().toString());
                statement.setString(2, cashew.getCashewTransactionId());
                statement.setString(3, cashew.getType().name());
                statement.setDouble(4, cashew.getAmount());
                statement.setString(5, cashew.getSourceAccountId() != null ? cashew.getSourceAccountId().toString() : null);
                statement.setString(6, cashew.getTargetAccountId() != null ? cashew.getTargetAccountId().toString() : null);
                statement.setString(7, cashew.getCashewSourceAccount());
                statement.setString(8, cashew.getCashewTargetAccount());
                statement.setString(9, cashew.getCategory().name());
                statement.setInt(10, customCategoryId == -1 ? 0 : customCategoryId);
                statement.setString(11, cashew.getComment());
                statement.setString(12, Timestamp.valueOf(cashew.getTimestamp()).toString());
                statement.setString(13, transactionId.toString());
                statement.execute();

            } else {
                PreparedStatement statement = currentConnection.prepareStatement(
                        "UPDATE transactions SET uuid = ?, type = ?, amount = ?, sourceAccountId = ?, targetAccountId = ?, category = ?, customCategoryId = ?, comment = ?, timestamp = ? WHERE uuid = ?;"
                );

                statement.setString(1, data.getId().toString());
                statement.setString(2, data.getType().name());
                statement.setDouble(3, data.getAmount());
                statement.setString(4, data.getSourceAccountId() != null ? data.getSourceAccountId().toString() : null);
                statement.setString(5, data.getTargetAccountId() != null ? data.getTargetAccountId().toString() : null);
                statement.setString(6, data.getCategory().name());
                statement.setInt(7, customCategoryId == -1 ? 0 : customCategoryId);
                statement.setString(8, data.getComment());
                statement.setString(9, Timestamp.valueOf(data.getTimestamp()).toString());
                statement.setString(10, transactionId.toString());
                statement.execute();

            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while updating transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }

        log("Updated transaction #" + transactionId);
    }

    public void deleteTransaction(UUID transactionId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            PreparedStatement statement = currentConnection.prepareStatement(
                    "DELETE FROM transactions WHERE uuid = ?"
            );
            statement.setString(1, transactionId.toString());
            statement.execute();

        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while deleting transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }

    }


    /**
     * Updates the saved ID of the last open account.
     *
     * @param account The account whose ID to set as the new last open account ID.
     */
    public void updateLastOpenAccount(Account account) {
        if (account == null) return;
        if (configurationData == null) {
            initialize();
        }
        configurationData.setLastActiveAccountId(account.getId());
    }
    //</editor-fold>

    public void updateDatabaseLocation(Path location) {
        if (location == null) return;
        if (configurationData == null) {
            initialize();
        }
        configurationData.setDatabaseLocation(location);
    }

    /**
     * Contains various methods for formatting data.
     */
    public static class Formatting {

        public static DecimalFormat decimalFormatter = new DecimalFormat("#.##");

        public static String generateDurationString(Date start, Date current, boolean showZeros, boolean withWords) {
            Instant startInstant = start.toInstant();
            Instant endInstant = current.toInstant();

            Duration dur = Duration.between(startInstant, endInstant);

            long days = Math.abs(dur.toDays());
            long hours = Math.abs(dur.minusDays(days).toHours());
            long minutes = Math.abs(dur.minusDays(days).minusHours(hours).toMinutes());
            long seconds = Math.abs(dur.minusDays(days).minusHours(hours).minusMinutes(minutes).toSeconds());

            if (!showZeros) {
                if (withWords) {
                    return (days > 0 ? (int) days + " days, " : "") + (hours > 0 ? (int) hours + " hours, " : "") + (minutes > 0 ? (int) minutes + " minutes, " : "") + (seconds > 0 ? (int) seconds + " seconds" : "");
                } else {
                    return (days > 0 ? (int) days + ":" : "") + (hours > 0 ? (int) hours + ":" : "") + (minutes > 0 ? (int) minutes + ":" : "") + (seconds > 0 ? (int) seconds + ":" : "");
                }
            } else {
                if (withWords) {
                    return String.format("%s hours, %s minutes and %s seconds", (int) hours, (int) minutes, (int) seconds);
                } else {
                    return String.format("%s:%s:%s", formatTimeUnit((int) hours), formatTimeUnit((int) minutes), formatTimeUnit((int) seconds));
                }
            }
        }

        public static String formatDate(Date date, boolean withTime) {

            if (date == null) {
                return "N/A";
            }

            DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter dateAndTime = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

            if (withTime) {
                return dateAndTime.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            } else {
                return dateOnly.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
            }
        }

        public static String formatLocalDate(LocalDate date) {

            if (date == null) {
                return "N/A";
            }

            DateTimeFormatter dateOnly = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return dateOnly.format(date);
        }

        public static String formatMoney(double money, String symbol, boolean symbolIsPrefix) {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            if (symbolIsPrefix) {
                return symbol + decimalFormat.format(money);
            } else {
                return decimalFormat.format(money) + symbol;
            }
        }

        public static String formatMoney(double money, CurrencyConfig config) {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            if (config.isCurrencySymbolPrefix()) {
                return config.getCurrencySymbol() + decimalFormat.format(money);
            } else {
                return decimalFormat.format(money) + config.getCurrencySymbol();
            }
        }

        public static String formatMoney(double money) {
            DecimalFormat decimalFormat = new DecimalFormat("0.00");
            return decimalFormat.format(money);
        }


        public static String formatTimeUnit(int unit) {
            return unit <= 9
                    ? "0" + unit
                    : String.valueOf(unit);
        }

        public static String formatTime(Date date) {
            if (date == null) {
                return "N/A";
            }
            return DateTimeFormatter.ofPattern("HH:mm:ss").format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        }

        public static String formatLocalTime(LocalTime time) {

            if (time == null) {
                return "N/A";
            }

            return DateTimeFormatter.ofPattern("HH:mm:ss").format(time);
        }

        public static String capitalize(String str) {
            if (str.isEmpty()) {
                return str;
            }

            return Character.toString(str.charAt(0)).toUpperCase() + str.toLowerCase().substring(1);
        }

        /**
         * Replaces underscores with spaces, trims, then capitalizes the input string.
         *
         * @param str The input string
         * @return A string with underscores replaced with spaces and the first letter capitalized with no trailing spaces.
         */
        public static String humanize(String str) {
            String s = str.toLowerCase();
            s = s.trim();
            s = s.replace('_', ' ');

            return capitalize(s);
        }

        public static String getNumberSuffix(int number) {
            return switch (String.valueOf(number).charAt(String.valueOf(number).length() - 1)) {
                case 1 -> "st";
                case 2 -> "nd";
                case 3 -> "rd";
                default -> "th";
            };
        }

        public Date dateFromJSON(String date) {
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            try {
                return format.parse(date);
            } catch (ParseException e) {
                log("Failed to parse a date from '" + date + "'");
                return null;
            }
        }
    }

}
