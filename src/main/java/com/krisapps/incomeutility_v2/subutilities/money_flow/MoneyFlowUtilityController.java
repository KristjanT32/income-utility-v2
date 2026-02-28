package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.dialogs.AddSingleTransactionDialog;
import com.krisapps.incomeutility_v2.dialogs.ImportFromCashewDialog;
import com.krisapps.incomeutility_v2.exceptions.TransactionNotPermittedException;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.fiscal.cashew.CashewTransaction;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.services.TransactionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Pair;
import javafx.util.StringConverter;

import javax.swing.*;
import javax.swing.text.html.Option;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;

/* Controller class for the Money In Money Out utility */
public class MoneyFlowUtilityController {

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
    //</editor-fold>


    private Account selectedAccount;
    private LocalDate selectedDate;


    private static void log(String message) {
        DataManager.log("[Money In Money Out] " + message);
    }


    @FXML
    public void initialize() {
        data.initialize();
        log("Initializing fiscal data...");
        initUI();
    }

    public void stop() {
        data.updateLastOpenAccount(selectedAccount);
        data.saveCurrentData();
    }

    public Account getSelectedAccount() {
        return this.selectedAccount;
    }

    public LocalDate getSelectedDate() {
        return this.selectedDate;
    }

    public void initUI() {
        HashSet<Account> accounts = data.getAccounts();
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

        datePicker.setConverter(new StringConverter<LocalDate>() {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            @Override
            public String toString(LocalDate localDate) {
                if (localDate != null) {
                    return format.format(localDate);
                } else {
                    return "";
                }
            }

            @Override
            public LocalDate fromString(String s) {
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
                try {
                    return LocalDate.ofInstant(format.parse(s).toInstant(), ZoneId.systemDefault());
                } catch (ParseException e) {
                    return null;
                }
            }
        });

        accountSelector.setCellFactory(new AccountComboboxCellFactory());
        accountSelector.setButtonCell(new AccountComboboxButtonCell());
        accountSelector.valueProperty().addListener(((_, _, newValue) -> {
            selectedAccount = newValue;
            transactionList.setCellFactory(new TransactionCellFactory(selectedAccount));
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
            accountSelector.setValue(data.getAccount(account).get());
        }, () -> {
            accountSelector.setValue(data.getAccounts().stream().findFirst().orElse(null));
        });
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
            items.setAll(data.getTransactions(selectedAccount));
            items.removeIf(transaction -> !transaction.getTimestamp().toLocalDate().isEqual(selectedDate));
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
    public void promptImportTransactions() {
        ImportFromCashewDialog dlg = new ImportFromCashewDialog(selectedAccount);
        Optional<Pair<Account, ArrayList<CashewTransaction>>> response = dlg.showAndWait();

        response.ifPresent(importedTransactions -> {
            transactor.pushTransactionsTo(importedTransactions.getKey(), importedTransactions.getValue());
            refreshTransactionList();
            refreshUI();
            refreshAccountSelector();
        });
    }
}
