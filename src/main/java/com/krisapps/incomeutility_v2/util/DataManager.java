package com.krisapps.incomeutility_v2.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.stream.JsonReader;
import com.krisapps.incomeutility_v2.dialogs.LoadingDialog;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTimeTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.LocalDateTypeAdapter;
import com.krisapps.incomeutility_v2.util.misc.TransactionDeserializer;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.sql.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class DataManager {

    private static final Gson gson = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .registerTypeAdapter(LocalDate.class, new LocalDateTypeAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeTypeAdapter())
            .registerTypeAdapter(Transaction.class, new TransactionDeserializer())
            .create();
    private static DataManager instance;
    private final File dataFile = new File(System.getProperty("user.home") + File.separator + "IncomeUtility v2" + File.separator + "data.json");
    private final Path databaseFilePath = Paths.get(System.getProperty("user.home") + File.separator + "IncomeUtility v2" + File.separator + "data.db");
    private boolean isSaving = false;

    private Data currentData;
    private Connection currentConnection;

    private DataManager() {
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
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
        loadData();

        if (currentConnection != null) {
            log("DataManager#initialize has been called after initialization - a new DB connection will not be opened.", Level.WARNING);
        } else {
            currentConnection = getDatabaseConnection();
        }
    }

    private void firstTimeFileSetup() {
        log("No files found, initializing first-time setup.");

        try {
            log("Creating a data directory at: " + Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2"));
            Files.createDirectory(Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2"));
        } catch (IOException e) {
            log("Failed to create data directory: " + e.getMessage());
        }

        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }
            log("Files successfully created.");
        } catch (IOException e) {
            log("Failed to create file: " + e.getMessage());
        }
    }

    private void createDataFile() {
        try {
            if (!dataFile.exists()) {
                dataFile.createNewFile();
                Data data = new Data();
                saveData(data);
            }
        } catch (IOException e) {
            log("Could not create a new data file - " + e.getMessage());
        }
    }


    public void saveData(Data data) {
        isSaving = true;

        if (!dataFile.exists()) {
            createDataFile();
        }

        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dataFile, false), StandardCharsets.UTF_16));

            writer.write(gson.toJson(data));
            writer.close();
        } catch (IOException e) {
            log("Data saving failed - " + e.getMessage());
        }
        isSaving = false;
    }

    public boolean isSaving() {
        return isSaving;
    }

    public void saveCurrentData() {
        if (currentData == null) {
            return;
        }
        saveData(currentData);
    }

    /**
     * Loads the saved data from disk.
     *
     * @return The data
     */
    private Data getData() {

        if (currentData != null) {
            return currentData;
        } else {
            if (!dataFile.exists()) {
                firstTimeFileSetup();
            }

            InputStreamReader inputStreamReader;
            try {
                inputStreamReader = new InputStreamReader(new FileInputStream(dataFile), StandardCharsets.UTF_16);
                JsonReader reader = new JsonReader(inputStreamReader);
                Data output = gson.fromJson(reader, Data.class);
                if (output == null) {
                    output = new Data();
                }
                return output;
            } catch (IOException e) {
                log("Failed to retrieve data from data file: " + e.getMessage());
                return new Data();
            }
        }
    }

    private void loadData() {
        log("Caching current data...");
        currentData = getData();
        log("Done.");
    }

    private Connection getDatabaseConnection() {
        try {
            log("Database connection ready!");
            return DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath);
        } catch (SQLException e) {
            Platform.runLater(() -> {
                PopupManager.showPopup("Database initialization error!", "The following error was encountered when initializing the database:\n" + e.getMessage(), Alert.AlertType.ERROR);
            });
        }
        return null;
    }

    //<editor-fold desc="Migration">

    public void migrateJSONDataToSQL() {
        try {
            currentConnection.setAutoCommit(false);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        AtomicBoolean success = new AtomicBoolean(false);

        PopupManager.showConfirmationAsync("Begin data migration?", "Would you like to begin the JSON to SQL data migration process? This will copy all data from the data.json file and transform it into data storable in a SQLite database.\nThe process takes around a minute at most.\n\nProceed?",
                new ButtonType("Start migration", ButtonBar.ButtonData.APPLY),
                new ButtonType("No, not now", ButtonBar.ButtonData.CANCEL_CLOSE)
        ).ifPresent(choice -> {
            if (choice.getButtonData().equals(ButtonBar.ButtonData.APPLY)) {
                Platform.runLater(() -> {
                    LoadingDialog dlg = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_PROGRESSBAR);
                    dlg.setPrimaryLabel("Data migration in progress");
                    dlg.setSecondaryLabel("Preparing for migration, please wait...");
                    dlg.show("JSON -> SQLite Data Migration Process", () -> {
                        dlg.setSecondaryLabel("Copying custom transactions...");
                        boolean categoriesCopied = copyCategories(dlg);
                        if (!categoriesCopied) {
                            PopupManager.showPopup("Category migration failed!", "Errors were encountered while migrating custom transaction categories. Check the logs for more info.", Alert.AlertType.ERROR);
                            return;
                        } else {
                            Optional<ButtonType> response = PopupManager.showConfirmationAsync("Proceed?", "Transaction categories have been successfully copied. Proceed to copy transactions?",
                                    new ButtonType("Yes, proceed", ButtonBar.ButtonData.APPLY),
                                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                            );

                            if (response.isEmpty() || response.get().getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) {
                                try {
                                    currentConnection.rollback();
                                    return;
                                } catch (SQLException e) {
                                    PopupManager.showPopup("Failed to roll back!", "An SQL error was encountered when rolling back. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                                return;
                            }

                            try {
                                currentConnection.commit();
                            } catch (SQLException e) {
                                PopupManager.showPopup("Failed to commit!", "An SQL error was encountered when committing changes. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        }

                        dlg.setSecondaryLabel("Copying transactions...");
                        boolean transactionsCopied = copyTransactions(dlg);
                        if (!transactionsCopied) {
                            PopupManager.showPopup("Transaction migration failed!", "Errors were encountered while migrating transactions. Check the logs for more info.", Alert.AlertType.ERROR);
                            return;
                        } else {
                            Optional<ButtonType> response = PopupManager.showConfirmationAsync("Proceed?", "Transactions have been successfully copied. Proceed to copy accounts?",
                                    new ButtonType("Yes, proceed", ButtonBar.ButtonData.APPLY),
                                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                            );

                            if (response.isEmpty() || response.get().getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) {
                                try {
                                    currentConnection.rollback();
                                    return;
                                } catch (SQLException e) {
                                    PopupManager.showPopup("Failed to roll back!", "An SQL error was encountered when rolling back. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                                return;
                            }

                            try {
                                currentConnection.commit();
                            } catch (SQLException e) {
                                PopupManager.showPopup("Failed to commit!", "An SQL error was encountered when committing changes. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        }

                        dlg.setSecondaryLabel("Copying accounts...");
                        boolean accountsCopied = copyAccounts(dlg);
                        if (!accountsCopied) {
                            PopupManager.showPopup("Account migration failed!", "Errors were encountered while migrating accounts. Check the logs for more info.", Alert.AlertType.ERROR);
                            return;
                        } else {
                            Optional<ButtonType> response = PopupManager.showConfirmationAsync("Proceed?", "Accounts have been successfully copied. Proceed to clean up?",
                                    new ButtonType("Yes, proceed", ButtonBar.ButtonData.APPLY),
                                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
                            );

                            if (response.isEmpty() || response.get().getButtonData().equals(ButtonBar.ButtonData.CANCEL_CLOSE)) {
                                try {
                                    currentConnection.rollback();
                                    return;
                                } catch (SQLException e) {
                                    PopupManager.showPopup("Failed to roll back!", "An SQL error was encountered when rolling back. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                            }

                            try {
                                currentConnection.commit();
                            } catch (SQLException e) {
                                PopupManager.showPopup("Failed to commit!", "An SQL error was encountered when committing changes. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        }
                        success.set(true);
                    });

                    if (success.get()) {
                        PopupManager.showPopup("Migration finished!", "All data has been successfully copied to the SQLite DB.\n\nFor the changes to take effect, please restart Income Utility.", Alert.AlertType.INFORMATION);
                    } else {
                        PopupManager.showPopup("Migration failed!", "Migration was terminated due to one or more errors. Check logs for more info.", Alert.AlertType.ERROR);
                    }
                });
            }

        });
    }

    @SuppressWarnings("ConstantConditions")
    private static int getCustomCategoryId(Connection connection, String customCategory) {
        if (connection == null) {
            throw new InvalidParameterException("Connection cannot be null!");
        }

        PreparedStatement stmt;
        try {
            stmt = connection.prepareStatement("SELECT id FROM transaction_categories WHERE displayName = ?;");
            stmt.setString(1, customCategory);
            ResultSet response = stmt.executeQuery();
            return response.getInt("id");
        } catch (SQLException e) {
            log("Failed to prepare SQL statement! Error: " + e.getMessage(), Level.SEVERE);
            e.printStackTrace();
            return -1;
        }
    }

    private boolean copyCategories(LoadingDialog progressDialog) {
        PreparedStatement stmt = null;
        try {
            stmt = currentConnection.prepareStatement("INSERT INTO transaction_categories (displayName) VALUES (?);");
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during category migration!", "Failed to construct query! Error: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }

        try {
            currentConnection.prepareStatement("INSERT INTO transaction_categories (id, displayName) VALUES (0, 'No custom category');").execute();

            for (String category : currentData.getCustomTransactionCategories()) {
                progressDialog.setSecondaryLabel("Copying transaction category: " + category);
                stmt.setString(1, category);
                stmt.execute();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during category migration!", "The following error occurred when migrating custom transaction categories: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    private boolean copyAccounts(LoadingDialog progressDialog) {
        PreparedStatement stmt = null;
        try {
            stmt = currentConnection.prepareStatement("INSERT INTO accounts (uuid, name, initialBalance, type, isDefault, currencySymbol, currencyIsPrefixed) VALUES (?, ?, ?, ?, ?, ?, ?);");
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during account migration!", "Failed to construct query! Error: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }

        try {
            for (Account account : currentData.getAccounts().values()) {
                progressDialog.setSecondaryLabel("Copying account: " + account.getName());
                stmt.setString(1, account.getId().toString());
                stmt.setString(2, account.getName());
                stmt.setDouble(3, account.getInitialBalance());
                stmt.setString(4, account.getType().name());
                stmt.setBoolean(5, account.isDefault());
                stmt.setString(6, account.getCurrencyConfig().getCurrencySymbol());
                stmt.setBoolean(7, account.getCurrencyConfig().isCurrencySymbolPrefix());
                stmt.execute();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during account migration!", "The following error occurred when migrating accounts: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    private boolean copyTransactions(LoadingDialog progressDialog) {
        PreparedStatement transactionStatement;
        try {
            transactionStatement = currentConnection.prepareStatement(
                    "INSERT INTO transactions (uuid, cashewTransactionId, type, amount, sourceAccountId, targetAccountId, cashewSourceAccountId, cashewTargetAccountId, category, customCategoryId, comment, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            );
        } catch (SQLException e) {
            Platform.runLater(() -> {
                PopupManager.showPopup("SQL exception during transaction migration!", "Failed to construct query! Error: " + e.getMessage(), Alert.AlertType.ERROR);
            });
            return false;
        }

        try {
            for (Map.Entry<UUID, Transaction> t : currentData.transactions.entrySet()) {
                progressDialog.setSecondaryLabel("Copying transaction: " + t.getKey());
                if (t.getValue() instanceof CashewTransaction asCashew) {
                    transactionStatement.setString(1, asCashew.getId().toString());
                    transactionStatement.setString(2, asCashew.getCashewTransactionId());
                    transactionStatement.setString(3, asCashew.getType().toString());
                    transactionStatement.setDouble(4, asCashew.getAmount());
                    transactionStatement.setString(5, asCashew.getSourceAccountId() != null ? asCashew.getSourceAccountId().toString() : null);
                    transactionStatement.setString(6, asCashew.getTargetAccountId() != null ? asCashew.getTargetAccountId().toString() : null);
                    transactionStatement.setString(7, asCashew.getCashewSourceAccount());
                    transactionStatement.setString(8, asCashew.getCashewTargetAccount());
                    transactionStatement.setString(9, asCashew.getCategory().name());
                    transactionStatement.setInt(10, getCustomCategoryId(currentConnection, asCashew.getCustomCategory()));
                    transactionStatement.setString(11, asCashew.getComment());
                    transactionStatement.setString(12, Timestamp.valueOf(asCashew.getTimestamp()).toString());
                } else {
                    transactionStatement.setString(1, t.getValue().getId().toString());
                    transactionStatement.setString(2, null);
                    transactionStatement.setString(3, t.getValue().getType().toString());
                    transactionStatement.setDouble(4, t.getValue().getAmount());
                    transactionStatement.setString(5, t.getValue().getSourceAccountId() != null ? t.getValue().getSourceAccountId().toString() : null);
                    transactionStatement.setString(6, t.getValue().getTargetAccountId() != null ? t.getValue().getTargetAccountId().toString() : null);
                    transactionStatement.setString(7, null);
                    transactionStatement.setString(8, null);
                    transactionStatement.setString(9, t.getValue().getCategory().name());
                    transactionStatement.setInt(10, getCustomCategoryId(currentConnection, t.getValue().getCustomCategory()));
                    transactionStatement.setString(11, t.getValue().getComment());
                    transactionStatement.setString(12, Timestamp.valueOf(t.getValue().getTimestamp()).toString());
                }
                transactionStatement.execute();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        } catch (SQLException e) {
            Platform.runLater(() -> {
                PopupManager.showPopup("SQL exception transaction migration!", "The following error occurred when migrating transactions: " + e.getMessage(), Alert.AlertType.ERROR);
            });
            return false;
        }
    }

    public static boolean copyTransaction(Transaction t, Connection database) {
        PreparedStatement transactionStatement;
        PreparedStatement categoryStatement;
        try {
            transactionStatement = database.prepareStatement(
                    "INSERT INTO transactions (uuid, cashewTransactionId, type, amount, sourceAccountId, targetAccountId, cashewSourceAccountId, cashewTargetAccountId, category, customCategoryId, comment, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            );
            categoryStatement = database.prepareStatement("INSERT INTO transaction_categories (displayName) VALUES (?);");
        } catch (SQLException e) {
            System.err.println("Error copying transaction '" + t.getId() + "' - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        try {
            if (t.getCategory().equals(TransactionCategory.CUSTOM)) {
                categoryStatement.setString(1, t.getCustomCategory());
                categoryStatement.execute();
            }

            if (t instanceof CashewTransaction asCashew) {
                transactionStatement.setString(1, asCashew.getId().toString());
                transactionStatement.setString(2, asCashew.getCashewTransactionId());
                transactionStatement.setString(3, asCashew.getType().toString());
                transactionStatement.setDouble(4, asCashew.getAmount());
                transactionStatement.setString(5, asCashew.getSourceAccountId() != null ? asCashew.getSourceAccountId().toString() : null);
                transactionStatement.setString(6, asCashew.getTargetAccountId() != null ? asCashew.getTargetAccountId().toString() : null);
                transactionStatement.setString(7, asCashew.getCashewSourceAccount());
                transactionStatement.setString(8, asCashew.getCashewTargetAccount());
                transactionStatement.setString(9, asCashew.getCategory().name());
                transactionStatement.setInt(10, getCustomCategoryId(database, asCashew.getCustomCategory()));
                transactionStatement.setString(11, asCashew.getComment());
                transactionStatement.setString(12, Timestamp.valueOf(asCashew.getTimestamp()).toString());
            } else {
                transactionStatement.setString(1, t.getId().toString());
                transactionStatement.setString(2, null);
                transactionStatement.setString(3, t.getType().toString());
                transactionStatement.setDouble(4, t.getAmount());
                transactionStatement.setString(5, t.getSourceAccountId() != null ? t.getSourceAccountId().toString() : null);
                transactionStatement.setString(6, t.getTargetAccountId() != null ? t.getTargetAccountId().toString() : null);
                transactionStatement.setString(7, null);
                transactionStatement.setString(8, null);
                transactionStatement.setString(9, t.getCategory().name());
                transactionStatement.setInt(10, getCustomCategoryId(database, t.getCustomCategory()));
                transactionStatement.setString(11, t.getComment());
                transactionStatement.setString(12, Timestamp.valueOf(t.getTimestamp()).toString());
            }
            transactionStatement.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("Error copying transaction '" + t.getId() + "' - " + e.getMessage());
            e.printStackTrace();
            return false;
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
        Data d = getData();
        return d.getLastActiveAccountId();
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
        if (currentData == null) {
            initialize();
        }
        currentData.lastActiveAccountId = account.getId().toString();
    }
    //</editor-fold>

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

    private static class Data {
        private final HashMap<UUID, Transaction> transactions;
        private final HashMap<UUID, Account> accounts;
        private final ArrayList<String> customTransactionCategories;
        private String lastActiveAccountId;

        public Data() {
            this.transactions = new HashMap<>();
            this.accounts = new HashMap<>();
            this.customTransactionCategories = new ArrayList<>();
            this.lastActiveAccountId = "";
        }

        public HashMap<UUID, Transaction> getTransactions() {
            return transactions;
        }

        public HashMap<UUID, Account> getAccounts() {
            return accounts;
        }

        public ArrayList<String> getCustomTransactionCategories() {
            return customTransactionCategories;
        }

        public Optional<UUID> getLastActiveAccountId() {
            return lastActiveAccountId.isEmpty() ? Optional.empty() : Optional.of(UUID.fromString(lastActiveAccountId));
        }
    }
}
