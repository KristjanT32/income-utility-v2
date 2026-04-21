package com.krisapps.incomeutility_v2.subutilities.breakdown;

import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.data.CategoryExpenseSummary;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.CurrencyConfig;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.CategoryExpenseTotalCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.ui.listview.cell.CategoryExpenseTotalCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class BreakdownController extends SubUtilityController {

    private SubUtility utility;

    @FXML
    private Label totalSpendingLabel;

    @FXML
    private Label mostSpentOnLabel;

    @FXML
    private ListView<CategoryExpenseSummary> categoryBreakdownListView;

    @FXML
    private ListView<Transaction> transactionList;

    @FXML
    private Button backButton;

    @FXML
    private Button resetPeriod;

    @FXML
    private Button prevMonthButton;

    @FXML
    private Button nextMonthButton;

    @FXML
    private DatePicker periodStartPicker;

    @FXML
    private DatePicker periodEndPicker;

    @FXML
    private ComboBox<Account> accountPicker;

    private static final FiscalService fiscal = FiscalService.getInstance();
    private Account selectedAccount;

    @Override
    public void onStartup(SubUtility utility) {
        this.utility = utility;
    }

    @Override
    public void onShutdown() {

    }

    @FXML
    public void initialize() {
        initUI();
    }

    private void autopickPeriod() {
        LocalDate now = LocalDate.now();

        if (now.getDayOfMonth() != 1) {
            periodStartPicker.setValue(now.withDayOfMonth(1));
        } else {
            periodStartPicker.setValue(now);
        }
        periodEndPicker.setValue(now.withDayOfMonth(now.getMonth().length(now.isLeapYear())));
    }

    public void initUI() {
        autopickPeriod();

        backButton.setOnAction(_ -> utility.stop());
        periodStartPicker.setConverter(Formats.DATE_FORMAT);
        periodEndPicker.setConverter(Formats.DATE_FORMAT);
        accountPicker.setCellFactory(new AccountComboboxCellFactory());
        accountPicker.setButtonCell(new AccountComboboxButtonCell());
        periodStartPicker.valueProperty().addListener((obs, old, val) -> {
            calculate();
        });
        periodEndPicker.valueProperty().addListener((obs, old, val) -> {
            calculate();
        });

        accountPicker.valueProperty().addListener(((_, _, newValue) -> {
            selectedAccount = newValue;
            calculate();
        }));

        resetPeriod.setOnAction(ev -> {
            autopickPeriod();
        });

        prevMonthButton.setOnAction(ev -> {
            periodStartPicker.setValue(periodStartPicker.getValue().minusMonths(1));

            LocalDate newEnd = periodEndPicker.getValue().minusMonths(1);
            periodEndPicker.setValue(newEnd.withDayOfMonth(newEnd.getMonth().length(newEnd.isLeapYear())));
        });

        nextMonthButton.setOnAction(ev -> {
            periodStartPicker.setValue(periodStartPicker.getValue().plusMonths(1).withDayOfMonth(1));

            LocalDate newEnd = periodEndPicker.getValue().plusMonths(1);
            periodEndPicker.setValue(newEnd.withDayOfMonth(newEnd.getMonth().length(newEnd.isLeapYear())));
        });

        HashSet<Account> accounts = fiscal.getAccounts();
        DataManager.getInstance().getLastActiveAccount().ifPresentOrElse(account -> {
            accountPicker.setValue(fiscal.getAccountById(account).get());
        }, () -> {
            if (!accounts.isEmpty()) {
                accountPicker.setValue(accounts.stream().findFirst().get());
            } else {
                PopupManager.showPopup("No accounts found!", "No accounts have been created yet, so no functionality will be available.", Alert.AlertType.WARNING);
            }
        });

        categoryBreakdownListView.setCellFactory(new CategoryExpenseTotalCellFactory(selectedAccount));
        transactionList.setCellFactory(new TransactionCellFactory(selectedAccount, (_) -> calculate(), true));

        accountPicker.setItems(FXCollections.observableList(accounts.stream().toList()));
        accountPicker.setConverter(new StringConverter<>() {
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

        calculate();
    }

    private void calculate() {
        if (selectedAccount == null) return;
        List<Transaction> transactions = fiscal.getTransactionsBetween(selectedAccount, periodStartPicker.getValue(), periodEndPicker.getValue()).stream().filter(t -> t.getAmount() < 0).toList();
        Map<String, List<Transaction>> categoriesToTransactions = transactions.stream().collect(Collectors.groupingBy(t -> t.getCategory() == TransactionCategory.CUSTOM ? t.getCustomCategory() : DataManager.Formatting.capitalize(t.getCategory().name())));

        totalSpendingLabel.setText(DataManager.Formatting.formatMoney(transactions.stream().mapToDouble(Transaction::getAbsoluteAmount).sum(), selectedAccount.getCurrencyConfig()));
        categoriesToTransactions.entrySet().stream().max(Comparator.comparingDouble(entry -> entry.getValue().stream().mapToDouble(Transaction::getAbsoluteAmount).sum())).ifPresentOrElse(max -> {
            mostSpentOnLabel.setText("%s (%s)".formatted(max.getKey(), DataManager.Formatting.formatMoney(max.getValue().stream().mapToDouble(Transaction::getAbsoluteAmount).sum(), selectedAccount.getCurrencyConfig())));
        }, () -> {
            mostSpentOnLabel.setText("N/A");
        });

        List<CategoryExpenseSummary> items = new ArrayList<>(categoriesToTransactions.entrySet().stream().map(entry -> new CategoryExpenseSummary(entry.getKey(), entry.getValue())).toList());
        items.sort(Comparator.comparingDouble(CategoryExpenseSummary::totalExpenses).reversed());
        categoryBreakdownListView.setItems(FXCollections.observableList(items));
        transactionList.setItems(FXCollections.observableList(transactions.stream().sorted(Comparator.comparing(Transaction::getAmount)).toList()));

    }
}
