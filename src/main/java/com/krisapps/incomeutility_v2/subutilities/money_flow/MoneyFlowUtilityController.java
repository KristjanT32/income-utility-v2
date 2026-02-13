package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.dialogs.AddSingleTransactionDialog;
import com.krisapps.incomeutility_v2.exceptions.TransactionNotPermittedException;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.TransactionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;

/* Controller class for the Money In Money Out utility */
public class MoneyFlowUtilityController {

    private static final DataManager data = DataManager.getInstance();
    private static final TransactionService transactor = TransactionService.getInstance();

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
        log("Initializing fiscal data...");
        initUI();
    }

    public void stop() {
        data.updateLastOpenAccount(selectedAccount);
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
        }));
        accountSelector.setItems(FXCollections.observableList(accounts.stream().toList()));

        datePicker.valueProperty().addListener((obs, _, newVal) -> {
            selectedDate = newVal;
            if (selectedDate != null) {
                refreshTransactionList();
            } else {
                selectedDate = LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault());
            }
        });
        datePicker.setValue(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));

        data.getLastActiveAccount().ifPresentOrElse(account -> {
            accountSelector.setValue(data.getAccount(account).get());
        }, () -> {
            accountSelector.setValue(data.getAccounts().stream().findFirst().orElse(null));
        });
    }

    public void refreshTransactionList() {
        if (selectedAccount == null) {
            ObservableList<Transaction> items = transactionList.getItems();
            items.clear();
            transactionList.setItems(items);
        } else {
            ObservableList<Transaction> items = transactionList.getItems();
            items.setAll(data.getTransactions(selectedAccount));
            items.removeIf(transaction -> !transaction.getTimestamp().toLocalDate().isEqual(selectedDate));
            items.sort(Comparator.comparing(Transaction::getTimestamp).reversed());
            transactionList.setItems(items);
        }

        Label l = new Label("There are no transactions for the selected period.");
        l.getStyleClass().add("medium-label");
        transactionList.setPlaceholder(l);
    }

    public void refreshUI() {
        accountSelector.requestLayout();
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
                refreshTransactionList();
                refreshUI();
            } catch (TransactionNotPermittedException exception) {
                PopupManager.showPopup(exception.getType().getDisplayName() + " not permitted", exception.getReason().getDescription(), Alert.AlertType.ERROR);
            }
        }));
    }
}
