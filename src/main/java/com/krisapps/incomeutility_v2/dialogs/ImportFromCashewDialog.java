package com.krisapps.incomeutility_v2.dialogs;

import com.krisapps.incomeutility_v2.IncomeUtilityApplication;
import com.krisapps.incomeutility_v2.subutilities.money_flow.MoneyFlowUtilityController;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewAccount;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import com.krisapps.incomeutility_v2.util.services.CashewService;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

public class ImportFromCashewDialog extends IncomeUtilityDialog<Pair<Account, ArrayList<CashewTransaction>>> {

    @FXML
    private VBox rootPane;

    @FXML
    private ComboBox<String> sourceAccountPicker;

    @FXML
    private ComboBox<String> targetAccountPicker;

    @FXML
    private ComboBox<DateFilteringMode> filterTypeSelector;

    @FXML
    private DatePicker startPicker;

    @FXML
    private DatePicker endPicker;

    @FXML
    private Button filePickerButton;

    @FXML
    private Label datePickerSeparator;

    @FXML
    private Button importButton;

    @FXML
    private Button reviewButton;

    private FileChooser filePicker = new FileChooser();
    private CashewService cashew = CashewService.getInstance();
    private FiscalService fiscal = FiscalService.getInstance();

    private ArrayList<CashewTransaction> importedTransactions = new ArrayList<>();

    private enum DateFilteringMode {
        NONE("All transactions (no filtering)"),
        RANGE("All transactions between (inclusive)"),
        ALL_BEFORE("All transactions before (inclusive)"),
        ALL_AFTER("All transactions after (inclusive)")
        ;
        private String displayName;

        DateFilteringMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static DateFilteringMode ofDisplayName(String displayName) {
            for (DateFilteringMode mode: values()) {
                if (mode.getDisplayName().equals(displayName)) {
                    return mode;
                }
            }
            return NONE;
        }
    }

    public ImportFromCashewDialog(Account selectedAccount) {
        super("import-transactions.fxml", "Import transactions");

        FileChooser.ExtensionFilter filter = new FileChooser.ExtensionFilter("Database files", "*.sql", "*.sqlite");
        filePicker.getExtensionFilters().add(filter);
        filePicker.setTitle("Select database file to import transactions from");

        startPicker.setConverter(Formats.DATE_FORMAT);
        endPicker.setConverter(Formats.DATE_FORMAT);
        startPicker.managedProperty().bind(startPicker.visibleProperty());
        endPicker.managedProperty().bind(endPicker.visibleProperty());
        datePickerSeparator.visibleProperty().bind(startPicker.visibleProperty().and(endPicker.visibleProperty()));
        datePickerSeparator.managedProperty().bind(datePickerSeparator.visibleProperty());

        filterTypeSelector.getItems().setAll(DateFilteringMode.values());
        filterTypeSelector.setConverter(new StringConverter<DateFilteringMode>() {
            @Override
            public String toString(DateFilteringMode dateFilteringMode) {
                return dateFilteringMode.getDisplayName();
            }

            @Override
            public DateFilteringMode fromString(String s) {
                return DateFilteringMode.ofDisplayName(s);
            }
        });
        filterTypeSelector.valueProperty().addListener((obs, old, val) -> {
            switch (val) {
                case NONE -> {
                    startPicker.setValue(null);
                    startPicker.setVisible(false);

                    endPicker.setValue(null);
                    endPicker.setVisible(false);
                }
                case RANGE -> {
                    startPicker.setValue(LocalDate.now());
                    startPicker.setVisible(true);

                    endPicker.setValue(LocalDate.now());
                    endPicker.setVisible(true);
                }
                case ALL_BEFORE -> {
                    startPicker.setValue(null);
                    startPicker.setVisible(false);

                    endPicker.setValue(LocalDate.now());
                    endPicker.setVisible(true);
                }
                case ALL_AFTER -> {
                    startPicker.setValue(LocalDate.now());
                    startPicker.setVisible(true);

                    endPicker.setValue(null);
                    endPicker.setVisible(false);
                }
            }
        });

        // Set defaults
        targetAccountPicker.setValue(selectedAccount.getName());
        filterTypeSelector.setValue(DateFilteringMode.NONE);

        importButton.setOnAction((ev) -> {
            if (!importedTransactions.isEmpty()) {
                PopupManager.showChoicePopup(
                        "Reimport transactions?",
                        "Some transactions have already been imported.\nWould you like to import them again? This can be useful if you've made changes to the database file externally.",
                    new ButtonType("Reimport", ButtonBar.ButtonData.APPLY),
                    new ButtonType("Don't reimport", ButtonBar.ButtonData.CANCEL_CLOSE)
                ).ifPresent(response -> {
                    if (response.getButtonData() == ButtonBar.ButtonData.APPLY) {
                        importedTransactions = new ArrayList<>();
                        importedTransactions = cashew.getTransactions(cashew.getWalletByName(sourceAccountPicker.getValue()).get(), startPicker.getValue(), endPicker.getValue());
                        refreshUI();
                    }
                });
            } else {
                importedTransactions = cashew.getTransactions(cashew.getWalletByName(sourceAccountPicker.getValue()).get(), startPicker.getValue(), endPicker.getValue());
                refreshUI();
            }

            if (importedTransactions.isEmpty()) {
                PopupManager.showPopup("No transactions found", "No transactions matched the specified criteria.", Alert.AlertType.INFORMATION);
                return;
            }

            PopupManager.showChoicePopup(
                    "Conversion complete",
                    "All selected (%s) transactions have been converted into the local format.\n\nProceed with import?".formatted(importedTransactions.size()),
                    new ButtonType("Yes, import all", ButtonBar.ButtonData.APPLY),
                    new ButtonType("Review transactions", ButtonBar.ButtonData.OTHER),
                    new ButtonType("No, cancel", ButtonBar.ButtonData.CANCEL_CLOSE)
            ).ifPresent(response -> {
                switch (response.getButtonData()) {
                    case APPLY -> {
                        importTransactions();
                        close();
                    }
                    case OTHER -> {
                        reviewTransactions();
                    }
                    default -> {

                    }
                }
            });
        });

        reviewButton.setOnAction((ev) -> {
            reviewTransactions();
        });

        setResultConverter((buttonType -> {
            if (buttonType.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
                return null;
            } else {
                return getResult();
            }
        }));

        refreshUI();
    }

    private void importTransactions() {
        HashMap<String, UUID> accountMappings = new HashMap<>();
        AccountMappingDialog dlg = new AccountMappingDialog();

        DataManager.log("Beginning to import transactions");
        for (CashewTransaction t: importedTransactions) {
            if (t.getType().equals(TransactionType.TRANSFER)) {
                if (accountMappings.containsKey(t.getCashewSourceAccount())) {
                    t.setSourceAccountId(accountMappings.get(t.getCashewSourceAccount()));
                } else {
                    Optional<CashewAccount> wallet = cashew.getWalletById(t.getCashewSourceAccount());
                    if (wallet.isEmpty()) {
                        PopupManager.showPopup("Invalid wallet entry!", "Wallet ID '" + t.getCashewSourceAccount() + "' does not reference a valid wallet.\nMaybe the wallet was deleted?", Alert.AlertType.ERROR);
                        continue;
                    }
                    dlg.setExternalAccountName(wallet.get().displayName());
                    dlg.setTitle("Transaction account mapping required: " + t.getCashewTransactionId());
                    Optional<UUID> mapping = dlg.showAndWait();
                    mapping.ifPresent(uuid -> {
                        accountMappings.put(t.getCashewSourceAccount(), uuid);
                        t.setSourceAccountId(uuid);
                    });
                }
                if (accountMappings.containsKey(t.getCashewTargetAccount())) {
                    t.setTargetAccountId(accountMappings.get(t.getCashewTargetAccount()));
                } else {
                    Optional<CashewAccount> wallet = cashew.getWalletById(t.getCashewTargetAccount());
                    if (wallet.isEmpty()) {
                        PopupManager.showPopup("Invalid wallet entry!", "Wallet ID '" + t.getCashewTargetAccount() + "' does not reference a valid wallet.\nMaybe the wallet was deleted?", Alert.AlertType.ERROR);
                        continue;
                    }
                    dlg.setExternalAccountName(wallet.get().displayName());
                    dlg.setTitle("Transaction account mapping required: " + t.getCashewTransactionId());
                    Optional<UUID> mapping = dlg.showAndWait();
                    mapping.ifPresent(uuid -> {
                        accountMappings.put(t.getCashewTargetAccount(), uuid);
                        t.setTargetAccountId(uuid);
                    });
                }
            } else {
                if (accountMappings.containsKey(t.getCashewTargetAccount())) {
                    t.setTargetAccountId(accountMappings.get(t.getCashewTargetAccount()));
                } else {
                    Optional<CashewAccount> wallet = cashew.getWalletById(t.getCashewTargetAccount());
                    if (wallet.isEmpty()) {
                        PopupManager.showPopup("Invalid wallet entry!", "Wallet ID '" + t.getCashewTargetAccount() + "' does not reference a valid wallet.\nMaybe the wallet was deleted?", Alert.AlertType.ERROR);
                        continue;
                    }
                    dlg.setExternalAccountName(wallet.get().displayName());
                    dlg.setTitle("Transaction account mapping required: " + t.getCashewTransactionId());
                    Optional<UUID> mapping = dlg.showAndWait();
                    mapping.ifPresent(uuid -> {
                        accountMappings.put(t.getCashewTargetAccount(), uuid);
                        t.setTargetAccountId(uuid);
                    });
                }
            }
        }
        DataManager.log("Import completed.");

        setResult(new Pair<>(fiscal.getAccountByName(targetAccountPicker.getValue()).get(), importedTransactions));
    }

    private void reviewTransactions() {
        PreImportTransactionReviewDialog dlg = new PreImportTransactionReviewDialog(importedTransactions);
        Optional<ArrayList<CashewTransaction>> filteredTransactions = dlg.showAndWait();

        filteredTransactions.ifPresent(cashewTransactions -> {
            importedTransactions.removeIf(element -> !cashewTransactions.contains(element));
            importTransactions();
        });
    }

    public void refreshUI() {
        sourceAccountPicker.setDisable(!cashew.isReady());
        targetAccountPicker.setDisable(!cashew.isReady());
        startPicker.setDisable(!cashew.isReady());
        endPicker.setDisable(!cashew.isReady());
        importButton.setDisable(!cashew.isReady());
        reviewButton.setDisable(importedTransactions.isEmpty());
        filterTypeSelector.setDisable(!cashew.isReady());
    }

    public void pickSourceDatabaseFile() {
        File f = filePicker.showOpenDialog(this.getOwner());
        if (f == null) return;

        LoadingDialog dlg = new LoadingDialog(LoadingDialog.LoadingOperationType.INDETERMINATE_SPINNER);
        dlg.setPrimaryLabel("Preparing for import");
        dlg.setSecondaryLabel("Reading database file...");
        dlg.show("Just a second", new Runnable() {
            @Override
            public void run() {
                try {
                    cashew.initialize(f.toURI());
                } catch (FileNotFoundException e) {
                    Platform.runLater(() -> {
                        PopupManager.showPopup("No file selected", "Please select a file to import transactions.", Alert.AlertType.WARNING);
                    });
                }
            }
        });
        filePickerButton.setText(f.getName());
        refreshUI();

        sourceAccountPicker.getItems().setAll(cashew.getWallets().stream().map(CashewAccount::displayName).toList());
        targetAccountPicker.getItems().setAll(DataManager.getInstance().getAccounts().stream().map(Account::getName).toList());
    }
}
