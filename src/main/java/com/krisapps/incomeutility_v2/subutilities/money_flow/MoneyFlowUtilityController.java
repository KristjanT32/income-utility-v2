package com.krisapps.incomeutility_v2.subutilities.money_flow;

import com.krisapps.incomeutility_v2.dialogs.AddSingleTransactionDialog;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.TransactionService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
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
    private ComboBox<Account> accountSelector;

    @FXML
    private DatePicker datePicker;

    @FXML
    private ListView<Transaction> transactionList;
    //</editor-fold>


    private Account selectedAccount;


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

        accountSelector.setCellFactory(new AccountComboboxCellFactory());
        accountSelector.setButtonCell(new AccountComboboxButtonCell());
        accountSelector.valueProperty().addListener(((_, _, newValue) -> {
            selectedAccount = newValue;
            refreshTransactionList();
        }));
        accountSelector.setItems(FXCollections.observableList(accounts.stream().toList()));

        datePicker.setValue(LocalDate.ofInstant(Instant.now(), ZoneId.systemDefault()));
        transactionList.setCellFactory(new TransactionCellFactory());

        data.getLastActiveAccount().ifPresentOrElse(account -> {
            accountSelector.setValue(data.getAccount(account).get());
        }, () -> {
            accountSelector.setValue(data.getAccounts().stream().findFirst().orElse(null));
        });
    }

    public void refreshTransactionList() {
        ObservableList<Transaction> items = transactionList.getItems();
        items.setAll(data.getTransactions(selectedAccount));
        transactionList.setItems(items);
    }

    public void refreshUI() {
        accountSelector.requestLayout();
    }

    public void promptAddSingleTransaction() {
        AddSingleTransactionDialog dlg = new AddSingleTransactionDialog(selectedAccount);
        Optional<Transaction> t = dlg.showAndWait();
        t.ifPresent((transaction -> {
            transactor.perform(transaction);
            refreshTransactionList();
            refreshUI();
        }));
    }
}
