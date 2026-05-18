package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.dialogs.AddMultipleTransactionsDialog;
import com.krisapps.incomeutility_v2.dialogs.AddSingleTransactionDialog;
import com.krisapps.incomeutility_v2.dialogs.ImportFromCashewDialog;
import com.krisapps.incomeutility_v2.exceptions.TransactionNotPermittedException;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import com.krisapps.incomeutility_v2.util.services.TransactionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Pair;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;

/* Controller class for the Money In Money Out utility */
public class MoneyFlowUtilityController extends SubUtilityController {

    private static final DataManager data = DataManager.getInstance();
    private static final TransactionService transactor = TransactionService.getInstance();
    private static final FiscalService fiscal = FiscalService.getInstance();

    //<editor-fold desc="UI">
    @FXML
    private Label startingBalanceLabel;

    @FXML
    private Label inflowLabel;

    @FXML
    private Label outflowLabel;

    @FXML
    private Label changeLabel;

    @FXML
    private Label currentBalanceLabel;

    @FXML
    private Label transactionsLabel;

    @FXML
    private ComboBox<Account> accountSelector;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ListView<Transaction> transactionList;

    @FXML
    private MenuButton addTransactionButton;

    @FXML
    private Button backButton;

    @FXML
    private HBox commandPrompt;

    @FXML
    private TextField commandPromptField;

    @FXML
    private Label commandPromptLabel;
    //</editor-fold>

    private Account selectedAccount;
    private LocalDate selectedDate;

    private SubUtility utility;

    @FXML
    public void initialize() {
        data.initialize();
        initUI();
    }

    public void onStartup(SubUtility utility) {
        this.utility = utility;
    }

    public void onShutdown() {
        data.updateLastOpenAccount(selectedAccount);
        data.applyConfigurationData();
    }

    @Override
    public void onPromptCommand(String command, String[] args) {
        switch (command) {
            case "prevday" -> previousDay();
            case "nextday" -> nextDay();
            case "today" -> resetToToday();
            case "list" -> {
                if (args.length < 1) {
                    PopupManager.showPopup("Invalid syntax", "Missing argument: transactions/accounts for utility command date", Alert.AlertType.WARNING);
                    return;
                }

                if (args[0].equals("transactions")) {
                    PopupManager.showListDialog("Transactions", "Transactions", new ArrayList<>(data.getAllTransactions().values()), new TransactionCellFactory(selectedAccount, (transaction) -> {}));
                }
            }
            case "date" -> {
                if (args.length < 1) {
                    String date = PopupManager.showInputDialog("Invalid syntax", "Missing argument: 'date (dd/MM/yyyy)' for utility command date", "Date: ", "");

                    if (!date.isEmpty()) {
                        datePicker.setValue(LocalDate.parse(date, Formats.DATE_FORMATTER));
                    }
                    return;
                }
                datePicker.setValue(LocalDate.parse(args[0], Formats.DATE_FORMATTER));
            }
            case "first-transaction" -> {
                if (fiscal.getTransactions(selectedAccount).isEmpty()) {
                    PopupManager.showPopup("No transactions found", "No transactions exist, so no date could be found with any transactions to be shown.", Alert.AlertType.ERROR);
                } else {
                    fiscal.getTransactions(selectedAccount).stream().min(Comparator.comparing(Transaction::getTimestamp)).ifPresentOrElse((t) -> {
                        datePicker.setValue(t.getTimestamp().toLocalDate());
                    }, () -> {
                        PopupManager.showPopup("No transactions found", "No transactions exist, so no date could be found with any transactions to be shown.", Alert.AlertType.ERROR);
                    });
                }
            }

            case "last-transaction" -> {
                if (fiscal.getTransactions(selectedAccount).isEmpty()) {
                    PopupManager.showPopup("No transactions found", "No transactions exist, so no date could be found with any transactions to be shown.", Alert.AlertType.ERROR);
                } else {
                    fiscal.getTransactions(selectedAccount).stream().max(Comparator.comparing(Transaction::getTimestamp)).ifPresentOrElse((t) -> {
                        datePicker.setValue(t.getTimestamp().toLocalDate());
                    }, () -> {
                        PopupManager.showPopup("No transactions found", "No transactions exist, so no date could be found with any transactions to be shown.", Alert.AlertType.ERROR);
                    });
                }
            }
            case "find" -> {
                // TODO: Implement
            }
            case "migration" -> data.migrateJSONDataToSQL();
            case "refresh" -> refreshUI();
            case "exit" -> utility.stop();
            default -> PopupManager.showPopup("Unknown utility command", "'" + command + "' is not a valid utility command.", Alert.AlertType.ERROR);
        }
    }

    public Account getSelectedAccount() {
        return this.selectedAccount;
    }

    public LocalDate getSelectedDate() {
        return this.selectedDate;
    }

    public void initUI() {
        HashSet<Account> accounts = fiscal.getAccounts();
        accountSelector.setConverter(new StringConverter<>() {
            @Override
            public String toString(Account account) {
                if (account == null) {
                    return "Select account";
                }
                return account.getName();
            }

            @Override
            public Account fromString(String s) {
                return null;
            }
        });

        datePicker.setConverter(Formats.DATE_FORMAT);

        accountSelector.setCellFactory(new AccountComboboxCellFactory());
        accountSelector.setButtonCell(new AccountComboboxButtonCell());
        accountSelector.valueProperty().addListener(((_, _, newValue) -> {
            selectedAccount = newValue;
            transactionList.setCellFactory(new TransactionCellFactory(selectedAccount, (item) -> {
                refreshUI();
                refreshAccountSelector();
                refreshTransactionList();
            }));
            refreshTransactionList();
            refreshUI();
        }));
        accountSelector.setItems(FXCollections.observableList(accounts.stream().toList()));

        datePicker.valueProperty().addListener((obs, _, newVal) -> {
            if (newVal == null) {
                selectedDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
            } else {
                selectedDate = newVal;
            }
            refreshTransactionList();
            refreshUI();
        });
        datePicker.setValue(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));

        data.getLastActiveAccount().ifPresentOrElse(account -> {
            if (data.getAccount(account).isPresent()) {
                accountSelector.setValue(data.getAccount(account).get());
            } else {
                accountSelector.setValue(data.getAccounts().stream().findFirst().orElse(null));
            }
        }, () -> {
            accountSelector.setValue(data.getAccounts().stream().findFirst().orElse(null));
        });

        backButton.setOnAction((ev) -> {
            this.utility.stop();
        });

        refreshUI();
    }

    public void refreshAccountSelector() {
        HashSet<Account> accounts = data.getAccounts();
        accountSelector.setItems(FXCollections.observableList(accounts.stream().toList()));
        accountSelector.getSelectionModel().select(selectedAccount);
    }

    public void refreshTransactionList() {
        ObservableList<Transaction> items = transactionList.getItems();
        if (selectedAccount == null) {
            items.clear();
        } else {
            items.setAll(data.getTransactions(selectedAccount).stream().filter(transaction -> transaction.getTimestamp().toLocalDate().isEqual(selectedDate)).toList());
            items.sort(Comparator.comparing(Transaction::getTimestamp).reversed());
        }
        transactionList.setItems(items);

        Label l = new Label("There are no transactions for the selected period.");
        l.getStyleClass().add("medium-label");
        transactionList.setPlaceholder(l);
    }

    public void refreshUI() {
        accountSelector.requestLayout();

        if (selectedAccount != null && selectedDate != null) {
            startingBalanceLabel.setText(DataManager.Formatting.formatMoney(
                    fiscal.getStartingBalance(selectedAccount, selectedDate),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
            inflowLabel.setText(DataManager.Formatting.formatMoney(
                    fiscal.getInflow(selectedAccount, selectedDate),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
            outflowLabel.setText(DataManager.Formatting.formatMoney(
                    fiscal.getOutflow(selectedAccount, selectedDate),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
            currentBalanceLabel.setText(DataManager.Formatting.formatMoney(
                    fiscal.getCurrentBalance(selectedAccount),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));

            double change = fiscal.getChange(selectedAccount, selectedDate);
            changeLabel.getStyleClass().removeAll("green-positive", "red-negative");
            changeLabel.getStyleClass().add((change > 0 ? "green-positive" : change < 0 ? "red-negative" : ""));
            changeLabel.setText(DataManager.Formatting.formatMoney(
                    change,
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
        } else {
            startingBalanceLabel.setText("N/A");
            inflowLabel.setText("N/A");
            outflowLabel.setText("N/A");
            changeLabel.setText("N/A");
        }

        addTransactionButton.setDisable(selectedAccount == null);
    }

    @FXML
    public void nextDay() {
        datePicker.setValue(datePicker.getValue().plusDays(1));
    }

    @FXML
    public void previousDay() {
        datePicker.setValue(datePicker.getValue().minusDays(1));
    }

    @FXML
    public void resetToToday() {
        datePicker.setValue(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));
    }


    @FXML
    public void promptAddSingleTransaction() {
        AddSingleTransactionDialog dlg = new AddSingleTransactionDialog(selectedAccount);
        Optional<Transaction> t = dlg.showAndWait();
        t.ifPresent((transaction -> {
            try {
                transactor.perform(transaction);
                refreshAccountSelector();
                refreshTransactionList();
                refreshUI();
            } catch (TransactionNotPermittedException exception) {
                PopupManager.showPopup(exception.getType().getDisplayName() + " not permitted", exception.getReason().getDescription(), Alert.AlertType.ERROR);
            }
        }));
    }

    @FXML
    public void promptAddMultipleTransactions() {
        AddMultipleTransactionsDialog dlg = new AddMultipleTransactionsDialog(selectedAccount);
        Optional<ArrayList<Transaction>> t = dlg.showAndWait();
        t.ifPresent((transaction -> {
            try {
                transactor.pushTransactionsTo(selectedAccount, transaction);
                refreshAccountSelector();
                refreshTransactionList();
                refreshUI();
            } catch (TransactionNotPermittedException exception) {
                PopupManager.showPopup(exception.getType().getDisplayName() + " not permitted", exception.getReason().getDescription(), Alert.AlertType.ERROR);
            }
        }));
    }

    @FXML
    public void promptImportTransactions() {
        ImportFromCashewDialog dlg = new ImportFromCashewDialog(selectedAccount);
        Optional<Pair<Account, ArrayList<CashewTransaction>>> response = dlg.showAndWait();

        response.ifPresent(importedTransactions -> {
            int imported = transactor.pushTransactionsTo(importedTransactions.getKey(), importedTransactions.getValue());

            refreshTransactionList();
            refreshUI();
            refreshAccountSelector();
            PopupManager.showPopup("Import completed!", "Successfully imported %s new transactions of %s selected from Cashew.".formatted(imported, importedTransactions.getValue().size()), Alert.AlertType.INFORMATION);
        });
    }
}
