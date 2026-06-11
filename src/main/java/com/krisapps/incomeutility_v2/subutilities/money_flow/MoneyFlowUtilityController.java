package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.dialogs.AddMultipleTransactionsDialog;
import com.krisapps.incomeutility_v2.dialogs.AddSingleTransactionDialog;
import com.krisapps.incomeutility_v2.dialogs.ImportFromCashewDialog;
import com.krisapps.incomeutility_v2.dialogs.generic.InputDialog;
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
import com.krisapps.incomeutility_v2.util.Formatting;
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
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.*;

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
    private Button nextDayButton;

    @FXML
    private Button prevDayButton;

    @FXML
    private Button resetDateButton;

    @FXML
    private HBox commandPrompt;

    @FXML
    private TextField commandPromptField;

    @FXML
    private Label commandPromptLabel;

    @FXML
    private Label dateTitle;
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
        data.saveCurrentConfigurationData();
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
                    InputDialog dialog = new InputDialog("Missing argument");
                    dialog.setPrimaryLabel("Invalid syntax");
                    dialog.setDescription("You are missing an argument for the 'date' command. Please supply it using the text field below.");
                    dialog.setPrompt("dd/MM/yyyy");
                    Optional<String> date = dialog.showAndWait();
                    try {
                        date.ifPresent(s -> datePicker.setValue(LocalDate.parse(s, Formats.DATE_FORMATTER)));
                    } catch (DateTimeParseException e) {
                        PopupManager.showPopup("Invalid date format", "The supplied date is not valid.\nPlease specify the date as dd/MM/yyyy.", Alert.AlertType.ERROR);
                    }
                    return;
                }
                datePicker.setValue(LocalDate.parse(args[0], Formats.DATE_FORMATTER));
            }
            case "first-transaction" -> {
                selectDateOfFirstTransaction();
            }

            case "last-transaction" -> {
                selectDateOfLastTransaction();
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

    private void selectDateOfFirstTransaction() {
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

    private void selectDateOfLastTransaction() {
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

        Optional<Account> lastActive = data.tryPickActiveAccount();
        if (lastActive.isPresent()) {
            accountSelector.setValue(lastActive.get());
        } else {
            PopupManager.showPopup("No accounts found!", "No accounts have been created yet, so no functionality will be available.", Alert.AlertType.WARNING);
        }

        backButton.setOnAction((ev) -> {
            this.utility.stop();
        });

        prevDayButton.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.isShiftDown()) {
                selectDateOfFirstTransaction();
            } else {
                previousDay();
            }
        });

        nextDayButton.setOnMouseClicked(mouseEvent -> {
            nextDay();
        });

        resetDateButton.setOnMouseClicked(mouseEvent -> {
            if (mouseEvent.isShiftDown()) {
                selectDateOfLastTransaction();
            } else {
                resetToToday();
            }
        });

        Tooltip.install(resetDateButton, new Tooltip(
                "Resets the date to today.\n\nNote: Shift+Click to reset to the date of the last transaction."
        ));

        Tooltip.install(prevDayButton, new Tooltip(
                "Selects the previous day.\n\nNote: Shift+Click to select the day of the first transaction."
        ));

        Tooltip.install(nextDayButton, new Tooltip(
                "Selects the next day."
        ));

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
            startingBalanceLabel.setText(Formatting.formatMoney(
                    fiscal.getStartingBalance(selectedAccount, selectedDate),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
            inflowLabel.setText(Formatting.formatMoney(
                    fiscal.getInflow(selectedAccount, selectedDate),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
            outflowLabel.setText(Formatting.formatMoney(
                    fiscal.getOutflow(selectedAccount, selectedDate),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));
            currentBalanceLabel.setText(Formatting.formatMoney(
                    fiscal.getCurrentBalance(selectedAccount),
                    selectedAccount.getCurrencyConfig().getCurrencySymbol(),
                    selectedAccount.getCurrencyConfig().isCurrencySymbolPrefix()
            ));

            dateTitle.setText("%s, %s %s %s".formatted(
                    selectedDate.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    selectedDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()),
                    selectedDate.getDayOfMonth() + Formatting.getNumberSuffix(selectedDate.getDayOfMonth()),
                    Objects.equals(selectedDate, LocalDate.now()) ? "(Today)" : Objects.equals(selectedDate, LocalDate.now().minusDays(1)) ? "(Yesterday)" : ""
            ));

            double change = fiscal.getChange(selectedAccount, selectedDate);
            changeLabel.getStyleClass().removeAll("green-positive", "red-negative");
            changeLabel.getStyleClass().add((change > 0 ? "green-positive" : change < 0 ? "red-negative" : ""));
            changeLabel.setText(Formatting.formatMoney(
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

    public void nextDay() {
        datePicker.setValue(datePicker.getValue().plusDays(1));
    }

    public void previousDay() {
        datePicker.setValue(datePicker.getValue().minusDays(1));
    }

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
