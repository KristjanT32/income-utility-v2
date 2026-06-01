package com.krisapps.incomeutility_v2.subutilities.breakdown;

import com.krisapps.incomeutility_v2.dialogs.generic.ListDialog;
import com.krisapps.incomeutility_v2.subutilities.SubUtility;
import com.krisapps.incomeutility_v2.subutilities.SubUtilityController;
import com.krisapps.incomeutility_v2.types.data.CategoryExpenseSummary;
import com.krisapps.incomeutility_v2.types.fiscal.Account;
import com.krisapps.incomeutility_v2.types.fiscal.Transaction;
import com.krisapps.incomeutility_v2.types.transaction.TransactionCategory;
import com.krisapps.incomeutility_v2.types.transaction.TransactionType;
import com.krisapps.incomeutility_v2.ui.listview.AccountComboboxCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.CategoryExpenseTotalCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.TransactionCellFactory;
import com.krisapps.incomeutility_v2.ui.listview.cell.AccountComboboxButtonCell;
import com.krisapps.incomeutility_v2.util.DataManager;
import com.krisapps.incomeutility_v2.util.PopupManager;
import com.krisapps.incomeutility_v2.util.misc.Formats;
import com.krisapps.incomeutility_v2.util.services.FiscalService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class BreakdownController extends SubUtilityController {

    private SubUtility utility;

    @FXML
    private Label totalSpendingLabel;

    @FXML
    private Label mostSpentOnLabel;

    @FXML
    private Label transactionCountLabel;

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

    @FXML
    private ComboBox<SortingOrder> sortSelector;

    @FXML
    private ComboBox<TypeFilter> transactionTypeSelector;

    @FXML
    private PieChart breakdownPieChart;

    @FXML
    private BarChart<String, Double> categoriesBarChart;

    @FXML
    private LineChart<String, Double> spendingLineChart;

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

    private static final FiscalService fiscal = FiscalService.getInstance();
    private Account selectedAccount;
    private TypeFilter currentTypeFilter = TypeFilter.EXPENSES;

    private List<Transaction> cachedTransactions = new ArrayList<>();
    private boolean shouldRecache = true;

    private void pickPreviousMonth() {
        periodStartPicker.setValue(periodStartPicker.getValue().minusMonths(1));

        LocalDate newEnd = periodEndPicker.getValue().minusMonths(1);
        periodEndPicker.setValue(newEnd.withDayOfMonth(newEnd.getMonth().length(newEnd.isLeapYear())));
    }

    private void pickNextMonth() {
        periodStartPicker.setValue(periodStartPicker.getValue().plusMonths(1).withDayOfMonth(1));

        LocalDate newEnd = periodEndPicker.getValue().plusMonths(1);
        periodEndPicker.setValue(newEnd.withDayOfMonth(newEnd.getMonth().length(newEnd.isLeapYear())));
    }

    private enum SortingOrder {
        CHRONOLOGICAL_ASC("Chronologically, oldest first"),
        CHRONOLOGICAL_DESC("Chronologically, most recent first"),
        TRANSACTION_AMOUNT_ASC("By amount, ascending"),
        TRANSACTION_AMOUNT_DESC("By amount, descending")
        ;

        String displayName;

        SortingOrder(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private enum TypeFilter {
        EXPENSES("All expenses"),
        INCOME("All income"),
        TRANSFERS("Only transfers"),
        EXPENSES_NO_TRANSFERS("Expenses, excluding transfers"),
        INCOME_NO_TRANSFERS("Income, excluding transfers");

        String displayName;

        TypeFilter(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    @Override
    public void onStartup(SubUtility utility) {
        this.utility = utility;
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onPromptCommand(String command, String[] args) {
        switch (command) {
            case "show" -> {
                if (args.length < 2) {

                    List<String> aliasesForNow = List.of("now", "today", "current", "nw", "td", "cr");
                    if (args.length == 1) {
                        if (aliasesForNow.contains(args[0])) {
                            autopickPeriod();
                        } else {
                            PopupManager.showPopup("Invalid syntax", "Syntax for 'show' is as follows:\nshow <period-start dd/MM/yyyy> <period-end dd/MM/yyyy>", Alert.AlertType.ERROR);
                        }
                    } else {
                        PopupManager.showPopup("Invalid syntax", "Syntax for 'show' is as follows:\nshow <period-start dd/MM/yyyy> <period-end dd/MM/yyyy>", Alert.AlertType.ERROR);
                    }
                } else {
                    try {
                        LocalDate from = LocalDate.parse(args[0], DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        LocalDate to = LocalDate.parse(args[0], DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        periodStartPicker.setValue(from);
                        periodEndPicker.setValue(to);
                    } catch (DateTimeParseException e) {
                        PopupManager.showPopup("Invalid date format!", "Please use dd/MM/yyyy when specifying dates!", Alert.AlertType.ERROR);
                    }
                }
            }
            case "prev" -> pickPreviousMonth();
            case "next" -> pickNextMonth();
            case "refresh" -> refreshUI();
            case "exit" -> utility.stop();
        }
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
        sortSelector.setConverter(new StringConverter<SortingOrder>() {
            @Override
            public String toString(SortingOrder object) {
                return object.getDisplayName();
            }

            @Override
            public SortingOrder fromString(String string) {
                return null;
            }
        });
        sortSelector.setItems(FXCollections.observableList(Arrays.stream(SortingOrder.values()).toList()));
        sortSelector.setValue(SortingOrder.TRANSACTION_AMOUNT_DESC);
        sortSelector.valueProperty().addListener((obs, old, val) -> {
            refreshUI();
        });

        periodStartPicker.valueProperty().addListener((obs, old, val) -> {
            shouldRecache = true;
            refreshUI();
        });
        periodEndPicker.valueProperty().addListener((obs, old, val) -> {
            shouldRecache = true;
            refreshUI();
        });

        accountPicker.valueProperty().addListener(((_, _, newValue) -> {
            shouldRecache = true;
            selectedAccount = newValue;
            refreshUI();
        }));

        resetPeriod.setOnAction(ev -> {
            autopickPeriod();
        });

        prevMonthButton.setOnAction(_ -> pickPreviousMonth());
        nextMonthButton.setOnAction(_ -> pickNextMonth());

        HashSet<Account> accounts = fiscal.getAccounts();
        Optional<Account> lastActive = DataManager.getInstance().tryPickActiveAccount();
        if (lastActive.isPresent()) {
            accountPicker.setValue(lastActive.get());
        } else {
            PopupManager.showPopup("No accounts found!", "No accounts have been created yet, so no functionality will be available.", Alert.AlertType.WARNING);
        }

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

        breakdownPieChart.setLabelsVisible(true);
        breakdownPieChart.dataProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            double total = breakdownPieChart.getData().stream().mapToDouble(PieChart.Data::getPieValue).sum();

            val.forEach(slice -> {

                Tooltip tooltip = new Tooltip("%s (%s%%)".formatted(DataManager.Formatting.formatMoney(slice.getPieValue(), selectedAccount.getCurrencyConfig()), DataManager.Formatting.decimalFormatter.format((slice.getPieValue() / total) * 100)));
                Tooltip.install(slice.getNode(), tooltip);
                slice.getNode().setOnMouseClicked(_ -> {
                    ListDialog<Transaction> listDialog = new ListDialog<>("Transactions for category '" + slice.getName() + "'");
                    listDialog.setLabel("Transactions marked");
                    listDialog.setSubLabel("%s, %s".formatted(slice.getName(), DataManager.Formatting.formatMoney(slice.getPieValue(), selectedAccount.getCurrencyConfig())));
                    listDialog.setListViewCellFactory(new TransactionCellFactory(selectedAccount, _ -> {}, true));
                    listDialog.setItems(transactionList.getItems().stream().filter(t -> t.getCategory().name().equalsIgnoreCase(slice.getName()) || t.getCustomCategory().equalsIgnoreCase(slice.getName())).toList());
                    listDialog.show();
                });
                slice.getNode().setOnMouseEntered(_ -> {
                    slice.getNode().setStyle("-fx-border-width: 2px; -fx-border-color: black");
                });

                slice.getNode().setOnMouseExited(_ -> {
                    slice.getNode().setStyle("");
                });
            });
        });

        spendingLineChart.dataProperty().addListener((obs, old, val) -> {
            if (val == null) return;

            val.forEach(series -> {
                series.getData().forEach(dataPoint -> {
                    Tooltip tooltip = new Tooltip(DataManager.Formatting.formatMoney(dataPoint.getYValue(), selectedAccount.getCurrencyConfig()));
                    Tooltip.install(dataPoint.getNode(), tooltip);

                    dataPoint.getNode().setCursor(Cursor.HAND);
                    dataPoint.getNode().setOnMouseClicked(_ -> {
                        ListDialog<Transaction> listDialog = new ListDialog<>("Transactions on " + dataPoint.getXValue());
                        listDialog.setLabel("Transactions on " + dataPoint.getXValue());
                        listDialog.setSubLabel("Total: " + DataManager.Formatting.formatMoney(dataPoint.getYValue(), selectedAccount.getCurrencyConfig()));
                        listDialog.setListViewCellFactory(new TransactionCellFactory(selectedAccount, _ -> {}, true));
                        listDialog.setItems(transactionList.getItems().stream().filter(t -> t.getTimestamp().toLocalDate().equals(LocalDate.parse(dataPoint.getXValue(), DateTimeFormatter.ofPattern("dd/MM/yyyy")))).toList());
                        listDialog.show();
                    });
                });
            });
        });

        categoriesBarChart.dataProperty().addListener((obs, old, val) -> {
            if (val == null) return;

            val.forEach(series -> {
                series.getData().forEach(dataPoint -> {
                    dataPoint.getNode().setCursor(Cursor.HAND);

                    Tooltip tooltip = new Tooltip(DataManager.Formatting.formatMoney(dataPoint.getYValue(), selectedAccount.getCurrencyConfig()));
                    Tooltip.install(dataPoint.getNode(), tooltip);

                    dataPoint.getNode().setOnMouseClicked(_ -> {
                        ListDialog<Transaction> listDialog = new ListDialog<>("Transactions for category '" + dataPoint.getXValue() + "'");
                        listDialog.setLabel("Transactions marked");
                        listDialog.setSubLabel("%s, %s".formatted(dataPoint.getXValue(), DataManager.Formatting.formatMoney(dataPoint.getYValue(), selectedAccount.getCurrencyConfig())));
                        listDialog.setListViewCellFactory(new TransactionCellFactory(selectedAccount, _ -> {}, true));
                        listDialog.setItems(transactionList.getItems().stream().filter(t -> t.getCategory().name().equalsIgnoreCase(dataPoint.getXValue()) || t.getCustomCategory().equalsIgnoreCase(dataPoint.getXValue())).toList());
                        listDialog.show();
                    });
                });
            });
        });

        transactionTypeSelector.setConverter(new StringConverter<TypeFilter>() {
            @Override
            public String toString(TypeFilter object) {
                return object.getDisplayName();
            }

            @Override
            public TypeFilter fromString(String string) {
                return null;
            }
        });

        transactionTypeSelector.setValue(TypeFilter.EXPENSES);
        transactionTypeSelector.setItems(FXCollections.observableList(List.of(TypeFilter.values())));
        transactionTypeSelector.valueProperty().addListener((obs, old, val) -> {
            if (val == null) return;
            currentTypeFilter = val;
            refreshUI();
        });

        refreshUI();
    }

    public void updateCache() {
        if (!shouldRecache) return;

        DataManager.log("Beginning to cache data...");
        long start = System.currentTimeMillis();

        cachedTransactions = null;
        cachedTransactions = fiscal.getTransactionsBetween(selectedAccount, periodStartPicker.getValue(), periodEndPicker.getValue());

        DataManager.log("Caching completed in " + (System.currentTimeMillis() - start) + "ms");
        shouldRecache = false;
    }

    private void refreshUI() {
        if (selectedAccount == null) return;
        updateCache();

        long refreshStart = System.currentTimeMillis();
        categoryBreakdownListView.setCellFactory(new CategoryExpenseTotalCellFactory(selectedAccount));
        transactionList.setCellFactory(new TransactionCellFactory(selectedAccount, (_) -> refreshUI(), true));

        List<Transaction> expenses = cachedTransactions.stream().filter(t -> fiscal.isExpense(selectedAccount, t)).toList();

        Map<String, List<Transaction>> categoriesToTransactions = expenses.stream().collect(Collectors.groupingBy(t -> t.getCategory() == TransactionCategory.CUSTOM ? t.getCustomCategory() : DataManager.Formatting.capitalize(t.getCategory().name())));

        totalSpendingLabel.setText(DataManager.Formatting.formatMoney(expenses.stream().mapToDouble(Transaction::getAbsoluteAmount).sum(), selectedAccount.getCurrencyConfig()));
        categoriesToTransactions.entrySet().stream().max(Comparator.comparingDouble(entry -> entry.getValue().stream().mapToDouble(Transaction::getAbsoluteAmount).sum())).ifPresentOrElse(max -> {
            mostSpentOnLabel.setText("%s (%s)".formatted(max.getKey(), DataManager.Formatting.formatMoney(max.getValue().stream().mapToDouble(Transaction::getAbsoluteAmount).sum(), selectedAccount.getCurrencyConfig())));
        }, () -> {
            mostSpentOnLabel.setText("N/A");
        });

        List<CategoryExpenseSummary> items = new ArrayList<>(categoriesToTransactions.entrySet().stream().map(entry -> new CategoryExpenseSummary(entry.getKey(), entry.getValue())).toList());
        items.sort(Comparator.comparingDouble(CategoryExpenseSummary::totalExpenses).reversed());
        categoryBreakdownListView.setItems(FXCollections.observableList(items));

        List<Transaction> sortedTransactionListItems = cachedTransactions.stream().filter(t -> switch (currentTypeFilter) {
            case EXPENSES -> fiscal.isExpense(selectedAccount, t);
            case INCOME -> fiscal.isIncome(selectedAccount, t);
            case TRANSFERS -> t.getType().equals(TransactionType.TRANSFER);
            case EXPENSES_NO_TRANSFERS ->
                    fiscal.isExpense(selectedAccount, t) && !t.getType().equals(TransactionType.TRANSFER);
            case INCOME_NO_TRANSFERS ->
                    fiscal.isIncome(selectedAccount, t) && !t.getType().equals(TransactionType.TRANSFER);
        }).toList();
        switch (sortSelector.getValue()) {
            case CHRONOLOGICAL_ASC -> {
                sortedTransactionListItems = sortedTransactionListItems.stream().sorted(Comparator.comparing(Transaction::getTimestamp)).toList();
            }
            case CHRONOLOGICAL_DESC -> {
                sortedTransactionListItems = sortedTransactionListItems.stream().sorted(Comparator.comparing(Transaction::getTimestamp).reversed()).toList();
            }
            case TRANSACTION_AMOUNT_ASC -> {
                sortedTransactionListItems = sortedTransactionListItems.stream().sorted(Comparator.comparing(Transaction::getAmount).reversed()).toList();
            }
            case TRANSACTION_AMOUNT_DESC -> {
                sortedTransactionListItems = sortedTransactionListItems.stream().sorted(Comparator.comparing(t -> {
                    if (t.getType().equals(TransactionType.TRANSFER)) {
                        return -t.getAbsoluteAmount();
                    } else {
                        return t.getAmount();
                    }
                })).toList();
            }
        }

        transactionList.setItems(FXCollections.observableList(sortedTransactionListItems));
        transactionCountLabel.setText("%s total transactions, %s different categories".formatted(sortedTransactionListItems.size(), items.size()));

        refreshCharts(cachedTransactions, items);
        refreshStats();
        DataManager.log("UI refresh took " + (System.currentTimeMillis() - refreshStart) + "ms");
    }

    private void refreshStats() {
        if (selectedAccount == null) return;
        startingBalanceLabel.setText(DataManager.Formatting.formatMoney(fiscal.getStartingBalance(selectedAccount, periodStartPicker.getValue()), selectedAccount.getCurrencyConfig()));
        currentBalanceLabel.setText(DataManager.Formatting.formatMoney(fiscal.getBalance(selectedAccount, periodEndPicker.getValue()), selectedAccount.getCurrencyConfig()));
        inflowLabel.setText(DataManager.Formatting.formatMoney(fiscal.getInflow(selectedAccount, cachedTransactions)));
        outflowLabel.setText(DataManager.Formatting.formatMoney(fiscal.getOutflow(selectedAccount, cachedTransactions)));
        changeLabel.setText(DataManager.Formatting.formatMoney(fiscal.getChange(selectedAccount, cachedTransactions), selectedAccount.getCurrencyConfig()));
    }

    private void refreshCharts(List<Transaction> transactions, List<CategoryExpenseSummary> categorisedTransactions) {
        breakdownPieChart.setData(FXCollections.observableList(categorisedTransactions.stream().map(entry -> new PieChart.Data(entry.categoryName(), entry.totalExpenses())).toList()));

        XYChart.Series<String, Double> series = new XYChart.Series<>();
        categorisedTransactions.forEach(summary -> {
            series.getData().add(new XYChart.Data<>(summary.categoryName(), summary.totalExpenses()));
        });
        categoriesBarChart.setData(FXCollections.observableList(List.of(series)));

        Map<LocalDate, List<Transaction>> daysToSpending = transactions.stream().collect(Collectors.groupingBy(t -> t.getTimestamp().toLocalDate()));
        XYChart.Series<String, Double> spendingSeries = new XYChart.Series<>();
        daysToSpending.forEach((date, list) -> {
            spendingSeries.getData().add(new XYChart.Data<>(Formats.DATE_FORMATTER.format(date), list.stream().mapToDouble(Transaction::getAbsoluteAmount).sum()));
        });

        spendingLineChart.setData(FXCollections.observableList(List.of(spendingSeries)));
    }
}
