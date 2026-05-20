package com.krisapps.incomeutility_v2.util.services;

import com.krisapps.incomeutility_v2.dialogs.generic.LoadingDialog;
import com.krisapps.incomeutility_v2.exceptions.NotInitializedException;
import com.krisapps.incomeutility_v2.types.data.LegacyData;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.security.InvalidParameterException;
import java.sql.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class MigrationService {

    private static MigrationService instance;
    private final Connection databaseConnection;

    public static MigrationService initialize(Connection databaseConnection) {
        if (instance == null) {
            instance = new MigrationService(databaseConnection);
        }
        return instance;
    }

    public static void shutdown() {
        if (instance == null) {
            throw new NotInitializedException("Service not initialized! Please call MigrationService#initialize(...) before using this utility!");
        }

        try {
            instance.databaseConnection.commit();
            log("Committed all transactions", Level.INFO);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private MigrationService(Connection databaseConnection) {
        this.databaseConnection = databaseConnection;
    }

    private static void log(String msg, Level level) {
        DataManager.log("[Migration] " + msg, level);
    }

    public void migrateFromFile(Path pathToFile) throws FileNotFoundException {
        if (!pathToFile.toFile().exists()) {
            throw new FileNotFoundException("No data file found at: " + pathToFile + "!");
        }

        LegacyData data = DataManager.getInstance().getLegacyData(pathToFile);

        try {
            databaseConnection.setAutoCommit(false);
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
                        boolean categoriesCopied = copyCategories(data.getCustomTransactionCategories(), dlg);
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
                                    databaseConnection.rollback();
                                    return;
                                } catch (SQLException e) {
                                    PopupManager.showPopup("Failed to roll back!", "An SQL error was encountered when rolling back. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                                return;
                            }

                            try {
                                databaseConnection.commit();
                            } catch (SQLException e) {
                                PopupManager.showPopup("Failed to commit!", "An SQL error was encountered when committing changes. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        }

                        dlg.setSecondaryLabel("Copying transactions...");
                        boolean transactionsCopied = copyTransactions(data.getTransactions().values().stream().toList(), dlg);
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
                                    databaseConnection.rollback();
                                    return;
                                } catch (SQLException e) {
                                    PopupManager.showPopup("Failed to roll back!", "An SQL error was encountered when rolling back. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                                return;
                            }

                            try {
                                databaseConnection.commit();
                            } catch (SQLException e) {
                                PopupManager.showPopup("Failed to commit!", "An SQL error was encountered when committing changes. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                            }
                        }

                        dlg.setSecondaryLabel("Copying accounts...");
                        boolean accountsCopied = copyAccounts(data.getAccounts().values().stream().toList(), dlg);
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
                                    databaseConnection.rollback();
                                    return;
                                } catch (SQLException e) {
                                    PopupManager.showPopup("Failed to roll back!", "An SQL error was encountered when rolling back. Error: " + e.getMessage(), Alert.AlertType.ERROR);
                                }
                            }

                            try {
                                databaseConnection.commit();
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

    public void migrateFromDefaultFile() throws FileNotFoundException {
        Path defaultPath = Path.of(System.getProperty("user.home") + File.separator + "IncomeUtility v2" + File.separator + "data.json");

        if (!defaultPath.toFile().exists()) {
            throw new FileNotFoundException("No data file found at " + defaultPath + "! Data file has to be named 'data.json'");
        }

        migrateFromFile(defaultPath);
    }

    @SuppressWarnings("ConstantConditions")
    public int getCustomCategoryId(String customCategory) {
        if (databaseConnection == null) {
            throw new InvalidParameterException("No database connection!");
        }

        PreparedStatement stmt;
        try {
            stmt = databaseConnection.prepareStatement("SELECT id FROM transaction_categories WHERE displayName = ?;");
            stmt.setString(1, customCategory);
            ResultSet response = stmt.executeQuery();
            return response.getInt("id");
        } catch (SQLException e) {
            log("Failed to prepare SQL statement to retrieve custom category id! Error: " + e.getMessage(), Level.SEVERE);
            e.printStackTrace();
            return -1;
        }
    }

    public boolean copyCategory(String customCategory, @Nullable LoadingDialog progressDialog) {
        PreparedStatement stmt;
        try {
            stmt = databaseConnection.prepareStatement("INSERT INTO transaction_categories (displayName) VALUES (?);");
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during category migration!", "Failed to construct query! Error: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }

        try {
            if (progressDialog != null) {
                progressDialog.setSecondaryLabel("Copying transaction category: " + customCategory);
            }
            stmt.setString(1, customCategory);
            stmt.execute();
            return true;
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during category migration!", "The following error occurred when migrating custom transaction category '" + customCategory + "': " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    public boolean copyCategories(List<String> categories, @Nullable LoadingDialog progressDialog) {
        try {
            databaseConnection.prepareStatement("INSERT INTO transaction_categories (id, displayName) VALUES (0, 'No custom category');").execute();
            for (String category : categories) {
                boolean success = copyCategory(category, progressDialog);
                if (!success) {
                    log("Failed to copy category '" + category + "'", Level.WARNING);
                }

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

    public boolean copyAccount(Account account, @Nullable LoadingDialog progressDialog) {
        PreparedStatement stmt;
        try {
            stmt = databaseConnection.prepareStatement("INSERT INTO accounts (uuid, name, initialBalance, type, isDefault, currencySymbol, currencyIsPrefixed) VALUES (?, ?, ?, ?, ?, ?, ?);");
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during account migration!", "Failed to construct query! Error: " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }

        try {
            if (progressDialog != null) {
                progressDialog.setSecondaryLabel("Copying account: " + account.getName());
            }
            stmt.setString(1, account.getId().toString());
            stmt.setString(2, account.getName());
            stmt.setDouble(3, account.getInitialBalance());
            stmt.setString(4, account.getType().name());
            stmt.setBoolean(5, account.isDefault());
            stmt.setString(6, account.getCurrencyConfig().getCurrencySymbol());
            stmt.setBoolean(7, account.getCurrencyConfig().isCurrencySymbolPrefix());
            stmt.execute();
            return true;
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during account migration!", "The following error occurred when migrating account ' " + account.getId() + "': " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    public boolean copyAccounts(List<Account> accounts, @Nullable LoadingDialog progressDialog) {
        for (Account account : accounts) {
            boolean success = copyAccount(account, progressDialog);
            if (!success) {
                log("Failed to copy account: '" + account.getId() + "'", Level.WARNING);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public boolean copyTransaction(Transaction t, @Nullable LoadingDialog progressDialog) {
        if (databaseConnection == null) {
            throw new InvalidParameterException("No database connection!");
        }

        PreparedStatement transactionStatement;
        try {
            transactionStatement = databaseConnection.prepareStatement(
                    "INSERT INTO transactions (uuid, cashewTransactionId, type, amount, sourceAccountId, targetAccountId, cashewSourceAccountId, cashewTargetAccountId, category, customCategoryId, comment, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
            );
        } catch (SQLException e) {
            Platform.runLater(() -> {
                PopupManager.showPopup("SQL exception during transaction migration!", "Failed to construct query! Error: " + e.getMessage(), Alert.AlertType.ERROR);
            });
            return false;
        }

        try {
            if (progressDialog != null) {
                progressDialog.setSecondaryLabel("Copying transaction: " + t.getId());
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
                transactionStatement.setInt(10, getCustomCategoryId(asCashew.getCustomCategory()));
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
                transactionStatement.setInt(10, getCustomCategoryId(t.getCustomCategory()));
                transactionStatement.setString(11, t.getComment());
                transactionStatement.setString(12, Timestamp.valueOf(t.getTimestamp()).toString());
            }
            transactionStatement.execute();
            return true;
        } catch (SQLException e) {
            PopupManager.showPopup("SQL exception during transaction migration!", "The following error occurred when migrating transaction '" + t.getId() + "' - " + e.getMessage(), Alert.AlertType.ERROR);
            return false;
        }
    }

    public boolean copyTransactions(List<? extends Transaction> transactions, @Nullable LoadingDialog progressDialog) {
        for (Transaction t : transactions) {
            boolean success = copyTransaction(t, progressDialog);
            if (!success) {
                log("Failed to copy transaction: '" + t.getId() + "'", Level.WARNING);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return true;
    }

}
