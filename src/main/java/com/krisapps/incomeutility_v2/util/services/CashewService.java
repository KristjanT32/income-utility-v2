package com.krisapps.incomeutility_v2.util.services;

import com.krisapps.incomeutility_v2.dialogs.AccountMappingDialog;
import com.krisapps.incomeutility_v2.dialogs.LoadingDialog;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewAccount;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.charset.UnmappableCharacterException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.*;
import java.util.*;

public class CashewService {

    private static CashewService instance;
    private File file = null;

    private CashewService() {

    }

    public static CashewService getInstance() {
        if (instance == null) {
            instance = new CashewService();
        }
        return instance;
    }

    /**
     * Loads the specified database file, preparing the utility for data access.
     * @param pathToDatabaseFile The path to the database file (.sql / .sqlite)
     * @throws FileNotFoundException If the specified path does not lead to a valid file.
     */
    public void initialize(URI pathToDatabaseFile) throws FileNotFoundException {
        if (!Files.exists(Path.of(pathToDatabaseFile))) {
            throw new FileNotFoundException("Couldn't find database file at path: " + pathToDatabaseFile);
        }

        file = new File(pathToDatabaseFile.getPath());
    }

    /**
     * Returns <code>true</code> if the service has been initialized with a valid database file and is thus ready for data querying.
     * Returns <code>false</code> otherwise.
     * @return <code>true</code> if the service is ready, <code>false</code> otherwise
     */
    public boolean isReady() {
        return file != null;
    }

    public Optional<CashewAccount> getWalletByName(String accountName) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
            PreparedStatement statement = connection.prepareStatement("SELECT name, wallet_pk FROM wallets WHERE name = ?");
            statement.setString(1, accountName);

            ResultSet resultSet = statement.executeQuery();
            CashewAccount account = new CashewAccount(resultSet.getString("name"), resultSet.getString("wallet_pk"));
            return Optional.of(account);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    public Optional<CashewAccount> getWalletById(String id) {
        System.out.println("Looking for wallet with ID: " + id);
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
            PreparedStatement statement = connection.prepareStatement("SELECT name, wallet_pk FROM wallets WHERE wallet_pk = ?");
            statement.setString(1, id);

            ResultSet resultSet = statement.executeQuery();
            CashewAccount account = new CashewAccount(resultSet.getString("name"), resultSet.getString("wallet_pk"));
            return Optional.of(account);
        } catch (SQLException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets the wallet ID associated with the transaction with the supplied ID.
     * @param transactionId The ID of the transaction
     * @return The wallet ID
     */
    public String getWalletFromTransaction(String transactionId) throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
        PreparedStatement statement = connection.prepareStatement("SELECT wallet_fk FROM transactions WHERE transaction_pk = ?");
        statement.setString(1, transactionId);
        return statement.executeQuery().getString("wallet_fk");
    }

    /**
     * Maps a Cashew transaction entry to a {@link CashewTransaction} object.
     * @param row The resultset from which to access the row data.
     * @return A CashewTransaction object for the supplied transaction row.
     * @throws SQLException
     */
    public CashewTransaction mapToTransaction(ResultSet row) throws SQLException {
        CashewTransaction transaction = new CashewTransaction(row.getString("transaction_pk"));
        transaction.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(row.getString("date_created"))), ZoneId.systemDefault()));
        transaction.setAmount(row.getDouble("amount"));
        transaction.setComment(row.getString("name"));
        transaction.setCategory(TransactionCategory.CUSTOM);
        transaction.setCustomCategory(row.getString("category_name"));

        if (row.getString("paired_transaction_fk") != null) {
            transaction.setType(TransactionType.TRANSFER);

            if (row.getDouble("amount") > 0) {
                transaction.setCashewSourceAccount(getWalletFromTransaction(row.getString("paired_transaction_fk")));
                transaction.setCashewTargetAccount(row.getString("wallet_fk"));
            } else {
                transaction.setCashewSourceAccount(row.getString("wallet_fk"));
                transaction.setCashewTargetAccount(getWalletFromTransaction(row.getString("paired_transaction_fk")));
            }
        } else {
            if (row.getDouble("amount") > 0) {
                transaction.setType(TransactionType.DEPOSIT);
            } else {
                transaction.setType(TransactionType.WITHDRAWAL);
            }
            transaction.setCashewTargetAccount(row.getString("wallet_fk"));
        }
        return transaction;
    }

    public ArrayList<CashewAccount> getWallets() {
        ArrayList<CashewAccount> wallets = new ArrayList<>();

        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());

            ResultSet results = connection.createStatement().executeQuery("SELECT name, wallet_pk FROM wallets");
            while (results.next()) {
                wallets.add(new CashewAccount(results.getString("name"), results.getString("wallet_pk")));
            }
        } catch (SQLException e) {
            return new ArrayList<>();
        }
        return wallets;
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> categories = new ArrayList<>();

        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());

            ResultSet results = connection.createStatement().executeQuery("SELECT * FROM categories");
            while (results.next()) {
                categories.add(results.getString("name"));
            }
        } catch (SQLException e) {
            return new ArrayList<>();
        }
        return categories;
    }

    public String getCategoryById(int id) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());

            PreparedStatement statement = connection.prepareStatement("SELECT * FROM categories WHERE category_pk = ?");
            statement.setInt(1, id);

            ResultSet results = statement.executeQuery();
            return results.getString("name");
        } catch (SQLException e) {
            return "Unknown category";
        }
    }

    /**
     * Retrieves transactions from the currently loaded database file from the supplied account.
     * <p>
     * - If both <code>startDateInclusive</code> and <code>endDateInclusive</code> are non-null,
     * only transactions falling into the resulting inclusive range will be returned.
     * </p>
     * <p>
     * - If only <code>startDateInclusive</code> is provided, only transactions recorded after
     * the supplied date (inclusive) will be returned.
     * </p>
     * <p>
     * - If only <code>endDateInclusive</code> is provided, all transactions up to the supplied
     * date (inclusive) will be supplied.
     * </p>
     * <p>
     * - If both date arguments are null, all transactions of the supplied account will be returned.
     * </p>
     * @param account The Cashew account whose transactions to return.
     * @param startDateInclusive The start date of the inclusive range of transactions. May be null.
     * @param endDateInclusive The end of the inclusive range of transactions. May be null.
     * @return A list of transactions matching the supplied criteria.
     */
    public ArrayList<CashewTransaction> getTransactions(CashewAccount account, @Nullable LocalDate startDateInclusive, @Nullable LocalDate endDateInclusive) {
        ArrayList<CashewTransaction> transactions = new ArrayList<>();
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.getPath());
            LoadingDialog dialog = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_PROGRESSBAR);
            dialog.setPrimaryLabel("Importing transactions for " + account.displayName());
            dialog.setSecondaryLabel("Reading database file");
            dialog.show("Importing transactions", new Runnable() {
                @Override
                public void run() {
                    try {
                        PreparedStatement statement;
                        if (startDateInclusive != null && endDateInclusive != null) {
                            System.out.println("Retrieving transactions between supplied dates");
                            statement = connection.prepareStatement("SELECT * FROM transactions JOIN (SELECT name AS category_name, category_pk AS category_pk FROM categories) ON transactions.category_fk = category_pk WHERE transactions.wallet_fk = ? AND (DATETIME(date_created, 'unixepoch') BETWEEN DATETIME(? / 1000, 'unixepoch') AND DATETIME(? / 1000, 'unixepoch'))");
                            statement.setString(1, account.id());
                            statement.setLong(2, startDateInclusive.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                            statement.setLong(3, endDateInclusive.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        } else if (startDateInclusive != null && endDateInclusive == null) {
                            System.out.println("Retrieving transactions after supplied date");
                            statement = connection.prepareStatement("SELECT * FROM transactions JOIN (SELECT name AS category_name, category_pk AS category_pk FROM categories) ON transactions.category_fk = category_pk WHERE transactions.wallet_fk = ? AND DATETIME(date_created, 'unixepoch') >= DATETIME(? / 1000, 'unixepoch')");
                            statement.setString(1, account.id());
                            statement.setLong(2, startDateInclusive.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        } else if (startDateInclusive == null && endDateInclusive != null) {
                            System.out.println("Retrieving transactions before supplied date");
                            statement = connection.prepareStatement("SELECT * FROM transactions JOIN (SELECT name AS category_name, category_pk AS category_pk FROM categories) ON transactions.category_fk = category_pk WHERE transactions.wallet_fk = ? AND DATETIME(date_created, 'unixepoch') <= DATETIME(? / 1000, 'unixepoch')");
                            statement.setString(1, account.id());
                            statement.setLong(2, endDateInclusive.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
                        } else {
                            statement = connection.prepareStatement("SELECT * FROM transactions JOIN (SELECT name AS category_name, category_pk AS category_pk FROM categories) ON transactions.category_fk = category_pk WHERE transactions.wallet_fk = ?");
                            statement.setString(1, account.id());
                        }
                        ResultSet results = statement.executeQuery();
                        int transactionCount = 1;

                        while (results.next()) {
                            if (results.getString("transaction_pk").contains("::predict::1")) {
                                dialog.setSecondaryLabel("Skipping future #" + transactionCount);
                                continue;
                            } else {
                                dialog.setSecondaryLabel("Importing transaction #" + transactionCount++);
                            }
                            transactions.add(mapToTransaction(results));
                        }
                    } catch (SQLException e) {
                        Platform.runLater(() -> {
                            PopupManager.showPopup("Error whilst importing transactions", e.getMessage(), Alert.AlertType.ERROR);
                        });
                    }
                }
            });
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return transactions;
    }
}
