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
import com.krisapps.incomeutility_v2.types.pricer.Dish;
import com.krisapps.incomeutility_v2.types.pricer.DishIngredient;
import com.krisapps.incomeutility_v2.types.pricer.Product;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final File configFile;
    private Path databaseFilePath;
    private static final Path DATA_DIRECTORY_PATH = Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2");

    private boolean isSaving = false;
    private ConfigurationData configurationData;
    private Connection currentConnection;

    public static Logging logger = Logging.getInstance();

    private DataManager() {
        this.configFile = Path.of(DATA_DIRECTORY_PATH.toString(), "config.json").toFile();
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
            logger.log(msg, Level.SEVERE);
        } else {
            logger.log(msg, Level.INFO);
        }
    }

    public static void log(String msg, String modulePrefix) {
        if (msg.toLowerCase().contains("failed") || msg.toLowerCase().contains("error") || msg.toLowerCase().contains("fail") || msg.toLowerCase().contains("couldn't") || msg.toLowerCase().contains("could not")) {
            logger.log(msg, modulePrefix, Level.SEVERE);
        } else {
            logger.log(msg, modulePrefix, Level.INFO);
        }
    }

    public static void log(String msg, Level level) {
        logger.log(msg, level);
    }

    public static void log(String msg, String modulePrefix, Level level) {
        logger.log(msg, modulePrefix, level);
    }

    public void initialize() {
        loadConfigurationData();
        databaseFilePath = configurationData.getDatabaseLocation();
        logger.initialize(configurationData.getLogFileLocation());
        logger.setEnableDebug(configurationData.isDebugEnabled());

        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();

            try (Statement stmt = currentConnection.createStatement();
                 ResultSet rs = stmt.executeQuery("PRAGMA user_version")) {
                int version = rs.next() ? rs.getInt(1) : 0;

                if (version == 0) {
                    initializeDatabase(currentConnection);
                }

            } catch (SQLException e) {
                log("Failed to check database state. Error: " + e.getMessage());
                logger.logStackTrace(e);
            }

        }
    }

    public void reinitializeCurrentDatabase() {
        initializeDatabase(currentConnection);
    }

    private void initializeDatabase(Connection currentConnection) {
        try (Statement statement = currentConnection.createStatement()) {

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
                     	"cashewPairedTransactionId"	TEXT,
                     	"category"	TEXT NOT NULL,
                     	"customCategoryId"	INTEGER,
                     	"comment"	TEXT,
                     	"timestamp"	TEXT NOT NULL,
                     	PRIMARY KEY("id" AUTOINCREMENT),
                     	CONSTRAINT "category_fk" FOREIGN KEY("customCategoryId") REFERENCES "transaction_categories"("id")
                     )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "products" (
                    	"id"	INTEGER NOT NULL,
                    	"name"	TEXT NOT NULL UNIQUE,
                    	"price"	REAL NOT NULL,
                    	"durationOfUseInDays"	REAL NOT NULL DEFAULT 1,
                    	"unitsPerProduct"	REAL NOT NULL,
                    	"smallestUnit"	REAL DEFAULT 0.5,
                    	"unitSingular"	TEXT NOT NULL DEFAULT 'gram',
                    	"unitPlural"	TEXT DEFAULT 'grams',
                    	PRIMARY KEY("id" AUTOINCREMENT)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "dishes" (
                    	"id"	INTEGER NOT NULL,
                    	"name"	TEXT NOT NULL DEFAULT 'New Dish' UNIQUE,
                    	"servings"	REAL NOT NULL DEFAULT 1,
                    	PRIMARY KEY("id" AUTOINCREMENT)
                    )
                    """);

            statement.execute("""
                    CREATE TABLE IF NOT EXISTS "dish_ingredients" (
                     	"relationId"	INTEGER NOT NULL,
                     	"productId"	INTEGER NOT NULL,
                     	"dishId"	INTEGER NOT NULL,
                     	"quantity"	REAL NOT NULL DEFAULT 1,
                     	PRIMARY KEY("relationId" AUTOINCREMENT),
                     	FOREIGN KEY("dishId") REFERENCES "dishes"("id"),
                     	FOREIGN KEY("productId") REFERENCES "products"("id")
                     )
                    """);
            statement.execute("PRAGMA user_version = 1");

            statement.execute("INSERT OR IGNORE INTO transaction_categories (id, displayName) VALUES (0, 'No custom category')");
            log("Database successfully initialized!");
        } catch (SQLException e) {
            log("Failed to initialize database: " + e.getMessage());
            logger.logStackTrace(e);
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
            log("Data saving failed: " + e.getMessage());
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
     *
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
            logger.debug("Database connection ready!");
            return DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath);
        } catch (SQLException e) {
            Platform.runLater(() -> PopupManager.showPopup("Database initialization error!", "The following error was encountered when initializing the database:\n" + e.getMessage(), Alert.AlertType.ERROR));
            log("");
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
            logger.logStackTrace(e);
        }
    }
    //</editor-fold>

    //<editor-fold desc="System data modification">

    /**
     * Drops the specified table.
     * This operation cannot be undone.
     *
     * @param tableName The table to drop.
     * @return <code>true</code> if the table was dropped, <code>false</code> otherwise.
     */
    public boolean dropTable(String tableName) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement("DROP TABLE IF EXISTS " + tableName + ";")) {
            statement.execute();
            return true;
        } catch (SQLException e) {
            PopupManager.showPopup("Couldn't drop table " + tableName, "Something went wrong when trying to drop table '" + tableName + "'. Error details: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Drops all tables in the database.
     * This operation cannot be undone.
     *
     * @return <code>true</code> if the table was dropped, <code>false</code> otherwise.
     */
    public boolean dropAllTables() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        for (String table : getTables()) {
            try (PreparedStatement statement = currentConnection.prepareStatement("DROP TABLE " + table + ";")) {
                statement.execute();
            } catch (SQLException e) {
                PopupManager.showPopup("Couldn't drop table '" + table + "'", "Something went wrong when trying to drop table '" + table + "'. Error details: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    //</editor-fold>


    //<editor-fold desc="Data access">
    public Transaction mapResultSetToTransaction(ResultSet row) throws SQLException {
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
                    row.getString("cashewTargetAccountId"),
                    row.getString("cashewPairedTransactionId")
            );
        }
        return t;
    }

    public Product mapResultSetToProduct(ResultSet row) throws SQLException {
        try {
            return new Product(
                    row.getInt("id"),
                    row.getString("name"),
                    row.getDouble("price"),
                    row.getDouble("durationOfUseInDays"),
                    row.getDouble("unitsPerProduct"),
                    row.getDouble("smallestUnit"),
                    row.getString("unitSingular"),
                    row.getString("unitPlural")
            );
        } catch (SQLException e) {
            log("Failed to map the supplied ResultSet to a Product record!");
            return null;
        }
    }

    public Dish mapResultSetToDish(ResultSet row, List<DishIngredient> ingredients) throws SQLException {
        try {
            return new Dish(
                    row.getInt("id"),
                    row.getString("name"),
                    row.getDouble("servings"),
                    ingredients
            );
        } catch (SQLException e) {
            log("Failed to map the supplied ResultSet to a Dish record!");
            return null;
        }
    }

    public List<String> getTables() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (ResultSet response = currentConnection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"})) {
            ArrayList<String> tables = new ArrayList<>();
            while (response.next()) {
                tables.add(response.getString("table_name"));
            }

            return tables;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered when querying table data. Error details: " + e.getMessage(), Alert.AlertType.ERROR);
            return new ArrayList<>();
        }
    }

    public HashSet<Account> getAccounts() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM accounts;");
             ResultSet response = stmt.executeQuery()) {
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

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM accounts WHERE uuid = ?;")) {
            stmt.setString(1, accountId.toString());

            try (ResultSet response = stmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Attempts to pick the account which was the last one in use.
     * If an active account ID exists, it will return that account.
     * If no such ID exists or the ID points to a non-existent account,
     * this method will pick the first account in the database.
     * If no accounts exist, it will return an empty Optional.
     *
     * @return An Optional representing the last active Account, or an
     * empty Optional, if no such account could be selected.
     */
    public Optional<Account> tryPickActiveAccount() {
        if (getAccounts().isEmpty()) {
            return Optional.empty();
        } else {
            if (getLastActiveAccount().isPresent() && accountExists(getLastActiveAccount().get())) {
                return getAccount(getLastActiveAccount().get());
            } else {
                return getAccounts().stream().findFirst();
            }
        }
    }

    public HashMap<UUID, ? extends Transaction> getAllTransactions() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id;");
             ResultSet response = stmt.executeQuery()) {
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

    public List<Transaction> getTransactionsWithCustomCategory(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id WHERE customCategoryId = ?;")) {
            stmt.setInt(1, id);

            try (ResultSet response = stmt.executeQuery()) {
                List<Transaction> out = new ArrayList<>();
                while (response.next()) {
                    Transaction t = mapResultSetToTransaction(response);
                    out.add(t);
                }
                return out;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public List<Transaction> getTransactions(UUID accountId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id WHERE (sourceAccountId = ? OR targetAccountId = ?);")) {
            stmt.setString(1, accountId.toString());
            stmt.setString(2, accountId.toString());

            try (ResultSet response = stmt.executeQuery()) {
                List<Transaction> out = new ArrayList<>();
                while (response.next()) {
                    Transaction t = mapResultSetToTransaction(response);
                    out.add(t);
                }
                return out;
            }
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

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM transactions JOIN transaction_categories AS categories ON customCategoryId = categories.id WHERE uuid = ?;")) {
            stmt.setString(1, transactionId.toString());

            try (ResultSet response = stmt.executeQuery()) {
                if (response.next()) {
                    return Optional.of(mapResultSetToTransaction(response));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Retrieves the paired transaction ID for the transaction with the supplied ID.
     *
     * @param transactionId The transaction whose paired transaction to look up.
     * @param findParent    If <code>true</code>, will look up the parent transaction for the supplied ID. If <code>false</code>, will find the paired transaction, treating the supplied ID as the parent transaction.
     * @return The ID of the paired transaction, or an empty Optional.
     */
    public Optional<String> getPairedTransaction(String transactionId, boolean findParent) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        if (transactionId == null) {
            return Optional.empty();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement(findParent ? "SELECT * FROM transactions WHERE (cashewPairedTransactionId = ?);" : "SELECT * FROM transactions WHERE (cashewTransactionId = ?);")) {
            stmt.setString(1, transactionId);

            try (ResultSet response = stmt.executeQuery()) {
                if (response.next()) {
                    return Optional.of(findParent ? response.getString("cashewTransactionId") : response.getString("cashewPairedTransactionId"));
                } else {
                    return Optional.empty();
                }
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

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT displayName FROM transaction_categories;");
             ResultSet response = stmt.executeQuery()) {
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

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT id, displayName FROM transaction_categories;");
             ResultSet response = stmt.executeQuery()) {
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

    public ArrayList<Product> getProducts() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM products;");
             ResultSet response = stmt.executeQuery()) {
            ArrayList<Product> products = new ArrayList<>();
            while (response.next()) {
                products.add(mapResultSetToProduct(response));
            }

            return products;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying products. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return new ArrayList<>();
    }

    public Optional<Product> getProduct(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM products WHERE id = ?;")) {
            stmt.setInt(1, id);

            try (ResultSet response = stmt.executeQuery()) {
                if (response.next()) {
                    return Optional.of(mapResultSetToProduct(response));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying products. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return Optional.empty();
    }

    public Optional<Dish> getDish(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM dishes WHERE id = ?;")) {
            stmt.setInt(1, id);

            try (ResultSet response = stmt.executeQuery()) {
                if (response.next()) {
                    return Optional.of(mapResultSetToDish(response, getDishIngredients(id)));
                } else {
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying products. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return Optional.empty();
    }

    public List<Dish> getDishes() {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM dishes;");
             ResultSet response = stmt.executeQuery()) {
            ArrayList<Dish> out = new ArrayList<>();
            while (response.next()) {
                out.add(mapResultSetToDish(response, getDishIngredients(response.getInt("id"))));
            }

            return out;
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying dishes. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return new ArrayList<>();
    }

    public List<DishIngredient> getDishIngredients(int dishId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement stmt = currentConnection.prepareStatement("SELECT * FROM dish_ingredients WHERE dishId = ?;")) {
            stmt.setInt(1, dishId);

            try (ResultSet response = stmt.executeQuery()) {
                List<DishIngredient> out = new ArrayList<>();
                while (response.next()) {
                    Optional<Product> product = getProduct(response.getInt("productId"));
                    if (product.isEmpty()) {
                        log("Invalid dish ingredient entry found: relation ID #" + response.getInt("relationId") + " - invalid product ID!");
                        continue;
                    }

                    out.add(new DishIngredient(
                            response.getInt("relationId"),
                            response.getInt("dishId"),
                            product.get(),
                            response.getDouble("quantity")
                    ));
                }

                return out;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to retrieve data!", "An SQL error was encountered while querying dish ingredients. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return new ArrayList<>();
    }

    public boolean customTransactionCategoryExists(String category) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM transaction_categories WHERE (displayName = ? or displayName LIKE ?);")) {
            statement.setString(1, category);
            statement.setString(2, category);

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean customTransactionCategoryExists(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM transaction_categories WHERE id = ?;")) {
            statement.setInt(1, id);

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean pairedTransactionExists(String transactionId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM transactions WHERE cashewTransactionId = ? OR cashewPairedTransactionId = ?")) {
            statement.setString(1, transactionId);
            statement.setString(2, transactionId);

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while looking for Cashew transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean transactionExists(Account source, Transaction t) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM transactions WHERE (sourceAccountId = ? OR targetAccountId = ?) AND uuid = ?")) {
            statement.setString(1, source.getId().toString());
            statement.setString(2, source.getId().toString());
            statement.setString(3, t.getId().toString());

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean importedTransactionExists(Account source, CashewTransaction t) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM transactions WHERE (sourceAccountId = ? OR targetAccountId = ?) AND cashewTransactionId = ?")) {
            statement.setString(1, source.getId().toString());
            statement.setString(2, source.getId().toString());
            statement.setString(3, t.getCashewTransactionId());

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking transactions. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean accountExists(Account account) {
        return accountExists(account.getId());
    }

    public boolean accountExists(UUID accountId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM accounts WHERE uuid = ?;")) {
            statement.setString(1, accountId.toString());

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking account data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean productExists(Product product) {
        return productExists(product.id());
    }

    public boolean productExists(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM products WHERE id = ?;")) {
            statement.setInt(1, id);

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking product data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public boolean dishExists(Dish dish) {
        return dishExists(dish.id());
    }

    public boolean dishExists(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "SELECT COUNT(*) FROM dishes WHERE id = ?;")) {
            statement.setInt(1, id);

            try (ResultSet set = statement.executeQuery()) {
                return set.getInt(1) > 0;
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to query data!", "An SQL error was encountered while checking dish data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
        return false;
    }

    public Optional<UUID> getLastActiveAccount() {
        ConfigurationData d = getConfigurationData();
        return Optional.ofNullable(d.getLastActiveAccountId());
    }

    public CurrencyConfig getPricerCurrencyConfiguration() {
        return configurationData.getPricerCurrencyConfiguration();
    }

    //</editor-fold>

    //<editor-fold desc="Data modification">
    public void addAccount(Account account) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "INSERT INTO accounts (uuid, name, initialBalance, type, isDefault, currencySymbol, currencyIsPrefixed) VALUES (?, ?, ?, ?, ?, ?, ?);")) {

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

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "UPDATE accounts SET uuid = ?, name = ?,initialBalance = ?, type = ?, isDefault = ?, currencySymbol = ?, currencyIsPrefixed = ? WHERE uuid = ?;")) {
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

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "DELETE FROM accounts WHERE uuid = ?;")) {
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
                try (PreparedStatement statement = currentConnection.prepareStatement(
                        "INSERT INTO transaction_categories (displayName) VALUES (?);")) {

                    statement.setString(1, category);
                    statement.execute();
                }
            }

            try (PreparedStatement idQuery = currentConnection.prepareStatement("SELECT id FROM transaction_categories WHERE displayName = ? OR displayName LIKE ?;")) {
                idQuery.setString(1, category);
                idQuery.setString(2, category);

                try (ResultSet rs = idQuery.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : -1;
                }
            }
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
                try (PreparedStatement statement = currentConnection.prepareStatement(
                        "UPDATE transaction_categories SET displayName = ? WHERE id = ?;")) {

                    statement.setString(1, newValue);
                    statement.setInt(2, id);
                    statement.execute();
                }
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while updating custom transaction categories. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void removeCustomTransactionCategory(String category) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "DELETE FROM transaction_categories WHERE displayName = ? OR displayName LIKE ?;")) {
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

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "DELETE FROM transaction_categories WHERE id = ?;")) {
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

        try (PreparedStatement statement = currentConnection.prepareStatement("UPDATE transactions SET customCategoryId = ? WHERE customCategoryId = ?;")) {
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
                try (PreparedStatement statement = currentConnection.prepareStatement(
                        "INSERT INTO transactions (uuid, cashewTransactionId, type, amount, sourceAccountId, targetAccountId, cashewSourceAccountId, cashewTargetAccountId, category, customCategoryId, comment, timestamp, cashewPairedTransactionId)" +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
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
                    statement.setString(13, cashew.getCashewPairedTransactionId());
                    statement.execute();
                }
            } else {
                try (PreparedStatement statement = currentConnection.prepareStatement(
                        "INSERT INTO transactions (uuid, type, amount, sourceAccountId, targetAccountId, category, customCategoryId, comment, timestamp)" +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

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
                try (PreparedStatement statement = currentConnection.prepareStatement(
                        "UPDATE transactions SET uuid = ?, cashewTransactionId = ?, type = ?, amount = ?, sourceAccountId = ?, targetAccountId = ?, cashewSourceAccountId = ?, cashewTargetAccountId = ?, category = ?, customCategoryId = ?, comment = ?, timestamp = ? WHERE uuid = ?;")) {
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
                }

            } else {
                try (PreparedStatement statement = currentConnection.prepareStatement(
                        "UPDATE transactions SET uuid = ?, type = ?, amount = ?, sourceAccountId = ?, targetAccountId = ?, category = ?, customCategoryId = ?, comment = ?, timestamp = ? WHERE uuid = ?;")) {

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

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "DELETE FROM transactions WHERE uuid = ?")) {
            statement.setString(1, transactionId.toString());
            statement.execute();

        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while deleting transaction data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }

    }

    public void addProduct(Product product) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "INSERT INTO products (name, price, durationOfUseInDays, unitsPerProduct, smallestUnit, unitSingular, unitPlural) VALUES (?, ?, ?, ?, ?, ?, ?);")) {

            statement.setString(1, product.name());
            statement.setDouble(2, product.price());
            statement.setDouble(3, product.durationOfUse());
            statement.setDouble(4, product.unitsPerProduct());
            statement.setDouble(5, product.smallestUnit());
            statement.setString(6, product.unitSingular());
            statement.setString(7, product.unitPlural());
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing product data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void updateProduct(int id, Product data) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                """
                        UPDATE products SET
                                            name = ?,
                                            price = ?,
                                            durationOfUseInDays = ?,
                                            unitsPerProduct = ?,
                                            smallestUnit = ?,
                                            unitSingular = ?,
                                            unitPlural = ?
                        WHERE id = ?;
                        """)) {

            statement.setString(1, data.name());
            statement.setDouble(2, data.price());
            statement.setDouble(3, data.durationOfUse());
            statement.setDouble(4, data.unitsPerProduct());
            statement.setDouble(5, data.smallestUnit());
            statement.setString(6, data.unitSingular());
            statement.setString(7, data.unitPlural());
            statement.setInt(8, id);
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while updating product data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void deleteProduct(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            // Delete all ingredient entries this product is a part of
            try (PreparedStatement ingredientsStatement = currentConnection.prepareStatement(
                    "DELETE FROM dish_ingredients WHERE productId = ?")) {
                ingredientsStatement.setInt(1, id);
                ingredientsStatement.execute();
            }

            // Delete the actual product
            try (PreparedStatement statement = currentConnection.prepareStatement(
                    "DELETE FROM products WHERE id = ?")) {
                statement.setInt(1, id);
                statement.execute();
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while deleting product data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void addDishIngredient(DishIngredient ingredient) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "INSERT INTO dish_ingredients (dishId, productId, quantity) VALUES (?, ?, ?);")) {

            statement.setInt(1, ingredient.dishId());
            statement.setInt(2, ingredient.product().id());
            statement.setDouble(3, ingredient.quantity());
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing dish ingredient data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void updateDishIngredient(int relationId, DishIngredient ingredient) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "UPDATE dish_ingredients SET dishId = ?, productId = ?, quantity = ? WHERE relationId = ?;")) {

            statement.setInt(1, ingredient.dishId());
            statement.setInt(2, ingredient.product().id());
            statement.setDouble(3, ingredient.quantity());
            statement.setInt(4, relationId);
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing dish ingredient data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void deleteDishIngredient(int relationId) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                "DELETE FROM dish_ingredients WHERE relationId = ?")) {
            statement.setInt(1, relationId);
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while deleting dish ingredient data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void addDishIngredients(int dishId, List<DishIngredient> ingredients) {
        if (dishId == -1) {
            throw new InvalidParameterException("Cannot push ingredients to a non-existent dish!");
        }

        if (ingredients.isEmpty()) return;

        for (DishIngredient ingredient : ingredients) {
            addDishIngredient(new DishIngredient(
                    -1,
                    dishId,
                    ingredient.product(),
                    ingredient.quantity()
            ));
        }
    }

    private void updateDishIngredients(int dishId, List<DishIngredient> ingredients) {
        if (ingredients.isEmpty()) return;
        if (dishId == -1) {
            throw new InvalidParameterException("Cannot update dish ingredients for a non-existent dish!");
        }

        for (DishIngredient ingredient : ingredients) {
            if (ingredient.relationId() == -1) {
                log("Cannot update ingredient '" + ingredient + "' - relation ID is invalid!");
                continue;
            }

            if (ingredient.product() == null) {
                log("Cannot update ingredient '" + ingredient + "' - product is invalid!");
                continue;
            }

            updateDishIngredient(ingredient.relationId(), new DishIngredient(
                    ingredient.relationId(),
                    dishId,
                    ingredient.product(),
                    ingredient.quantity()
            ));
        }
    }

    private void deleteDishIngredients(List<DishIngredient> ingredients) {
        for (DishIngredient ingredient : ingredients) {
            if (ingredient.relationId() == -1) {
                log("Cannot delete dish ingredient '" + ingredient + "' - relation ID is invalid!");
                continue;
            }

            deleteDishIngredient(ingredient.relationId());
        }
    }

    public void addDish(Dish dish) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        int generatedDishId = -1;
        try (PreparedStatement statement = currentConnection.prepareStatement(
                "INSERT INTO dishes (name, servings) VALUES (?, ?);",
                Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, dish.name());
            statement.setDouble(2, dish.servings());
            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    generatedDishId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Couldn't obtain the ID, as dish creation failed!");
                }
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while writing dish data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
            return;
        }

        addDishIngredients(generatedDishId, dish.ingredients());
    }

    /**
     * Updates dish data for the dish at the supplied ID to match the supplied data.
     * <br>
     * This method also doesn't update the dish ingredients list. For this, use {@link DataManager#addDishIngredient(DishIngredient)}, {@link DataManager#updateDishIngredient(int, DishIngredient)} and {@link DataManager#deleteDishIngredient(int)}.
     *
     * @param id   The ID of the dish to update.
     * @param data The data to apply to the dish.
     */
    public void updateDish(int id, Dish data) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try (PreparedStatement statement = currentConnection.prepareStatement(
                """
                        UPDATE dishes SET
                                            name = ?,
                                            servings = ?
                        WHERE id = ?;
                        """)) {

            statement.setString(1, data.name());
            statement.setDouble(2, data.servings());
            statement.setDouble(3, id);
            statement.execute();
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while updating dish data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    public void deleteDish(int id) {
        if (currentConnection == null) {
            currentConnection = getDatabaseConnection();
        }

        try {
            // Delete related ingredient entries first
            try (PreparedStatement ingredientStatement = currentConnection.prepareStatement("DELETE FROM dish_ingredients WHERE dishId = ?")) {
                ingredientStatement.setInt(1, id);
                ingredientStatement.execute();
            }

            // Delete the dish
            try (PreparedStatement statement = currentConnection.prepareStatement(
                    "DELETE FROM dishes WHERE id = ?")) {
                statement.setInt(1, id);
                statement.execute();
            }
        } catch (SQLException e) {
            PopupManager.showPopup("Failed to write data!", "An SQL error was encountered while deleting dish data. Error details:\n" + e.getMessage(), Alert.AlertType.ERROR);
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

    /**
     * Updates the saved database path.
     *
     * @param location The path of a SQLite database file.
     */
    public void updateDatabaseLocation(Path location) {
        if (location == null) return;
        if (configurationData == null) {
            initialize();
        }
        configurationData.setDatabaseLocation(location);
    }

    public void updatePricerCurrencyConfiguration(CurrencyConfig config) {
        if (config == null) return;
        if (configurationData == null) {
            initialize();
        }

        configurationData.setPricerCurrencyConfiguration(config);
    }

    public void updatePricerCurrencySymbol(String symbol) {
        if (symbol == null || symbol.isEmpty()) return;
        if (configurationData == null) {
            initialize();
        }

        CurrencyConfig current = configurationData.getPricerCurrencyConfiguration();
        current.setCurrencySymbol(symbol);
        configurationData.setPricerCurrencyConfiguration(current);
    }

    public void updatePricerCurrencyIsPrefix(boolean isPrefixed) {
        if (configurationData == null) {
            initialize();
        }

        CurrencyConfig current = configurationData.getPricerCurrencyConfiguration();
        current.setCurrencySymbolPrefix(isPrefixed);
        configurationData.setPricerCurrencyConfiguration(current);
    }

    public void updateLogLocation(Path location) {
        if (location == null) return;
        if (configurationData == null) {
            initialize();
        }
        configurationData.setLogFileLocation(location);
    }

    public void updateDebugEnabled(boolean enabled) {
        if (configurationData == null) {
            initialize();
        }
        configurationData.setDebugEnabled(enabled);
    }
    //</editor-fold>

}
